#ifndef TRANSACTIONSERVICE_H
#define TRANSACTIONSERVICE_H

#include "entityservice.h"
#include "database/transactiondao.h"
#include "database/transactiondetaildao.h"

class TransactionService : EntityService<Transaction, TransactionDao> {
    TransactionDetailDao &detailDao;

public:
    TransactionService(ConnectionPool *pool, TransactionDao &transactionDao, TransactionDetailDao &detailDao);

    QHash<qlonglong, const Transaction*> getAll(qlonglong accountId);

    const TransactionsData update(TransactionUpdate &changes, const QString &user);
};

#endif // TRANSACTIONSERVICE_H
