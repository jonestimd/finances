#ifndef PAYEESTORE_H
#define PAYEESTORE_H

#include "service/payeeservice.h"
#include "entitystore.h"

class PayeeStore : public EntityStore<Payee, PayeeService> {
    const DataStore* dataStore;

public:
    PayeeStore(PayeeService *service, DataStore* dataStore);

    void mergePayees(QWidget *source, const Payee *payee, const QVariant destinationId);
};

#endif // PAYEESTORE_H
