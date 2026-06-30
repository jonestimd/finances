#include "transaction.h"
#include "service/database/mapping.h"
#include "service/database/sql.h"
#include <QSqlField>

Transaction::Transaction() {}

Transaction::Transaction(domain_id accountId) : accountId{accountId} {}

Transaction::Transaction(const QSqlRecord &record)
    : BaseDomain{record}
    , accountId{record.field("account_id").value().toLongLong()}
    , date{sql::getDate(record, "date").value()}
    , payeeId{sql::getInt(record, "payee_id")}
    , securityId{sql::getInt(record, "security_id")}
    , referenceNumber{sql::getString(record, "reference_number")}
    , memo{sql::getString(record, "memo")}
    , cleared{sql::yesNoValue(record, "cleared")}
    , detailIds(mapping::jsonToIntList(record.field("detail_ids").value()))
{}

bool Transaction::deletable() const {
    return true;
}

Transaction *Transaction::newTransfer(domain_id accountId) const {
    auto relatedTransaction = new Transaction(*this);
    relatedTransaction->accountId = accountId;
    return relatedTransaction;
}

QString Transaction::toString() const {
    return QString("accountId{") % QString::number(accountId)
           % "},date{" % date.toString()
           % "},referenceNumber{" % referenceNumber
           % "},payeeId{" % domain::toString(payeeId)
           % "},securityId{" % domain::toString(securityId)
           % "},memo{" % memo
           % "},cleared{" % (cleared ? "Y" : "N") % "}";
}

PendingTransaction::PendingTransaction() {}

PendingTransaction::PendingTransaction(domain_id accountId) : Transaction{accountId} {
    details.append(new TransactionDetail);
}

PendingTransaction::PendingTransaction(const PendingTransaction &that) : Transaction(that) {
    for (auto detail : std::as_const(that.details)) details.append(new TransactionDetail(*detail));
}

PendingTransaction::~PendingTransaction() {
    qDeleteAll(details);
}

bool PendingTransaction::isEmpty() const {
    for (auto detail : std::as_const(details)) if (!detail->isEmpty()) return false;
    return !payeeId.has_value() && !securityId.has_value() && referenceNumber.isNull() && memo.isNull();
}

TransactionUpdate::TransactionUpdate(
    const QList<Transaction*> updates,
    const QList<const PendingTransaction*> adds,
    QList<const Transaction*> deletes,
    const QList<TransactionDetail*> detailUpdates,
    QList<const TransactionDetail*>  detailAdds,
    QList<const TransactionDetail*> detailDeletes)
    : BulkUpdate{updates, adds, deletes}
    , detailUpdates{detailUpdates}
    , detailAdds{domain::copy(detailAdds)}
    , detailDeletes{detailDeletes}
{}

TransactionUpdate::~TransactionUpdate() {
    qDeleteAll(adds);
}

void TransactionUpdate::onError() {
    BulkUpdate::onError();
    qDeleteAll(detailUpdates);
    qDeleteAll(detailAdds);
}

TransactionsData::TransactionsData(
    QList<const Transaction*> transactions,
    QList<const TransactionDetail*> details,
    const QList<domain_id> deletedIds,
    const QList<domain_id> deletedDetailIds
)
    : transactions{transactions}
    , details{details}
    , deletedIds(deletedIds)
    , deletedDetailIds(deletedDetailIds)
{}

TransactionChange::TransactionChange(const Transaction *oldTx, const Transaction *newTx)
    : oldTransaction{oldTx}, newTransaction{newTx} {}

DetailChange::DetailChange(const TransactionDetail *oldDetail, const TransactionDetail *newDetail)
    : oldDetail{oldDetail}, newDetail{newDetail} {}
