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

    /**
     * @return `TransactionsData`:
     *   * `transactions`:
     *     - input `adds` converted to `Transaction`
     *     - input `updates`
     *     - transactions for deleted details
     *     - transactions for updated details (change transfer account)
     *     - new related transactions for added details
     *     - new related transactions for updated details
     *   * `details`
     *     - input adds (from transaction `adds`)
     *     - input `detailUpdates`
     *     - new related details
     *     - updated related details
     *   * `deletedIds`
     *     - implicitly deleted transactions
     *       - for changes to transfer details
     *       - for deleted transfer (transaction or detail)
     *   * `deletedDetailIds`
     *     - implicitly deleted related details
     *       - for changes to transfer details
     *       - for deleted transfer transactions
     *       - NOT for DELETED transfer details
     */
    const TransactionsData update(TransactionUpdate &changes, const QString &user);
};

#endif // TRANSACTIONSERVICE_H
