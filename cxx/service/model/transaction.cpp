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

bool Transaction::isEmpty() const {
    return payeeId.isNull() && securityId.isNull() && referenceNumber.isNull() && memo.isNull();
}

Transaction *Transaction::newTransfer(const QVariant &accountId) const {
    auto relatedTransaction = new Transaction(*this);
    relatedTransaction->accountId = accountId;
    return relatedTransaction;
}

TransactionUpdate::TransactionUpdate(
    const QList<Transaction*> updates,
    const QList<Transaction*> adds,
    QList<const Transaction*> deletes,
    const QList<TransactionDetail*> detailUpdates,
    QHash<const Transaction*, QList<TransactionDetail*>>  detailAdds,
    QList<const TransactionDetail*> detailDeletes)
    : BulkUpdate<Transaction>{updates, adds, deletes}
    , detailUpdates{detailUpdates}
    , detailAdds{detailAdds}
    , detailDeletes{detailDeletes}
{}

void TransactionUpdate::onError() {
    BulkUpdate::onError();
    for (auto entity : std::as_const(detailUpdates)) delete entity;
    for (auto &entities : std::as_const(detailAdds)) {
        for (auto entity : entities) delete entity;
    }
}

TransactionsData::TransactionsData(QList<const Transaction*> transactions, QList<const TransactionDetail*> details)
    : transactions{transactions}
    , details{details}
{}
