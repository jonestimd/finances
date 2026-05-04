#include "accountdao.h"
#include "mapping.h"
#include "dbdialect.h"

#define CREATE_TABLE_QUERY(idtype) \
    "create table account (\n" \
    "    id " idtype ",\n" \
    "    company_id bigint,\n" \
    "    currency_id bigint not null,\n" \
    "    type varchar(25) not null,\n" \
    "    name varchar(100) not null,\n" \
    "    description text,\n" \
    "    account_no varchar(25),\n" \
    "    closed character(1) not null,\n" \
    "    change_date timestamp not null default current_timestamp,\n" \
    "    change_user varchar(50) not null,\n" \
    "    version bigint not null,\n" \
    "    constraint account_ak unique (name, company_id),\n" \
    "    constraint account_company_fk foreign key (company_id) references company (id),\n" \
    "    constraint account_currency_fk foreign key (currency_id) references asset (id)\n" \
    ")"

static const auto pgCreateTableSql = CREATE_TABLE_QUERY(PG_ID_TYPE);
static const auto mysqlCreateTableSql = CREATE_TABLE_QUERY(MYSQL_ID_TYPE);
static const auto sqliteCreateTableSql = CREATE_TABLE_QUERY(SQLITE_ID_TYPE);

#define GET_ALL_QUERY(sum) \
    "with balance as (\n" \
    "  select tx.account_id, " sum "(case when tc.amount_type = 'ASSET_VALUE' then 0 else td.amount end) balance, count(distinct tx.id) transactions\n" \
    "  from tx\n" \
    "  join tx_detail td on tx.id = td.tx_id\n" \
    "  left join tx_category tc on td.tx_category_id = tc.id\n" \
    "  group by tx.account_id\n" \
    ")\n" \
    "select a.*, coalesce(b.transactions, 0) transactions, b.*, cur.symbol currency\n" \
    "from account a\n" \
    "join asset cur on a.currency_id = cur.id\n" \
    "left join balance b on a.id = b.account_id"

static const auto updateAccountSql = R"(
update account
set company_id = :companyId, name = :name, description = :description,
    type = :type, account_no = :accountNo, closed = :closed,
    change_user = :user, change_date = current_timestamp, version = version + 1
where id = :id and version = :version)";

static const auto insertAccountSql = R"(
insert into account (name, company_id, description, type, account_no, closed, currency_id, version, change_user, change_date)
select :name, :companyId, :description, :type, :accountNo, :closed, c.id, 0, :user, current_timestamp
from asset c
where c.type = 'Currency' and c.symbol = '$')";

static const auto deleteAccountSql = "delete from account where id = :id";

#define DAO_QUERIES(selectAll) \
    .getAllSql = selectAll,\
    .updateSql = updateAccountSql,\
    .insertSql = insertAccountSql,\
    .deleteSql = deleteAccountSql,

static const DaoQueries pgMysqlQueries{
    DAO_QUERIES(GET_ALL_QUERY("sum"))
};
static const DaoQueries sqliteQueries{
    DAO_QUERIES(GET_ALL_QUERY(SQLITE_SUM))
};

AccountDao::AccountDao(const QString &dbType)
    : NamedEntityDao<Account>{dbType == SQLITE_DRIVER ? sqliteQueries : pgMysqlQueries, "AccountDao",
                              QObject::tr("Accounts have been modified.  Please reload and try again."), "a.id"}
    , createTableSql{DB_TYPE_QUERY(dbType, CreateTableSql)}
{}

void AccountDao::createTable(const QSqlDatabase &db) const {
    sql::exec(db, createTableSql, className, "createTable");
}

void AccountDao::bindUpdateValues(QSqlQuery &query, Account *account) {
    NamedEntityDao::bindUpdateValues(query, account);
    SQL_BIND_VALUE(query, ":companyId", account->companyId);
    SQL_BIND_VALUE(query, ":description", account->description);
    SQL_BIND_VALUE(query, ":type", account->type);
    SQL_BIND_VALUE(query, ":accountNo", account->accountNumber);
    SQL_BIND_VALUE(query, ":closed", mapping::toYesNo(account->closed));
}

void AccountDao::bindInsertValues(QSqlQuery &query, Account *account) {
    NamedEntityDao::bindInsertValues(query, account);
    SQL_BIND_VALUE(query, ":companyId", account->companyId);
    SQL_BIND_VALUE(query, ":description", account->description);
    SQL_BIND_VALUE(query, ":type", account->type);
    SQL_BIND_VALUE(query, ":accountNo", account->accountNumber);
    SQL_BIND_VALUE(query, ":closed", mapping::toYesNo(account->closed));
}
