#include "payeestore.h"

PayeeStore::PayeeStore(PayeeService *service) : EntityStore{service} {}

void PayeeStore::mergePayees(QWidget *source, const Payee *payee, const QVariant destinationId) {
    doInBackground(source, [this, payee, destinationId] {
        auto payees = service->merge(payee, destinationId, user);
        update(payees, QList{payee});
        emit valuesLoaded(ids());
    });
}
