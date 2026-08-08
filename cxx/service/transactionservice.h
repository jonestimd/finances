#ifndef TRANSACTIONSERVICE_H
#define TRANSACTIONSERVICE_H

#include "entityservice.h"
#include "database/transactiondao.h"
#include "database/transactiondetaildao.h"
#include "service/database/accountdao.h"

/**
 * @copydoc EntityService::getAll(GetAllArgs...)
 * @param accountId
 */
class TransactionService : public EntityService<Transaction, TransactionDao, domain_id> {
    TransactionDetailDao& detailDao;
    AccountDao& accountDao;

public:
    TransactionService(ConnectionPool* pool, TransactionDao& transactionDao, TransactionDetailDao& detailDao, AccountDao& accountDao);

    QList<PendingTransaction*> getRecentForPayee(domain_id accountId, domain_id payeeId);

    /**
     * @return `TransactionsData`:
     *   - `transactions`:
     *     - input `adds` converted to `Transaction`
     *     - input `updates`
     *     - transactions for deleted details
     *     - transactions for updated details (change transfer account)
     *     - new related transactions for added details
     *     - new related transactions for updated details
     *   - `details`:
     *     - input adds (from transaction `adds`)
     *     - input `detailUpdates`
     *     - new related details
     *     - updated related details
     *   - `deletedIds` - implicitly deleted transactions
     *     - for changes to transfer details
     *     - for deleted transfer (transaction or detail)
     *   - `deletedDetailIds` - implicitly deleted related details
     *     - for changes to transfer details
     *     - for deleted transfer transactions
     *     - \b NOT for \b DELETED transfer details
     */
    const TransactionsData update(TransactionUpdate &changes, const QString &user);
};

#endif // TRANSACTIONSERVICE_H
