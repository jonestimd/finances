#ifndef TRANSACTIONSTORE_H
#define TRANSACTIONSTORE_H

#include "categorystore.h"
#include "entitystore.h"
#include "transactiondetailstore.h"
#include "service/model/transaction.h"
#include "service/servicecontext.h"

class TransactionTableModel;

class TransactionStore : public EntityStore<Transaction, TransactionService, qlonglong> {
    Q_OBJECT

    class TransactionSorter;
    TransactionSorter *sorter;

    const CategoryStore *const categoryStore;

    QList<qlonglong> loadedAccounts{};
    /**
     * @brief idsByAccountId List of transaction IDs for each loaded account.  The lists are sorted by
     * transaction date/id to enable calculating the running balance.
     */
    QHash<qlonglong, QList<qlonglong>> idsByAccountId{};

public:
    TransactionDetailStore detailStore;

    TransactionStore(ServiceContext* serviceContext, CategoryStore* categoryStore);
    ~TransactionStore();

    bool load(EntityView *view, qlonglong accountId, bool reload = false);

    void update(QWidget *source, TransactionTableModel *model, int txRow = -1);
    void replacePayee(const QVariant oldPayeeId, const QVariant newPayeeId);

    const QList<qlonglong> transactionIds(qlonglong accountId) const;

    QDecNumber amount(const QVariant &transactionId) const;

    void clearData(qlonglong accountId);

Q_SIGNALS:
    void accountLoaded(qlonglong id);
    void accountUpdated(qlonglong id);
    void transactionsSaved(const QList<const PendingTransaction*>& transactions);
    void transactionAdded(qlonglong accountId, int index);
    void transactionRemoved(qlonglong accountId, int index);
    void transactionUpdated(qlonglong accountId, int index, int oldDetailCount);

protected:
    void setValues(qlonglong accountId, const QHash<qlonglong, const Transaction*> values) override;

    virtual void update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) override;

private:
    void sortTransactionIds(QList<qlonglong> &ids) const;
};

#endif // TRANSACTIONSTORE_H
