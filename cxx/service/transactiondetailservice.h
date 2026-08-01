#ifndef TRANSACTIONDETAILSERVICE_H
#define TRANSACTIONDETAILSERVICE_H

#include "entityservice.h"
#include "service/database/transactiondetaildao.h"
#include "service/model/transactiondetail.h"

class TransactionDetailService : public EntityService<TransactionDetail, TransactionDetailDao, domain_id> {
public:
    TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao);
    
    /**
     * @copydoc EntityService::getAll(GetAllArgs...)
     * @param accountId
     */
};

#endif // TRANSACTIONDETAILSERVICE_H
