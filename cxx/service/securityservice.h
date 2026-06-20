#ifndef SECURITYSERVICE_H
#define SECURITYSERVICE_H

#include "entityservice.h"
#include "service/database/securitydao.h"
#include "service/database/stocksplitdao.h"

class SecurityService : public EntityService<Security, SecurityDao> {
    StockSplitDao &stockSplitDao;

public:
    SecurityService(ConnectionPool *connectionPool, SecurityDao &securityDao, StockSplitDao &stockSplitDao);

    QHash<qlonglong, const StockSplit*> getSplits();
};

#endif // SECURITYSERVICE_H
