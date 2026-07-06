#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool)
    : pool{pool}
    , companyDao{pool->dbType()}
    , accountDao{pool->dbType()}
    , categoryDao{pool->dbType()}
    , transactionGroupDao{pool->dbType()}
    , payeeDao{pool->dbType()}
    , securityDao{pool->dbType()}
    , stockSplitDao{pool->dbType()}
    , transactionDao{pool->dbType()}
    , transactionDetailDao{pool->dbType()}
    , accountService{pool, accountDao, companyDao}
    , companyService{pool, companyDao}
    , payeeService{pool, payeeDao, transactionDao}
    , categoryService{pool, categoryDao, transactionDetailDao}
    , groupService{pool, transactionGroupDao}
    , securityService{pool, securityDao, stockSplitDao}
    , transationDetailService{pool, transactionDetailDao}
    , transationService{pool, transactionDao, transactionDetailDao}
{}

ServiceContext::ServiceContext(const ConnectionSettings &settings) : ServiceContext{new ConnectionPool(settings)} {}

ServiceContext::~ServiceContext() {
    delete pool;
}

const QString ServiceContext::connectionName() const {
    return pool->settings.displayName();
}

const QString ServiceContext::connectionConfigName() const {
    return pool->settings.configName();
}

void ServiceContext::shutdown() {
    pool->shutdown();
}
