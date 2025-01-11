#include "transactiondao.h"
#include "service/model/sql.h"
#include <QSqlQuery>

static const auto setPayeeSql = R"(update tx
set payee_id = :payeeId, change_user = :user, change_date = current_timestamp, version = version + 1
where payee_id = :oldPayeeId)";

static QString staleDataMessage(QObject::tr("Transactions have been modified.  Please reload and try again."));

TransactionDao::TransactionDao() {}

void TransactionDao::replacePayee(QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user) {
    QSqlQuery query(db);
    query.prepare(setPayeeSql);
    query.bindValue(":user", user);
    query.bindValue(":payeeId", newPayeeId);
    query.bindValue(":oldPayeeId", payee->id);
    sql::exec(query, "TransactionDao", "setCategory");
    if (query.numRowsAffected() != payee->transactions.toInt()) throw staleDataMessage;
}
