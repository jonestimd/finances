#include "transactionstore.h"
#include "ui/widget/statusmessage.h"
#include <QDate>

TransactionStore::TransactionStore(ServiceContext *serviceContext)
    : EntityStore{&serviceContext->transationService}
    , detailStore{&serviceContext->transationDetailService}
{}

bool TransactionStore::load(EntityView *view, qlonglong accountId, bool reload) {
    if (reload || !loadedAccounts.contains(accountId)) {
        return EntityStore<Transaction, TransactionService, qlonglong>::load(view, tr(LOADING_TRANSACTIONS), accountId, true);
    }
    return false;
}

const QList<qlonglong> TransactionStore::transactionIds(qlonglong accountId) const {
    return idsByAccountId.value(accountId);
}

QDecNumber TransactionStore::amount(const QVariant &transactionId) const {
    QDecNumber total(0);
    auto tx = value(transactionId);
    for (const QVariant &detailId : tx->detailIds) {
        auto detail = detailStore.value(detailId);
        total += detail->amount.value<QDecNumber>();
    }
    return total;
}

void TransactionStore::setValues(qlonglong accountId, const QHash<qlonglong, const Transaction*> values) {
    loadedAccounts.append(accountId);
    EntityStore::setValues(accountId, values);
    detailStore.load(accountId);
    QList<qlonglong> accountTxIds{};
    for (auto [id, tx] : values.asKeyValueRange()) {
        if (tx->accountId.toLongLong() == accountId) accountTxIds.append(tx->id.toLongLong());
    }
    std::stable_sort(accountTxIds.begin(), accountTxIds.end(), [=, this](qlonglong id1, qlonglong id2) -> bool {
        auto tx1 = value(id1);
        auto tx2 = value(id2);
        return tx1 && tx2 && tx1->date.toDate() < tx2->date.toDate() || id1 < id2;
    });
    idsByAccountId[accountId] = accountTxIds;
    emit accountLoaded(accountId);
}
