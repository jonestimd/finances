#include "accountdao.h"
#include "mapping.h"

#include <QtSql>

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
    : EntityDao<Account>{getAccountsSql, updateAccountSql, insertAccountSql, deleteAccountSql, "AccountDao",
                         QObject::tr("Accounts have been modified.  Please reload and try again.")} {}

void AccountDao::bindUpdateValues(QSqlQuery &query, Account *account) {
    EntityDao::bindUpdateValues(query, account);
    query.bindValue(":companyId", account->companyId);
    query.bindValue(":description", account->description);
    query.bindValue(":type", account->type);
    query.bindValue(":accountNo", account->accountNumber);
    query.bindValue(":closed", mapping::toYesNo(account->closed));
}

void AccountDao::bindInsertValues(QSqlQuery &query, Account *account) {
    EntityDao::bindInsertValues(query, account);
    query.bindValue(":companyId", account->companyId);
    query.bindValue(":description", account->description);
    query.bindValue(":type", account->type);
    query.bindValue(":accountNo", account->accountNumber);
    query.bindValue(":closed", mapping::toYesNo(account->closed));
}
