#ifndef TRANSACTION_H
#define TRANSACTION_H

#include "basedomain.h"
#include "bulkupdate.h"
#include "transactiondetail.h"

class TransactionDetail;
class TransactionDetailUpdate;

class Transaction : public BaseDomain {
public:
    QVariant accountId;
    QVariant date;
    QVariant payeeId;
    QVariant securityId;
    QVariant referenceNumber;
    QVariant memo;
    QVariant cleared;
    QList<QVariant> detailIds{};

    Transaction();
    Transaction(const QVariant &accountId);
    Transaction(const QSqlRecord &record);

    bool deletable() const;

    Transaction *newTransfer(const QVariant &accountId) const;
};

struct TransactionUpdate : public BulkUpdate<Transaction> {
    const QList<TransactionDetail*> detailUpdates;
    QMultiHash<const Transaction*, TransactionDetail*> detailAdds;
    const QList<const TransactionDetail*> detailDeletes;

    TransactionUpdate(
        const QList<Transaction*> updates,
        const QList<Transaction*> adds,
        QList<const Transaction*> deletes,
        const QList<TransactionDetail*> detailUpdates,
        QMultiHash<const Transaction*, TransactionDetail*> detailAdds,
        QList<const TransactionDetail*> detailDeletes);

    virtual void onError() override;
};

struct TransactionsData {
    QList<const Transaction*> transactions{};
    QList<const TransactionDetail*> details{};

    TransactionsData() = default;
    TransactionsData(QList<const Transaction*> transactions, QList<const TransactionDetail*> details);
};

#endif // TRANSACTION_H
