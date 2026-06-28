#ifndef TRANSACTIONSTORE_H
#define TRANSACTIONSTORE_H

#include "categorystore.h"
#include "entitystore.h"
#include "transactiondetailstore.h"
#include "service/model/transaction.h"
#include "service/servicecontext.h"

class TransactionTableModel;

class TransactionStore : public EntityStore<Transaction, TransactionService, domain_id> {
    Q_OBJECT
    const CategoryStore *const categoryStore;

    QList<domain_id> loadedAccounts{};

public:
    TransactionDetailStore detailStore;

    TransactionStore(ServiceContext* serviceContext, DataStore* dataStore);

    bool load(EntityView *view, domain_id accountId, bool reload = false);

    void update(QWidget *source, TransactionTableModel *model, const QString message, int txRow = -1);
    void replacePayee(const domain_id oldPayeeId, const domain_id newPayeeId);

    const QList<domain_id> transactionIds(domain_id accountId) const;
    
    QDecNumber amount(domain_id transactionId) const;

    /** @brief Sorts `ids` using transaction date. */
    void sort(QList<domain_id>& txIds) const;
    /** @return true if the sort order of `txId1` is less than `txId2`. */
    bool lessThan(domain_id txId1, domain_id txId2) const;

    void clearData(domain_id accountId);

Q_SIGNALS:
    void accountLoaded(domain_id id);
    void accountUpdated(domain_id id);
    void transactionsSaved(const QList<const PendingTransaction*> transactions); // clazy:exclude=fully-qualified-moc-types
    void transactionsUpdated(const QList<TransactionChange> changes);
    void detailsUpdated(const QList<DetailChange> changes);

protected:
    void setValues(domain_id accountId, const QHash<domain_id, const Transaction*> values) override;

    virtual void update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) override;

private:
    void emitTransactionsUpdated(const QList<const Transaction*> deletes, const TransactionsData& updates);
    void emitDetailsUpdated(const TransactionUpdate& change, const TransactionsData& updates);
};

#endif // TRANSACTIONSTORE_H
