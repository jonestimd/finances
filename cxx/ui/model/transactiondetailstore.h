#ifndef TRANSACTIONDETAILSTORE_H
#define TRANSACTIONDETAILSTORE_H

#include "entitystore.h"
#include "service/transactiondetailservice.h"

class TransactionDetailStore : public EntityStore<TransactionDetail, TransactionDetailService, qlonglong> {
    QList<qlonglong> loadedAccounts{};

public:
    TransactionDetailStore(TransactionDetailService *service);

    void load(qlonglong accountId);

protected:
    void setValues(qlonglong accountId, QHash<qlonglong, const TransactionDetail*> values) override;
};

#endif // TRANSACTIONDETAILSTORE_H
