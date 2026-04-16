#include "transactiondao.h"
#include "sql.h"
#include <QSqlQuery>

#define DAO_NAME "TransactionDao"

static const auto getByAccountQuery = R"(
with detail_summary as (
  select tx_id, json_arrayagg(id) detail_ids
  from tx_detail
  group by tx_id
), tx_data as (
  select distinct tx.*
  from tx
  join tx_detail td on td.tx_id = tx.id
  left join tx_detail rd on rd.id = td.related_detail_id
  left join tx rx on rx.id = rd.tx_id
  where :accountId in (tx.account_id, rx.account_id)
)
select tx.*, ds.detail_ids
from tx_data tx
join detail_summary ds on ds.tx_id = tx.id)";

static const auto setPayeeQuery = R"(update tx
set payee_id = :payeeId, change_user = :user, change_date = current_timestamp, version = version + 1
where payee_id = :oldPayeeId)";

TransactionDao::TransactionDao()
    : EntityDao<Transaction>{"", "", "", "", "TransactionDao", QObject::tr("Transactions have been modified.  Please reload and try again.")}
{}

QHash<qlonglong, const Transaction*> TransactionDao::getAll(const QSqlDatabase &db, qlonglong accountId) {
    QSqlQuery query(db);
    query.prepare(getByAccountQuery);
    query.bindValue(":accountId", accountId);
    sql::exec(query, DAO_NAME, "getByAccount");
    return load(query);
}

void TransactionDao::replacePayee(const QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setPayeeQuery);
    query.bindValue(":user", user);
    query.bindValue(":payeeId", newPayeeId);
    query.bindValue(":oldPayeeId", payee->id);
    sql::exec(query, DAO_NAME, "setCategory");
    if (query.numRowsAffected() != payee->transactions.toInt()) throw staleDataMessage;
}

void TransactionDao::bindInsertValues(QSqlQuery &query, Transaction *entity) {
}
