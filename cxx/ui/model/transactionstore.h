#ifndef TRANSACTIONSTORE_H
#define TRANSACTIONSTORE_H

#include "entitystore.h"
#include "transactiondetailstore.h"
#include "service/model/transaction.h"
#include "service/servicecontext.h"

class TransactionStore : public EntityStore<Transaction, TransactionService, qlonglong> {
    Q_OBJECT
    QList<qlonglong> loadedAccounts{};
    QHash<qlonglong, QList<qlonglong>> idsByAccountId{};

public:
    TransactionDetailStore detailStore;

    TransactionStore(ServiceContext *serviceContext);

    bool load(EntityView *view, qlonglong accountId, bool reload = false);

    const QList<qlonglong> transactionIds(qlonglong accountId) const;

    QDecNumber amount(const QVariant &transactionId) const;

Q_SIGNALS:
    void accountLoaded(qlonglong id);

protected:
    void setValues(qlonglong accountId, const QHash<qlonglong, const Transaction*> values) override;
};

#endif // TRANSACTIONSTORE_H
