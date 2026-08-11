#ifndef PAYEESERVICE_H
#define PAYEESERVICE_H

#include "database/connectionpool.h"
#include "entityservice.h"
#include "database/payeedao.h"
#include "database/transactiondao.h"

class PayeeService : public NamedEntityService<Payee, PayeeDao> {
    TransactionDao &transactionDao;

public:
    PayeeService(ConnectionPool *connectionPool, PayeeDao &payeeDao, TransactionDao &transactionDao);

    QList<const Payee*> merge(const Payee *payee, domain_id destinationId, const QString &user);
};

#endif // PAYEESERVICE_H
