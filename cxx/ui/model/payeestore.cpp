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
    if (updateTransactionCounts(changes, &Transaction::payeeId)) {
        emit valuesLoaded(ids());
    }
}
