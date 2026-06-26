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

    class TransactionSorter;
    TransactionSorter *sorter;

    const CategoryStore *const categoryStore;

    QList<domain_id> loadedAccounts{};
    /**
     * @brief idsByAccountId List of transaction IDs for each loaded account.  The lists are sorted by
     * transaction date/id to enable calculating the running balance.
     */
    QHash<domain_id, QList<domain_id>> idsByAccountId{};

public:
    TransactionDetailStore detailStore;

    TransactionStore(ServiceContext* serviceContext, DataStore* dataStore);
    ~TransactionStore();

    bool load(EntityView *view, domain_id accountId, bool reload = false);

    void update(QWidget *source, TransactionTableModel *model, const QString message, int txRow = -1);
    void replacePayee(const QVariant oldPayeeId, const QVariant newPayeeId);

    const QList<domain_id> transactionIds(domain_id accountId) const;
    
    QDecNumber amount(domain_id transactionId) const;

    void clearData(domain_id accountId);

Q_SIGNALS:
    void accountLoaded(domain_id id);
    void accountUpdated(domain_id id);
    void transactionsSaved(const QList<const PendingTransaction*>& transactions);
    void transactionAdded(domain_id accountId, int index);
    void transactionRemoved(domain_id accountId, int index);
    void transactionUpdated(domain_id accountId, int index, int oldDetailCount);
    void transactionsUpdated(const QList<TransactionChange> changes);
    void detailsUpdated(const QList<DetailChange> changes);

protected:
    void setValues(domain_id accountId, const QHash<domain_id, const Transaction*> values) override;

    virtual void update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) override;

private:
    void sortTransactionIds(QList<domain_id> &ids) const;
    void emitTransactionsUpdated(const QList<const Transaction*> deletes, const TransactionsData& updates);
    void emitDetailsUpdated(const TransactionUpdate& change, const TransactionsData& updates);
};

#endif // TRANSACTIONSTORE_H
