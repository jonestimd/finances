#include "transactiondetailstore.h"

TransactionDetailStore::TransactionDetailStore(TransactionDetailService *service)
    : EntityStore{service}
{}

void TransactionDetailStore::load(qlonglong accountId) {
    setValues(accountId, service->getAll(accountId));
}

void TransactionDetailStore::setValues(qlonglong accountId, QHash<qlonglong, const TransactionDetail *> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
}
