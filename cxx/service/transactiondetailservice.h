#ifndef TRANSACTIONDETAILSERVICE_H
#define TRANSACTIONDETAILSERVICE_H

#include "entityservice.h"
#include "service/database/transactiondetaildao.h"
#include "service/model/transactiondetail.h"

class SecurityLotDao;
class SecurityLot;

struct SecuritySaleData {
    /** @brief Current allocations. */
    QList<const SecurityLot*> lots;
    /** @brief Previous purchases. */
    QList<const SecurityPurchase*> purchases;
};

/**
 * @copydoc EntityService::getAll(GetAllArgs...)
 * @param accountId
 */
class TransactionDetailService : public EntityService<TransactionDetail, TransactionDetailDao, domain_id> {
    SecurityLotDao& securityLotDao;

public:
    TransactionDetailService(ConnectionPool *pool, TransactionDetailDao &transactionDetailDao, SecurityLotDao& securityLotDao);
    
    QList<const SearchTransactionDetail*> findTransactionDetails(const DetailSearchCriteria &criteria);

    SecuritySaleData findAvailableLots(const TransactionDetail* sale);
};

#endif // TRANSACTIONDETAILSERVICE_H
