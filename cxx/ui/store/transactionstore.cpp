#include "transactionstore.h"
#include "ui/model/transactiontablemodel.h"
#include "ui/widget/statusmessage.h"
#include "ui/widget/transactionswindow.h"
#include <QDate>
#include <QWindow>

Q_STATIC_LOGGING_CATEGORY(logger, "store.transaction")

TransactionStore::TransactionStore(ServiceContext* serviceContext, DataStore *dataStore)
    : EntityStore{&serviceContext->transationService, &dataStore->messageStore}
    , categoryStore{dataStore->categoryStore}
    , detailStore{&serviceContext->transationDetailService, &dataStore->messageStore}
{}

bool TransactionStore::load(EntityView *view, domain_id accountId, bool reload) {
    if (reload || !loadedAccounts.contains(accountId)) {
        return EntityStore<Transaction, TransactionService, domain_id>::load(view, tr(LOADING_TRANSACTIONS), accountId, true);
    }
    return true;
}

void TransactionStore::update(QWidget *source, TransactionTableModel *model, const QString message, int txRow) {
    doInBackground(source, message, [=, this]() {
        auto adds = model->unsavedAdds(txRow);
        adds.removeIf([](const PendingTransaction* tx) -> bool { return tx->isEmpty(); });
        auto deletes = model->unsavedDeletes(txRow);
        auto detailDeletes = model->unsavedDetailDeletes(txRow);
        QSharedPointer<TransactionUpdate> changes{
            new TransactionUpdate{model->unsavedChanges(txRow), adds, deletes,
                                  model->unsavedDetailChanges(txRow), model->unsavedDetailAdds(txRow), detailDeletes}};
        auto updateData = service->update(*changes, user);
        QMetaObject::invokeMethod(this, &TransactionStore::applyUpdates, Qt::QueuedConnection, adds, changes, updateData);
    });
}

void TransactionStore::applyUpdates(const QList<const PendingTransaction*> adds, QSharedPointer<TransactionUpdate> changes, TransactionsData updateData) {
    emit transactionsUpdated(transactionChanges(changes->deletes, updateData), detailChanges(*changes, updateData));
    if (!adds.isEmpty()) emit transactionsSaved(adds);
    auto deletes = changes->deletes;
    for (const auto& id : std::as_const(updateData.deletedIds)) {
        if (contains(id)) deletes.append(value(id));
    }
    QSet<domain_id> accountIds;
    // TODO filter details for unloaded accounts
    auto detailDeletes = changes->detailDeletes;
    detailStore.update(updateData, detailDeletes, deletes);
    for (auto detail: std::as_const(updateData.details)) {
        auto tx = value(detail->transactionId);
        if (tx) accountIds.insert(tx->accountId);
    }
    for (auto tx : updateData.transactions + deletes) accountIds.insert(tx->accountId);
    // TODO filter tx's for unloaded accounts
    update(updateData.transactions, deletes);
    for (auto accountId : accountIds) emit accountUpdated(accountId);
}

void TransactionStore::replacePayee(domain_id oldPayeeId, domain_id newPayeeId) {
    QList<const Transaction*> updates;
    forEachEntry([&](domain_id id, const Transaction* tx) {
        if (tx->payeeId.has_value() && tx->payeeId.value() == oldPayeeId) {
            auto updatedTx = new Transaction(*tx);
            updatedTx->payeeId = newPayeeId;
            updatedTx->version = tx->version + 1;
            updates.append(updatedTx);
        }
    });
    if (!updates.isEmpty()) update(updates, {});
}

void TransactionStore::findRecentForPayee(domain_id accountId, domain_id payeeId) const {
    TransactionsWindow *window = qobject_cast<TransactionsWindow *>(qApp->activeWindow());
    if (window) {
        doInBackground(window, tr(LOADING_TRANSACTIONS), [=, this]() {
            auto transactions = service->getRecentForPayee(accountId, payeeId);
            QMetaObject::invokeMethod(window, &TransactionsWindow::showRecent, transactions);
        });
    } else {
        auto focusWindow = qApp->focusWindow();
        qWarning() << "expected a transactions window:" << (focusWindow ? focusWindow->title() : "no focus window");
    }
}

const QList<domain_id> TransactionStore::transactionIds(domain_id accountId) const {
    if (!loadedAccounts.contains(accountId)) return {};
    QList<domain_id> accountTxIds{};
    forEachEntry([=, &accountTxIds](domain_id id, const Transaction* tx) {
        if (tx->accountId == accountId) accountTxIds.append(id);
    });
    return accountTxIds;
}

QDecNumber TransactionStore::amount(domain_id transactionId) const {
    QDecNumber total(0);
    auto tx = value(transactionId);
    for (const QVariant &detailId : tx->detailIds) {
        auto detail = detailStore.value(detailId.toLongLong());
        if (detail->categoryId.has_value()) {
            auto category = categoryStore->value(detail->categoryId.value());
            if (!category) qCDebug(logger, "amount: category not loaded: %lld", detail->categoryId.value());
            else if (!category->amountType->affectsBalance) continue;
        }
        if (!detail->amount.isNaN()) total += detail->amount;
    }
    return total;
}

void TransactionStore::sort(QList<domain_id> &txIds) const {
    auto compare = std::bind_front(&TransactionStore::lessThan, this);
    std::stable_sort(txIds.begin(), txIds.end(), compare);
}

bool TransactionStore::lessThan(domain_id txId1, domain_id txId2) const {
    return *value(txId1) < *value(txId2);
}

void TransactionStore::clearData(domain_id accountId) {
    if (loadedAccounts.contains(accountId)) {
        const auto transactionIds = this->transactionIds(accountId);
        QList<domain_id> detailIds;
        for (auto txId : transactionIds) {
            for (auto& detailId : std::as_const(value(txId)->detailIds)) detailIds.append(detailId);
        }
        removeValues(transactionIds);
        detailStore.removeValues(detailIds);
        loadedAccounts.removeOne(accountId);
    }
}

void TransactionStore::setValues(domain_id accountId, const QHash<domain_id, const Transaction*> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
    detailStore.load(accountId);
    emit accountLoaded(accountId);
}

void TransactionStore::update(const QList<const Transaction*>& updates, const QList<const Transaction*> deletes) {
    QList<domain_id> detailCountIds;
    for (auto tx : updates) {
        if (contains(tx->id.value())) {
            auto oldCount = value(tx->id.value())->detailIds.size();
            if (tx->detailIds.size() != oldCount) detailCountIds.append(tx->id.value());
        }
    }
    EntityStore::update(updates, deletes);
    if (!detailCountIds.isEmpty()) emit valuesUpdated(detailCountIds);
}

QHash<domain_id, TransactionChange> TransactionStore::transactionChanges(const QList<const Transaction*> deletes, const TransactionsData& updates) {
    QHash<domain_id, TransactionChange> deltas;
    for (auto tx : deletes) deltas.insert(tx->id.value(), TransactionChange{tx, nullptr});
    for (auto tx : std::as_const(updates.transactions)) deltas.insert(tx->id.value(), TransactionChange{value(tx->id.value()), tx});
    for (const auto& txId : std::as_const(updates.deletedIds)) deltas.insert(txId, TransactionChange{value(txId), nullptr});
    return deltas;
}

QHash<domain_id, DetailChange> TransactionStore::detailChanges(const TransactionUpdate& change, const TransactionsData& updates) const {
    QHash<domain_id, DetailChange> deltas;
    for (auto tx : change.deletes) {
        for (const auto& detailId : tx->detailIds) deltas.insert(detailId, DetailChange{detailStore.value(detailId), nullptr});
    }
    for (const auto& txId : updates.deletedIds) {
        auto tx = value(txId);
        for (const auto& detailId : tx->detailIds) deltas.insert(detailId, DetailChange{detailStore.value(detailId), nullptr});
    }
    for (auto detail : change.detailDeletes) deltas.insert(detail->id.value(), DetailChange{detail, nullptr});
    for (auto detail : std::as_const(updates.details)) deltas.insert(detail->id.value(), DetailChange{detailStore.value(detail->id.value()), detail});
    for (const auto& detailId : std::as_const(updates.deletedDetailIds)) deltas.insert(detailId, DetailChange{detailStore.value(detailId), nullptr});
    return deltas;
}
