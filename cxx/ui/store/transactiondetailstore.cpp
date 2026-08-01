#include "transactiondetailstore.h"
#include "service/model/transaction.h"

TransactionDetailStore::TransactionDetailStore(TransactionDetailService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

void TransactionDetailStore::load(domain_id accountId) {
    setValues(accountId, service->getAll(accountId));
}

void TransactionDetailStore::replaceCategory(const optional_id oldCategoryId, const optional_id newCategoryId) {
    QList<const TransactionDetail*> updates;
    forEachEntry([&](domain_id id, const TransactionDetail* detail) {
        if (detail->categoryId == oldCategoryId) {
            auto updatedDetail = new TransactionDetail(*detail);
            updatedDetail->categoryId = newCategoryId;
            updatedDetail->version = detail->version + 1;
            updates.append(updatedDetail);
        }
    });
    if (!updates.isEmpty()) update(updates, {});
}

void TransactionDetailStore::update(const TransactionsData& updates, const QList<const TransactionDetail*>& deletes, const QList<const Transaction*>& txDeletes) {
    auto allDeletes = QList<const TransactionDetail*>{deletes};
    for (auto& detailId : updates.deletedDetailIds) {
        if (contains(detailId)) allDeletes.append(value(detailId));
    }
    for (auto tx : txDeletes) {
        for (auto& detailId : tx->detailIds) {
            if (contains(detailId)) allDeletes.append(value(detailId));
        }
    }
    EntityStore::update(updates.details, allDeletes);
}

void TransactionDetailStore::setValues(domain_id accountId, QHash<domain_id, const TransactionDetail *> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
}
