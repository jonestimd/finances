#ifndef TRANSACTION_H
#define TRANSACTION_H

#include "basedomain.h"
#include "bulkupdate.h"
#include "transactiondetail.h"

class Transaction;
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
    QVariant cleared{false};
    QList<QVariant> detailIds{};

    Transaction();
    Transaction(const QVariant &accountId);
    Transaction(const QSqlRecord &record);

    bool deletable() const;

    Transaction *newTransfer(const QVariant &accountId) const;

    QString toString() const;
};

class PendingTransaction : public Transaction {
public:
    QList<TransactionDetail*> details{};

    PendingTransaction();
    PendingTransaction(const QVariant &accountId);
    PendingTransaction(const PendingTransaction &that);
    ~PendingTransaction();

    bool isEmpty() const;
};

struct TransactionUpdate : public BulkUpdate<Transaction, PendingTransaction> {
    const QList<TransactionDetail*> detailUpdates;
    QList<TransactionDetail*> detailAdds;
    const QList<const TransactionDetail*> detailDeletes;

    TransactionUpdate(
        const QList<Transaction*> updates,
        const QList<const PendingTransaction*> adds,
        QList<const Transaction*> deletes,
        const QList<TransactionDetail*> detailUpdates,
        QList<const TransactionDetail*> detailAdds,
        QList<const TransactionDetail*> detailDeletes);

    /** @brief Deteles `adds` because the service converted them to `Transaction`s. */
    ~TransactionUpdate();

    virtual void onError() override;
};

struct TransactionsData {
    QList<const Transaction*> transactions{};
    QList<const TransactionDetail*> details{};
    /** @brief deletedIds IDs of transactions deleted due to changes to transfer details. */
    QList<QVariant> deletedIds{};
    /** @brief deletedDetailIds IDs of deleted related details. */
    QList<QVariant> deletedDetailIds{};

    TransactionsData() = default;
    TransactionsData(
        QList<const Transaction*> transactions,
        QList<const TransactionDetail*> details,
        const QList<QVariant> deletedIds,
        const QList<QVariant> deletedDetailIds
    );
};

struct TransactionChange {
    const Transaction* const oldTransaction;
    const Transaction* const newTransaction;

    TransactionChange(const Transaction* oldTx, const Transaction* newTx);
};

#endif // TRANSACTION_H
