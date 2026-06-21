#include "transaction.h"
#include "service/database/mapping.h"
#include "service/database/sql.h"
#include <QSqlField>

Transaction::Transaction() {}

Transaction::Transaction(const QVariant &accountId) : accountId{accountId} {}

Transaction::Transaction(const QSqlRecord &record)
    : BaseDomain{record}
    , accountId{record.field("account_id").value()}
    , date{record.field("date").value()}
    , payeeId{sql::getValue(record, "payee_id")}
    , securityId{sql::getValue(record, "security_id")}
    , referenceNumber{sql::getValue(record, "reference_number")}
    , memo{sql::getValue(record, "memo")}
    , cleared{sql::yesNoValue(record, "cleared")}
    , detailIds(mapping::jsonToList(record.field("detail_ids").value()))
{}

bool Transaction::deletable() const {
    return true;
}

Transaction *Transaction::newTransfer(const QVariant &accountId) const {
    auto relatedTransaction = new Transaction(*this);
    relatedTransaction->accountId = accountId;
    return relatedTransaction;
}

QString Transaction::toString() const {
    return QString("accountId{") % accountId.toString()
           % "},date{" % date.toString()
           % "},referenceNumber{" % referenceNumber.toString()
           % "},payeeId{" % payeeId.toString()
           % "},securityId{" % securityId.toString()
           % "},memo{" % memo.toString()
           % "},cleared{" % cleared.toString() % "}";
}

PendingTransaction::PendingTransaction() {}

PendingTransaction::PendingTransaction(const QVariant &accountId) : Transaction{accountId} {
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
    return payeeId.isNull() && securityId.isNull() && referenceNumber.isNull() && memo.isNull();
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
    const QList<QVariant> deletedIds,
    const QList<QVariant> deletedDetailIds
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
