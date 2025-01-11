#ifndef PAYEESERVICE_H
#define PAYEESERVICE_H

#include "database/connectionpool.h"
#include "entityservice.h"
#include "database/payeedao.h"

class PayeeService : public EntityService<Payee, PayeeDao>
{
public:
    PayeeService(ConnectionPool *connectionPool);

    QList<const Payee*> merge(const Payee *payee, const QVariant destinationId, const QString &user);
};

#endif // PAYEESERVICE_H
