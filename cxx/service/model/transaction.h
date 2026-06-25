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
    qlonglong accountId;
    QVariant date;
    QVariant payeeId;
    QVariant securityId;
    QVariant referenceNumber;
    QVariant memo;
    QVariant cleared{false};
    QList<qlonglong> detailIds{};

    Transaction();
    Transaction(qlonglong accountId);
    Transaction(const QSqlRecord &record);

    bool deletable() const;

    Transaction *newTransfer(qlonglong accountId) const;

    QString toString() const;
};

class PendingTransaction : public Transaction {
public:
    QList<TransactionDetail*> details{};

    PendingTransaction();
    PendingTransaction(qlonglong accountId);
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
    QList<qlonglong> deletedIds{};
    /** @brief deletedDetailIds IDs of deleted related details. */
    QList<qlonglong> deletedDetailIds{};

    TransactionsData() = default;
    TransactionsData(
        QList<const Transaction*> transactions,
        QList<const TransactionDetail*> details,
        const QList<qlonglong> deletedIds,
        const QList<qlonglong> deletedDetailIds
    );
};

struct TransactionChange {
    const Transaction* const oldTransaction;
    const Transaction* const newTransaction;

    TransactionChange(const Transaction* oldTx, const Transaction* newTx);
};

struct DetailChange {
    const TransactionDetail* const oldDetail;
    const TransactionDetail* const newDetail;

    DetailChange(const TransactionDetail* oldTx, const TransactionDetail* newTx);
};

#endif // TRANSACTION_H
