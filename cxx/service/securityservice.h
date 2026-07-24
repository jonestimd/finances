#ifndef SECURITYSERVICE_H
#define SECURITYSERVICE_H

#include "entityservice.h"
#include "service/database/securitydao.h"
#include "service/database/stocksplitdao.h"

class SecurityService : public EntityService<Security, SecurityDao> {
    StockSplitDao &stockSplitDao;

public:
    SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao, StockSplitDao &stockSplitDao);

    QHash<domain_id, const StockSplit*> getSplits();

    QMultiHash<domain_id, const AccountSecurity*> getAccountSecurities() const;
};

#endif // SECURITYSERVICE_H
