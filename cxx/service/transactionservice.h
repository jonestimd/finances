#ifndef TRANSACTIONSERVICE_H
#define TRANSACTIONSERVICE_H

#include "entityservice.h"
#include "service/database/transactiondao.h"

class TransactionService : EntityService<Transaction, TransactionDao> {
public:
    TransactionService(ConnectionPool *pool);

    QHash<qlonglong, const Transaction*> getAll(qlonglong accountId);

    const TransactionsData update(TransactionUpdate &changes, const QString &user);
};

#endif // TRANSACTIONSERVICE_H
