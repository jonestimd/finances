#ifndef TRANSACTIONSTORE_H
#define TRANSACTIONSTORE_H

#include "categorystore.h"
#include "entitystore.h"
#include "transactiondetailstore.h"
#include "service/model/transactiondetail.h"
#include "service/model/transaction.h"
#include "service/servicecontext.h"

class TransactionsWindow;
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
    void replacePayee(domain_id oldPayeeId, domain_id newPayeeId);
    void findRecentForPayee(domain_id accountId, domain_id payeeId) const;
    void findRecentForSecurity(domain_id accountId, domain_id securityId) const;
private:
    void findRecent(domain_id accountId, domain_id searchId, QList<PendingTransaction*> (TransactionService::*search)(domain_id, domain_id)) const;

public:
    const QList<domain_id> transactionIds(domain_id accountId) const;

    QDecNumber amount(domain_id transactionId) const;

    /** @brief Sorts `ids` using transaction date. */
    void sort(QList<domain_id>& txIds) const;
    /** @return true if the sort order of `txId1` is less than `txId2`. */
    bool lessThan(domain_id txId1, domain_id txId2) const;

    void clearData(domain_id accountId);

    void moveTransaction(TransactionsWindow* window, const Transaction* transaction, domain_id accountId);
    void findTransactions(QWidget* window, const QString text);

Q_SIGNALS:
    void accountLoaded(domain_id id);
    void accountUpdated(domain_id id);
    void transactionsSaved(const QList<const PendingTransaction*> transactions); // clazy:exclude=fully-qualified-moc-types
    void transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges);
    void showRecents(QList<PendingTransaction*> recents) const; // clazy:exclude=fully-qualified-moc-types
    void showTransactions(QList<const SearchTransactionDetail*> recents) const; // clazy:exclude=fully-qualified-moc-types

protected:
    void setValues(domain_id accountId, const QHash<domain_id, const Transaction*> values) override;

    virtual void update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) override;

private:
    QHash<domain_id, TransactionChange> transactionChanges(const QList<const Transaction*> deletes, const TransactionsData& updates);
    QHash<domain_id, DetailChange> detailChanges(const TransactionUpdate& change, const TransactionsData& updates) const;
    Q_INVOKABLE void applyUpdates(const QList<const PendingTransaction*> adds, QSharedPointer<TransactionUpdate> changes, TransactionsData updateData);
};

#endif // TRANSACTIONSTORE_H
