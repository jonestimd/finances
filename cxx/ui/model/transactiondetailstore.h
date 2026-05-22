#ifndef TRANSACTIONDETAILSTORE_H
#define TRANSACTIONDETAILSTORE_H

#include "entitystore.h"
#include "service/transactiondetailservice.h"

class TransactionDetailStore : public EntityStore<TransactionDetail, TransactionDetailService, qlonglong> {
    friend class TransactionStore;

    QList<qlonglong> loadedAccounts{};

public:
    TransactionDetailStore(TransactionDetailService *service);

    void load(qlonglong accountId);

    void update(const TransactionsData& updates, const QList<const TransactionDetail*>& deletes, const QList<const Transaction*>& txDeletes);

protected:
    using EntityStore::update;
    void setValues(qlonglong accountId, QHash<qlonglong, const TransactionDetail*> values) override;
};

#endif // TRANSACTIONDETAILSTORE_H
