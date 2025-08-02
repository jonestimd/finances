#include "accountdao.h"
#include "mapping.h"
#include "dbdialect.h"

static const auto createTableQuery = R"(
create table account (
    id %1,
    company_id bigint,
    currency_id bigint not null,
    type varchar(25) not null,
    name varchar(100) not null,
    description text,
    account_no varchar(25),
    closed character(1) not null,
    change_date timestamp not null default current_timestamp,
    change_user varchar(50) not null,
    version bigint not null,
    constraint account_ak unique (name, company_id),
    constraint account_company_fk foreign key (company_id) references company (id),
    constraint account_currency_fk foreign key (currency_id) references asset (id)
))";

static const auto getAccountsSql = R"(
with balance as (
  select tx.account_id, sum(case when tc.amount_type = 'ASSET_VALUE' then 0 else td.amount end) balance, count(distinct tx.id) transactions
  from tx
  join tx_detail td on tx.id = td.tx_id
  left join tx_category tc on td.tx_category_id = tc.id
  group by tx.account_id
)
select a.*, coalesce(b.transactions, 0) transactions, b.*, cur.symbol currency
from account a
join asset cur on a.currency_id = cur.id
left join balance b on a.id = b.account_id
order by a.name)";

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

AccountDao::AccountDao()
    : NamedEntityDao<Account>{getAccountsSql, updateAccountSql, insertAccountSql, deleteAccountSql, "AccountDao",
                              QObject::tr("Accounts have been modified.  Please reload and try again.")} {}

void AccountDao::createTable(const QSqlDatabase &db) {
    sql::exec(db, dbDialect::createTableSql(db, createTableQuery), className, "createTable");
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
