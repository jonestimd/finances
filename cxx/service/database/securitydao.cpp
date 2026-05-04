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

static const auto pgCreateAssetTableSql = CREATE_ASSET_TABLE_QUERY(PG_ID_TYPE);
static const auto mysqlCreateAssetTableSql = CREATE_ASSET_TABLE_QUERY(MYSQL_ID_TYPE);
static const auto sqliteCreateAssetTableSql = CREATE_ASSET_TABLE_QUERY(SQLITE_ID_TYPE);

static const auto createTableSql = R"(
create table security (
    type varchar(25) not null,
    asset_id bigint not null,
    constraint security_pkey primary key (asset_id),
    constraint security_asset_fk foreign key (asset_id) references asset (id)
))";

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
    "     , coalesce(sum.cost_basis) cost_basis\n" \
    "     , coalesce(sum.dividends) dividends\n" \
    "from asset a\n" \
    "join security s on a.id = s.asset_id\n" \
    "left join summary sum on a.id = sum.security_id"

static const auto updateSecuritySql = R"(
update asset a
join security s on a.id = s.asset_id
set a.name = :name, a.symbol = :symbol, s.type = :securityType,
    a.change_user = :user, a.change_date = current_timestamp, a.version = version + 1
where a.id = :id and a.version = :version)";

static const auto insertSecuritySql = R"(
insert into asset (type, name, symbol, scale, version, change_user, change_date)
values (:type, :name, :symbol, :scale, 0, :user, current_timestamp))";

static const auto insertSecurityQuery = "insert into security (asset_id, type) values (:id, :type)";

static const auto deleteSecuritySql = "delete from security where asset_id = :id";

#define DAO_QUERIES(selectAll) \
    .getAllSql = selectAll,\
    .updateSql = updateSecuritySql,\
    .insertSql = insertSecuritySql,\
    .deleteSql = deleteSecuritySql,

static const DaoQueries pgMysqlQueries{
    DAO_QUERIES(GET_ALL_QUERY("sum"))
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(GET_ALL_QUERY(SQLITE_SUM))
};

SecurityDao::SecurityDao(const QString &dbType)
    : NamedEntityDao<Security>{dbType == SQLITE_DRIVER ? sqliteQueries : pgMysqlQueries, "SecurityDao",
                               QObject::tr("Securities have been modified.  Please reload and try again")}
    , createAssetTableSql{DB_TYPE_QUERY(dbType, CreateAssetTableSql)}
{}

void SecurityDao::createTable(const QSqlDatabase &db) const {
    sql::exec(db, createAssetTableSql, className, "createTable.asset");
    sql::exec(db, createTableSql, className, "createTable.security");
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

void SecurityDao::bindUpdateValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindUpdateValues(query, security);
    query.bindValue(":symbol", security->symbol);
    query.bindValue(":securityType", security->securityType);
}

 void SecurityDao::bindInsertValues(QSqlQuery &query, Security *security) {
    NamedEntityDao::bindInsertValues(query, security);
    query.bindValue(":type", security->type);
    query.bindValue(":symbol", security->symbol);
    query.bindValue(":scale", security->scale);
}
