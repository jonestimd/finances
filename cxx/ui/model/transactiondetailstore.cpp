#include "transactiondetailstore.h"
#include "service/model/transaction.h"

TransactionDetailStore::TransactionDetailStore(TransactionDetailService *service)
    : EntityStore{service}
{}

void TransactionDetailStore::load(qlonglong accountId) {
    setValues(accountId, service->getAll(accountId));
}

void TransactionDetailStore::update(const QList<const TransactionDetail*> &updates, const QList<const TransactionDetail*> deletes, const QList<const Transaction*> txDeletes) {
    auto allDeletes = QList<const TransactionDetail*>{deletes};
    for (auto tx : txDeletes) {
        for (auto &detailId : tx->detailIds) {
            if (contains(detailId)) {
                auto detail = value(detailId);
                allDeletes.append(detail);
                if (!detail->relatedDetailId.isNull() && contains(detail->relatedDetailId)) {
                    allDeletes.append(value(detail->relatedDetailId));
                }
            }
        }
    }
    for (auto detail : deletes) {
        if (!detail->relatedDetailId.isNull() && contains(detail->relatedDetailId)) {
            allDeletes.append(value(detail->relatedDetailId));
        }
    }
    for (auto updated : updates) {
        auto id = updated->id.toLongLong();
        if (contains(id)) {
            auto oldRelatedId = value(id)->relatedDetailId;
            if (!oldRelatedId.isNull() && updated->relatedDetailId.isNull()) {
                if (contains(oldRelatedId)) allDeletes.append(value(oldRelatedId));
            }
        }
    }
    EntityStore::update(updates, allDeletes);
}

void TransactionDetailStore::setValues(qlonglong accountId, QHash<qlonglong, const TransactionDetail *> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
}
