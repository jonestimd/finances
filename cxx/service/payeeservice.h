#ifndef PAYEESERVICE_H
#define PAYEESERVICE_H

#include "database/connectionpool.h"
#include "entityservice.h"
#include "database/payeedao.h"
#include "database/transactiondao.h"

class PayeeService : public EntityService<Payee, PayeeDao> {
    TransactionDao &transactionDao;

public:
    PayeeService(ConnectionPool *connectionPool, PayeeDao &payeeDao, TransactionDao &transactionDao);

    QHash<domain_id, const Payee*> merge(const Payee *payee, domain_id destinationId, const QString &user);
};

#endif // PAYEESERVICE_H
