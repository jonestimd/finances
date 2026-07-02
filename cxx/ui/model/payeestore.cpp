#include "payeestore.h"
#include "datastore.h"
#include "ui/widget/statusmessage.h"

PayeeStore::PayeeStore(PayeeService *service, DataStore *dataStore)
    : EntityStore{service, &dataStore->messageStore}
    , dataStore{dataStore}
{}

void PayeeStore::mergePayees(QWidget *source, const Payee *payee, domain_id destinationId) {
    doInBackground(source, tr(SAVING_PAYEES), [this, payee, destinationId] {
        auto payees = service->merge(payee, destinationId, user);
        dataStore->transactionStore->replacePayee(payee->id.value(), destinationId);
        update(payees.values(), QList{payee});
        emit valuesLoaded(ids());
    });
}

void PayeeStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts(changes, &Transaction::payeeId)) {
        emit valuesLoaded(ids());
    }
}
