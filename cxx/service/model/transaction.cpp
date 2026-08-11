#include "transaction.h"
#include "service/database/mapping.h"
#include "service/database/sql.h"
#include <QSqlField>

Transaction::Transaction() {}

Transaction::Transaction(domain_id accountId) : accountId{accountId} {}

Transaction::Transaction(const QSqlRecord &record)
    : BaseDomain{record}
    , accountId{record.value("account_id").toLongLong()}
    , date{sql::getDate(record, "date").value()}
    , payeeId{sql::getInt(record, "payee_id")}
    , securityId{sql::getInt(record, "security_id")}
    , referenceNumber{sql::getString(record, "reference_number")}
    , memo{sql::getString(record, "memo")}
    , cleared{sql::yesNoValue(record, "cleared")}
    , detailIds(mapping::jsonToSortedIntList(record.value("detail_ids")))
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

void Transaction::appendIds(QSet<domain_id> &accountIds, QSet<domain_id> &securityIds) const {
    if (securityId.has_value()) {
        accountIds.insert(accountId);
        securityIds.insert(securityId.value());
    }
}

bool operator<(const Transaction& left, const Transaction &right) {
    if (left.date.isValid() && left.date == right.date) {
        return left.id.has_value() && (!right.id.has_value() || left.id.value() < right.id.value());
    }
    return left.date.isValid() && (!right.date.isValid() || left.date < right.date);
}

PendingTransaction::PendingTransaction() {}

PendingTransaction::PendingTransaction(domain_id accountId)
    : Transaction{accountId}
    , details{new TransactionDetail}
{}

PendingTransaction::PendingTransaction(const PendingTransaction &that) : Transaction(that) {
    for (auto detail : std::as_const(that.details)) details.append(new TransactionDetail(*detail));
}

PendingTransaction* PendingTransaction::copyRecent(const Transaction* tx, const QHash<domain_id, const TransactionDetail*> details) {
    auto copy = new PendingTransaction;
    copy->payeeId = tx->payeeId;
    copy->securityId = tx->securityId;
    copy->memo = tx->memo;
    for (auto id : std::as_const(tx->detailIds)) {
        copy->details.append(TransactionDetail::copyRecent(details.value(id)));
    }
    return copy;
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

domain_id TransactionChange::txId() const {
    return oldTransaction ? oldTransaction->id.value() : newTransaction->id.value();
}

void TransactionChange::appendIds(QList<domain_id>& txIds, QSet<domain_id>& accountIds, QSet<domain_id>& securityIds) const {
    txIds.append(txId());
    if (oldTransaction) oldTransaction->appendIds(accountIds, securityIds);
    if (newTransaction) newTransaction->appendIds(accountIds, securityIds);
}

DetailChange::DetailChange(const TransactionDetail *oldDetail, const TransactionDetail *newDetail)
    : oldDetail{oldDetail}, newDetail{newDetail} {}

bool DetailChange::isSecurityChange() const {
    if (oldDetail && oldDetail->assetQuantity.has_value()) {
        return !newDetail || oldDetail->assetQuantity != newDetail->assetQuantity || oldDetail->amount != newDetail->amount;
    }
    return newDetail && newDetail->assetQuantity.has_value() && !oldDetail;
}


