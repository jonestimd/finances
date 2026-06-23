#include "transactiondetailstore.h"
#include "service/model/transaction.h"

TransactionDetailStore::TransactionDetailStore(TransactionDetailService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

void TransactionDetailStore::load(qlonglong accountId) {
    setValues(accountId, service->getAll(accountId));
}

void TransactionDetailStore::replaceCategory(const QVariant oldCategoryId, const QVariant newCategoryId) {
    QList<const TransactionDetail*> updates;
    forEachEntry([&](qlonglong id, const TransactionDetail* detail) {
        if (detail->categoryId == oldCategoryId) {
            auto updatedDetail = new TransactionDetail(*detail);
            updatedDetail->categoryId = newCategoryId;
            updatedDetail->version = detail->version.toLongLong() + 1;
            updates.append(updatedDetail);
        }
    });
    if (!updates.isEmpty()) update(updates, {});
}

void TransactionDetailStore::update(const TransactionsData& updates, const QList<const TransactionDetail*>& deletes, const QList<const Transaction*>& txDeletes) {
    auto allDeletes = QList<const TransactionDetail*>{deletes};
    for (auto& detailId : updates.deletedDetailIds) {
        if (contains(detailId.toLongLong())) allDeletes.append(value(detailId.toLongLong()));
    }
    for (auto tx : txDeletes) {
        for (auto& detailId : tx->detailIds) {
            if (contains(detailId.toLongLong())) {
                auto detail = value(detailId.toLongLong());
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
