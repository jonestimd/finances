#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool)
    : pool{pool}
    , daos{pool->dbType()}
    , accountService{pool, daos.accountDao, daos.companyDao}
    , companyService{pool, daos.companyDao}
    , payeeService{pool, daos.payeeDao, daos.transactionDao}
    , categoryService{pool, daos.categoryDao, daos.transactionDetailDao}
    , groupService{pool, daos.transactionGroupDao}
    , securityService{pool, daos.securityDao, daos.stockSplitDao}
    , transationDetailService{pool, daos.transactionDetailDao}
    , transationService{pool, daos.transactionDao, daos.transactionDetailDao}
{}

ServiceContext::ServiceContext(const ConnectionSettings &settings) : ServiceContext{new ConnectionPool(settings)} {}

ServiceContext::~ServiceContext() {
    delete pool;
}

const ConnectionSettings& ServiceContext::connectionSettings() const {
    return pool->settings;
}

void ServiceContext::shutdown() {
    pool->shutdown();
}
