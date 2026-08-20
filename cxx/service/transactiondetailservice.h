#ifndef TRANSACTIONDETAILSERVICE_H
#define TRANSACTIONDETAILSERVICE_H

#include "entityservice.h"
#include "service/database/transactiondetaildao.h"
#include "service/model/transactiondetail.h"

/**
 * @copydoc EntityService::getAll(GetAllArgs...)
 * @param accountId
 */
class TransactionDetailService : public EntityService<TransactionDetail, TransactionDetailDao, domain_id> {
public:
    TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao);
    
    QList<const SearchTransactionDetail*> findTransactionDetails(const DetailSearchCriteria &criteria);
};

#endif // TRANSACTIONDETAILSERVICE_H
