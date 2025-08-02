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
    CategoryStore *const categoryStore;
    QList<qlonglong> loadedAccounts{};
    QHash<qlonglong, QList<qlonglong>> idsByAccountId{};

public:
    TransactionDetailStore detailStore;

    TransactionStore(ServiceContext *serviceContext, CategoryStore *categoryStore);

    bool load(EntityView *view, qlonglong accountId, bool reload = false);

    void update(QWidget *source, TransactionTableModel *model);

    const QList<qlonglong> transactionIds(qlonglong accountId) const;

    QDecNumber amount(const QVariant &transactionId) const;

Q_SIGNALS:
    void accountLoaded(qlonglong id);

protected:
    void setValues(qlonglong accountId, const QHash<qlonglong, const Transaction*> values) override;

    using EntityStore::update;
    void updateDetails(const QList<const TransactionDetail*> &updates, const QList<const TransactionDetail*> deletes, const QList<const Transaction*> txDeletes);
};

#endif // TRANSACTIONSTORE_H
