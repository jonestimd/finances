#include "accountdao.h"

#include <QtSql>

static const auto getAccountsSql = R"(
with balance as (
  select tx.account_id, sum(case when tc.amount_type = 'ASSET_VALUE' then 0 else td.amount end) balance, count(distinct tx.id) transactions
  from tx
  join tx_detail td on tx.id = td.tx_id
  left join tx_category tc on td.tx_category_id = tc.id
  group by tx.account_id
)
select a.*, b.*, cur.symbol currency
from account a
join asset cur on a.currency_id = cur.id
left join balance b on a.id = b.account_id
order by a.name)";

namespace accountDao {
    QList<const Account*> getAll(QSqlDatabase db) {
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
}
