#ifndef TRANSACTIONDETAILSERVICE_H
#define TRANSACTIONDETAILSERVICE_H

#include "entityservice.h"
#include "service/database/transactiondetaildao.h"
#include "service/model/transactiondetail.h"

class TransactionDetailService : public EntityService<TransactionDetail, TransactionDetailDao> {
public:
    TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao);
    
    QHash<domain_id, const TransactionDetail*> getAll(domain_id accountId);
};

#endif // TRANSACTIONDETAILSERVICE_H
