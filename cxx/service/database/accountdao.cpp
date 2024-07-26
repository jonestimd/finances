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

namespace accountDao {
    QList<const Account*> getAll(QSqlDatabase &db) {
        QSqlQuery query(db);
        if (!query.exec(getAccountsSql)) {
            qCritical() << "accountDao:" << query.lastError().text();
            throw query.lastError().text();
        }
        QList<const Account*> accounts;
        if (query.size() > 0) accounts.reserve(query.size());
        while (query.next()) {
            accounts.append(new Account(query.record()));
        }
        return accounts;
    }

    QList<const Account *> update(QSqlDatabase &db, QList<Account *> accounts, const QString &user) {
        QSqlQuery query(db);
        QList<const Account*> result;
        result.reserve(accounts.length());
        query.prepare(updateAccountSql);
        query.bindValue(":user", user);
        for (auto account : accounts) {
            query.bindValue(":id", account->id);
            query.bindValue(":version", account->version);
            query.bindValue(":companyId", account->companyId);
            query.bindValue(":name", account->name);
            query.bindValue(":description", account->description);
            query.bindValue(":type", account->type);
            query.bindValue(":accountNo", account->accountNumber);
            query.bindValue(":closed", mapping::toYesNo(account->closed));
            if (!query.exec()) {
                qCritical() << "accountDao.update:" << query.lastError();
                throw query.lastError().text();
            }
            if (query.numRowsAffected() < 1) throw "stale data";
            account->version = account->version.toInt() + 1;
            account->changeUser = user;
            result.append(account);
        }
        return result;
    }

    QList<const Account *> add(QSqlDatabase &db, QList<Account *> accounts, const QString &user) {
        QSqlQuery query(db);
        QList<const Account*> result;
        result.reserve(accounts.length());
        query.prepare(insertAccountSql);
        query.bindValue(":user", user);
        for (auto account: accounts) {
            query.bindValue(":companyId", account->companyId);
            query.bindValue(":name", account->name);
            query.bindValue(":description", account->description);
            query.bindValue(":type", account->type);
            query.bindValue(":accountNo", account->accountNumber);
            query.bindValue(":closed", mapping::toYesNo(account->closed));
            if (!query.exec()) {
                qCritical() << "accountDao.insert:" << query.lastError();
                throw query.lastError().text();
            }
            account->id = query.lastInsertId();
            account->changeUser = user;
            result.append(account);
        }
        return result;
    }

    void remove(QSqlDatabase &db, QList<const Account *> companies) {
        QSqlQuery query(db);
        query.prepare(deleteAccountSql);
        for (auto company : companies) {
            query.bindValue(":id", company->id);
            if (!query.exec()) {
                qCritical() << "accountDao.remove:" << query.lastError();
                throw query.lastError().text();
            }
        }
    }
}
