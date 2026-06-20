#include "securitydao.h"
#include "dbdialect.h"

#define CREATE_ASSET_TABLE_QUERY(idtype) \
    "create table asset (\n" \
    "    id " idtype ",\n" \
    "    name varchar(100) not null,\n" \
    "    type varchar(31) not null,\n" \
    "    scale integer not null,\n" \
    "    symbol varchar(10) default null,\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    constraint asset_ak unique (name, type)\n" \
    ")"

static const auto createSecurityTableSql = R"(
create table security (
    type varchar(25) not null,
    asset_id bigint not null,
    constraint security_pkey primary key (asset_id),
    constraint security_asset_fk foreign key (asset_id) references asset (id)
))";

static const auto pgCreateAdjustShares = R"(
create function adjust_shares(_security_id bigint, _from_date timestamp, _shares numeric)
  returns decimal(19,6) as $$
declare
  curs cursor for
    select ss.shares_out, ss.shares_in
    from stock_split ss
    where ss.security_id = _security_id and ss.date >= _from_date;
begin
  if _shares is not null then
    for split in curs loop
      _shares := _shares * split.shares_out / split.shares_in;
    end loop;
  end if;
  return _shares;
end;
$$ language plpgsql stable)";

static const char *const sqliteCreateAdjustShares = nullptr;

static const auto mysqlCreateAdjustShares = R"(
create function adjust_shares(security_id bigint, from_date datetime, shares decimal(19,6))
  returns decimal(19,6)
  deterministic reads sql data
begin
  declare done boolean default false;
  declare shares_in  decimal(19,6);
  declare shares_out decimal(19,6);
  declare cur cursor for
    select ss.shares_out, ss.shares_in
    from stock_split ss
    where ss.security_id = security_id and ss.date >= from_date;
  declare continue handler for NOT FOUND set done = true;
  if shares is not null then
    open cur;
    read_loop: loop
      fetch cur into shares_out, shares_in;
      if done then
        leave read_loop;
      end if;
      set shares = shares * shares_out / shares_in;
    end loop read_loop;
    close cur;
  end if;
  return shares;
end)";

#define CREATE_ACCOUNT_SECURITY_QUERY(sum, greatest) \
    "create view account_security as\n" \
    "with shares_out as (\n" \
    "    select sx.account_id\n" \
    "       , sx.security_id\n" \
    "       , rx.account_id xfer_account_id\n" \
    "       , " sum "(abs(round(pd.amount * sl.purchase_shares / pd.asset_quantity, 2))) cost_basis\n" \
    "    from security_lot sl\n" \
    "    join tx_detail pd on sl.purchase_tx_detail_id = pd.id\n" \
    "    join tx_detail sd on sl.related_tx_detail_id = sd.id\n" \
    "    join tx sx on sd.tx_id = sx.id\n" \
    "    left join tx_detail rd on sd.related_detail_id = rd.id\n" \
    "    left join tx rx on rd.tx_id = rx.id\n" \
    "    group by sx.account_id, sx.security_id, rx.account_id\n" \
    ")\n" \
    "select tx.account_id, tx.security_id\n" \
    ", " sum "(adjust_shares(tx.security_id, tx.date, td.asset_quantity)) shares\n" \
    ", " sum "(case when td.asset_quantity > 0 and td.related_detail_id is null then abs(td.amount) else 0 end)\n" \
    "    - (select coalesce(" sum "(cost_basis), 0) from shares_out where account_id = tx.account_id and security_id = tx.security_id)\n" \
    "    + (select coalesce(" sum "(cost_basis), 0) from shares_out where xfer_account_id = tx.account_id and security_id = tx.security_id) cost_basis\n" \
    ", " sum "(case when td.asset_quantity is null and td.related_detail_id is null then " greatest "(td.amount, 0) else 0 end) dividends\n" \
    ", min(tx.date) first_acquired\n" \
    ", count(distinct tx.id) use_count\n" \
    "from tx\n" \
    "join tx_detail td on tx.id = td.tx_id\n" \
    "where tx.security_id is not null\n" \
    "group by tx.account_id, tx.security_id\n"

static const auto pgMysqlCreateAccountSecuritySql = CREATE_ACCOUNT_SECURITY_QUERY(DEFAULT_SUM, DEFAULT_GREATEST);
static const auto sqliteCreateAccountSecuritySql = CREATE_ACCOUNT_SECURITY_QUERY(SQLITE_SUM, SQLITE_GREATEST);

#define GET_ALL_QUERY(sum) \
    "with summary as (\n" \
    "    select security_id\n" \
    "         , " sum "(use_count) transactions\n" \
    "         , " sum "(shares) shares\n" \
    "         , min(first_acquired) first_acquired\n" \
    "         , " sum "(cost_basis) cost_basis\n" \
    "         , " sum "(dividends) dividends\n" \
    "    from account_security\n" \
    "    group by security_id\n" \
    ")\n" \
    "select a.*, s.type security_type, coalesce(sum.transactions, 0) transactions\n" \
    "     , coalesce(sum.shares, 0) shares, sum.first_acquired\n" \
    "     , coalesce(sum.cost_basis, 0) cost_basis\n" \
    "     , coalesce(sum.dividends, 0) dividends\n" \
    "from asset a\n" \
    "join security s on a.id = s.asset_id\n" \
    "left join summary sum on a.id = sum.security_id"

static const auto updateAssetSql = R"(
update asset
set name = :name, symbol = :symbol,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto updateSecuritySql = "update security set type = :securityType where asset_id = :id";

static const auto insertAssetSql = R"(
insert into asset (type, name, symbol, scale, version, change_user, change_date)
values (:type, :name, :symbol, :scale, 0, :user, current_timestamp))";

static const auto insertSecurityQuery = "insert into security (asset_id, type) values (:id, :type)";

static const auto deleteSecuritySql = "delete from security where asset_id = :id";

#define DAO_QUERIES(idtype, sum) \
    .createTableSql = CREATE_ASSET_TABLE_QUERY(idtype),\
    .getAllSql = GET_ALL_QUERY(sum),\
    .updateSql = updateAssetSql,\
    .insertSql = insertAssetSql,\
    .deleteSql = deleteSecuritySql,

static const DaoQueries pgQueries{
    DAO_QUERIES(PG_ID_TYPE, DEFAULT_SUM)
};
static const DaoQueries mysqlQueries{
    DAO_QUERIES(MYSQL_ID_TYPE, DEFAULT_SUM)
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(SQLITE_ID_TYPE, SQLITE_SUM)
};

SecurityDao::SecurityDao(const QString &dbType)
    : NamedEntityDao<Security>{DB_TYPE_QUERY(dbType, Queries), "SecurityDao",
                               QObject::tr("Securities have been modified.  Please reload and try again")}
    , createAdjustSharesSql{DB_TYPE_QUERY(dbType, CreateAdjustShares)}
    , createAccountSecuritySql{dbType == SQLITE_DRIVER ? sqliteCreateAccountSecuritySql : pgMysqlCreateAccountSecuritySql}
{}

void SecurityDao::createTable(const QSqlDatabase &db) const {
    NamedEntityDao::createTable(db);
    sql::exec(db, createSecurityTableSql, className, "createTable.security");
}

void SecurityDao::createViews(const QSqlDatabase &db) const {
    if (createAdjustSharesSql != nullptr) sql::exec(db, createAdjustSharesSql, className, "createAdjustShares");
    sql::exec(db, createAccountSecuritySql, className, "createAccountSecurity");
}

QList<const Security *> SecurityDao::add(QSqlDatabase &db, QList<Security*> securities, const QString &user) {
    auto result = EntityDao::add(db, securities, user);
    QSqlQuery query(db);
    query.prepare(insertSecurityQuery);
    for (auto security : std::as_const(result)) {
        query.bindValue(":id", security->id);
        query.bindValue(":type", security->securityType);
        sql::exec(query, className, "insert security");
    }
    return result;
}

void SecurityDao::remove(QSqlDatabase &db, const QList<const Security*> securities) {
    EntityDao::remove(db, securities);
    QSqlQuery query(db);
    query.prepare("delete from asset where id = :id");
    for (auto security : securities) {
        query.bindValue(":id", security->id);
        sql::exec(query, className, "remove asset");
    }
}

QList<const Security*> SecurityDao::update(QSqlDatabase &db, const QList<Security*> securities, const QString &user) {
    auto updates = NamedEntityDao::update(db, securities, user);
    QSqlQuery query(db);
    query.prepare(updateSecuritySql);
    for (auto security : securities) {
        query.bindValue(":id", security->id);
        query.bindValue(":securityType", security->securityType);
    }
    return updates;
}

void SecurityDao::bindUpdateValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindUpdateValues(query, security);
    query.bindValue(":symbol", security->symbol);
}

 void SecurityDao::bindInsertValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindInsertValues(query, security);
    query.bindValue(":type", security->type);
    query.bindValue(":symbol", security->symbol);
    query.bindValue(":scale", security->scale);
}
