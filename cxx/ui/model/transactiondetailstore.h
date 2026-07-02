#ifndef TRANSACTIONDETAILSTORE_H
#define TRANSACTIONDETAILSTORE_H

#include "entitystore.h"
#include "service/transactiondetailservice.h"

class TransactionDetailStore : public EntityStore<TransactionDetail, TransactionDetailService, domain_id> {
    friend class TransactionStore;

    QList<domain_id> loadedAccounts{};

public:
    TransactionDetailStore(TransactionDetailService *service, StatusMessageStore* messageStore);

    void load(domain_id accountId);

    void replaceCategory(const optional_id oldCategoryId, const optional_id newCategoryId);

    void update(const TransactionsData& updates, const QList<const TransactionDetail*>& deletes, const QList<const Transaction*>& txDeletes);

protected:
    using EntityStore::update;
    void setValues(domain_id accountId, QHash<domain_id, const TransactionDetail*> values) override;
};

#endif // TRANSACTIONDETAILSTORE_H
