#include "transactionstore.h"
#include "transactiontablemodel.h"
#include "ui/widget/statusmessage.h"
#include <QDate>

Q_STATIC_LOGGING_CATEGORY(logger, "store.transaction")

class TransactionStore::TransactionSorter {
    const TransactionStore* const store;
    std::function<bool(qlonglong, qlonglong)> compare = std::bind_front(&TransactionSorter::lessThan, this);

public:
    TransactionSorter(TransactionStore* const store) : store{store} {}

    bool lessThan(qlonglong id1, qlonglong id2) const {
        auto d1 = store->value(id1)->date.toDate();
        auto d2 = store->value(id2)->date.toDate();
        return d1 == d2 ? id1 < id2 : d1 < d2;
    }

    void sort(QList<qlonglong>& ids) const {
        std::stable_sort(ids.begin(), ids.end(), compare);
    }

    int insert(QList<qlonglong>& ids, qlonglong id) const {
        for (int i = 0; i < ids.size(); i++) {
            if (lessThan(id, ids[i])) {
                ids.insert(i, id);
                return i;
            }
        }
        ids.append(id);
        return ids.size()-1;
    }

    bool inOrder(QList<qlonglong>& ids, int index) const {
        if (index > 0 && !lessThan(ids[index-1], ids[index])) return false;
        return index >= ids.size()-1 || lessThan(ids[index], ids[index+1]);
    }
};

TransactionStore::TransactionStore(ServiceContext* serviceContext, CategoryStore* categoryStore)
    : EntityStore{&serviceContext->transationService}
    , categoryStore{categoryStore}
    , detailStore{&serviceContext->transationDetailService}
    , sorter{new TransactionSorter(this)}
{}

TransactionStore::~TransactionStore() {
    delete sorter;
}

bool TransactionStore::load(EntityView *view, qlonglong accountId, bool reload) {
    if (reload || !loadedAccounts.contains(accountId)) {
        return EntityStore<Transaction, TransactionService, qlonglong>::load(view, tr(LOADING_TRANSACTIONS), accountId, true);
    }
    return true;
}

void TransactionStore::update(QWidget *source, TransactionTableModel *model, int txRow) {
    doInBackground(source, [=, this]() {
        auto adds = model->unsavedAdds(txRow);
        adds.removeIf([](const PendingTransaction* tx) -> bool { return tx->isEmpty(); });
        auto deletes = model->unsavedDeletes(txRow);
        auto detailDeletes = model->unsavedDetailDeletes(txRow);
        TransactionUpdate changes{model->unsavedChanges(txRow), adds, deletes,
                                  model->unsavedDetailChanges(txRow), model->unsavedDetailAdds(txRow), detailDeletes};
        auto updateData = service->update(changes, user);
        emitTransactionsUpdated(changes.deletes, updateData);
        if (!adds.isEmpty()) emit transactionsSaved(adds);
        for (const auto& id : std::as_const(updateData.deletedIds)) {
            if (contains(id)) deletes.append(value(id));
        }
        QSet<qlonglong> accountIds;
        // TODO filter details for unloaded accounts
        detailStore.update(updateData, detailDeletes, deletes);
        for (auto detail: std::as_const(updateData.details)) {
            auto tx = value(detail->transactionId);
            if (tx) accountIds.insert(tx->accountId.toLongLong());
        }
        // TODO use shared status bar model
        for (auto tx : updateData.transactions + deletes) accountIds.insert(tx->accountId.toLongLong());
        // TODO filter tx's for unloaded accounts
        update(updateData.transactions, deletes);
        for (auto accountId : accountIds) emit accountUpdated(accountId);
    });
}

void TransactionStore::replacePayee(const QVariant oldPayeeId, const QVariant newPayeeId) {
    QList<const Transaction*> updates;
    forEachEntry([&](qlonglong id, const Transaction* tx) {
        if (tx->payeeId == oldPayeeId) {
            auto updatedTx = new Transaction(*tx);
            updatedTx->payeeId = newPayeeId;
            updatedTx->version = tx->version.toLongLong() + 1;
            updates.append(updatedTx);
        }
    });
    if (!updates.isEmpty()) update(updates, {});
}

const QList<qlonglong> TransactionStore::transactionIds(qlonglong accountId) const {
    return idsByAccountId.value(accountId);
}

QDecNumber TransactionStore::amount(const QVariant &transactionId) const {
    QDecNumber total(0);
    auto tx = value(transactionId);
    for (const QVariant &detailId : tx->detailIds) {
        auto detail = detailStore.value(detailId);
        if (!detail->categoryId.isNull()) {
            auto category = categoryStore->value(detail->categoryId);
            if (!category) qCDebug(logger, "amount: category not loaded: %lld", detail->categoryId.toLongLong());
            else if (!AmountType::values.value(category->amountType.toString())->affectsBalance) continue;
        }
        total += detail->amount.value<QDecNumber>();
    }
    return total;
}

void TransactionStore::clearData(qlonglong accountId) {
    if (idsByAccountId.contains(accountId)) {
        const auto transactionIds = idsByAccountId.take(accountId);
        QList<qlonglong> detailIds;
        for (auto txId : transactionIds) {
            for (auto& detailId : std::as_const(value(txId)->detailIds)) detailIds.append(detailId.toLongLong());
        }
        removeValues(transactionIds);
        detailStore.removeValues(detailIds);
        loadedAccounts.removeOne(accountId);
    }
}

void TransactionStore::setValues(qlonglong accountId, const QHash<qlonglong, const Transaction*> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
    detailStore.load(accountId);
    QList<qlonglong> accountTxIds{};
    for (auto [id, tx] : values.asKeyValueRange()) {
        if (tx->accountId.toLongLong() == accountId) accountTxIds.append(tx->id.toLongLong());
    }
    sorter->sort(accountTxIds);
    idsByAccountId.insert(accountId, accountTxIds);
    emit accountLoaded(accountId);
}

void TransactionStore::update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) {
    for (auto tx : deletes) {
        auto accountId = tx->accountId.toLongLong();
        if (idsByAccountId.contains(accountId)) {
            auto& txIds = idsByAccountId[accountId];
            auto index = txIds.indexOf(tx->id.toLongLong());
            if (index >= 0) {
                txIds.remove(index);
                emit transactionRemoved(accountId, index);
            }
        }
    }
    QHash<qlonglong, int> detailCounts;
    for (auto tx : updates) {
        if (contains(tx->id)) detailCounts.insert(tx->id.toLongLong(), value(tx->id)->detailIds.size());
    }
    EntityStore::update(updates, deletes);
    for (auto tx : updates) {
        auto accountId = tx->accountId.toLongLong();
        if (idsByAccountId.contains(accountId)) {
            auto& txIds = idsByAccountId[accountId];
            auto txId = tx->id.toLongLong();
            auto index = txIds.indexOf(txId);
            if (index < 0) emit transactionAdded(accountId, sorter->insert(txIds, txId));
            else {
                if (sorter->inOrder(txIds, index))
                    emit transactionUpdated(accountId, index, detailCounts.value(txId));
                else {
                    txIds.remove(index);
                    emit transactionRemoved(accountId, index);
                    emit transactionAdded(accountId, sorter->insert(txIds, txId));
                }
            }
        }
    }
}

void TransactionStore::emitTransactionsUpdated(const QList<const Transaction *> deletes, const TransactionsData& updates) {
    QList<TransactionChange> deltas;
    for (auto tx : deletes) deltas.append(TransactionChange{tx, nullptr});
    for (auto tx : std::as_const(updates.transactions)) deltas.append(TransactionChange{value(tx->id), tx});
    for (const auto& txId : std::as_const(updates.deletedIds)) deltas.append(TransactionChange{value(txId), nullptr});
    emit transactionsUpdated(deltas);
}
