#include "transactiondetailstore.h"
#include "service/model/transaction.h"

TransactionDetailStore::TransactionDetailStore(TransactionDetailService *service)
    : EntityStore{service}
{}

void TransactionDetailStore::load(qlonglong accountId) {
    setValues(accountId, service->getAll(accountId));
}

void TransactionDetailStore::update(const TransactionsData& updates, const QList<const TransactionDetail*>& deletes, const QList<const Transaction*>& txDeletes) {
    auto allDeletes = QList<const TransactionDetail*>{deletes};
    for (auto& detailId : updates.deletedDetailIds) {
        if (contains(detailId)) allDeletes.append(value(detailId));
    }
    for (auto tx : txDeletes) {
        for (auto& detailId : tx->detailIds) {
            if (contains(detailId)) {
                auto detail = value(detailId);
                allDeletes.append(detail);
            }
        }
    }
    EntityStore::update(updates.details, allDeletes);
}

void TransactionDetailStore::setValues(qlonglong accountId, QHash<qlonglong, const TransactionDetail *> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
}
