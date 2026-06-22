#include "payeestore.h"
#include "datastore.h"
#include "ui/widget/statusmessage.h"

PayeeStore::PayeeStore(PayeeService *service, DataStore *dataStore)
    : EntityStore{service, &dataStore->messageStore}
    , dataStore{dataStore}
{}

void PayeeStore::mergePayees(QWidget *source, const Payee *payee, const QVariant destinationId) {
    messageStore->addMessage(tr(SAVING_PAYEES));
    doInBackground(source, [this, payee, destinationId] {
        auto payees = service->merge(payee, destinationId, user);
        dataStore->transactionStore->replacePayee(payee->id, destinationId);
        update(payees.values(), QList{payee});
        emit valuesLoaded(ids());
        QMetaObject::invokeMethod(messageStore, &StatusMessageStore::removeMessage, Qt::QueuedConnection, tr(SAVING_PAYEES));
    });
}

void PayeeStore::transactionsUpdated(const QList<TransactionChange> changes) {
    if (updateTransactionCounts(changes, &Transaction::payeeId)) {
        emit valuesLoaded(ids());
    }
}
