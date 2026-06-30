#ifndef TRANSACTION_H
#define TRANSACTION_H

#include "basedomain.h"
#include "bulkupdate.h"
#include "transactiondetail.h"

#include <QDate>

class Transaction;
class TransactionDetail;
class TransactionDetailUpdate;

class Transaction : public BaseDomain {
public:
    domain_id accountId;
    QDate date;
    optional_id payeeId;
    optional_id securityId;
    QString referenceNumber;
    QString memo;
    bool cleared{false};
    QList<domain_id> detailIds{};

    Transaction();
    Transaction(domain_id accountId);
    Transaction(const QSqlRecord &record);

    bool deletable() const;

    Transaction *newTransfer(domain_id accountId) const;

    QString toString() const;
};

class PendingTransaction : public Transaction {
public:
    QList<TransactionDetail*> details{};

    PendingTransaction();
    PendingTransaction(domain_id accountId);
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
    QList<domain_id> deletedIds{};
    /** @brief deletedDetailIds IDs of deleted related details. */
    QList<domain_id> deletedDetailIds{};

    TransactionsData() = default;
    TransactionsData(
        QList<const Transaction*> transactions,
        QList<const TransactionDetail*> details,
        const QList<domain_id> deletedIds,
        const QList<domain_id> deletedDetailIds
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
