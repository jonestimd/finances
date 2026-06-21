#include "payeestore.h"
#include "datastore.h"

PayeeStore::PayeeStore(PayeeService *service, DataStore *dataStore)
    : EntityStore{service}
    , dataStore{dataStore}
{}

void PayeeStore::mergePayees(QWidget *source, const Payee *payee, const QVariant destinationId) {
    doInBackground(source, [this, payee, destinationId] {
        auto payees = service->merge(payee, destinationId, user);
        dataStore->transactionStore->replacePayee(payee->id, destinationId);
        update(payees.values(), QList{payee});
        emit valuesLoaded(ids());
    });
}

void PayeeStore::transactionsUpdated(const QList<TransactionChange> changes) {
    QSet<qlonglong> updateIds;
    for (auto change : changes) {
        auto oldTx = change.oldTransaction;
        auto newTx = change.newTransaction;
        if (oldTx && oldTx->payeeId.isValid() && (!newTx || newTx->payeeId != oldTx->payeeId)) {
            auto oldPayee = value(oldTx->payeeId);
            oldPayee->transactions = oldPayee->transactions.toInt() - 1;
            updateIds.insert(oldPayee->id.toLongLong());
        }
        if (newTx && newTx->payeeId.isValid() && (!oldTx || newTx->payeeId != oldTx->payeeId)) {
            auto newPayee = value(newTx->payeeId);
            newPayee->transactions = newPayee->transactions.toInt() + 1;
            updateIds.insert(newPayee->id.toLongLong());
        }
    }
    if (!updateIds.isEmpty()) emit valuesLoaded(ids());;
}
