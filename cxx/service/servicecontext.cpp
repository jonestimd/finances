#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool)
    : pool{pool}
    , companyDao{pool->dbType()}
    , accountDao{pool->dbType()}
    , categoryDao{pool->dbType()}
    , transactionGroupDao{pool->dbType()}
    , payeeDao{pool->dbType()}
    , securityDao{pool->dbType()}
    , transactionDao{pool->dbType()}
    , transactionDetailDao{pool->dbType()}
    , accountService{pool, accountDao, companyDao}
    , companyService{pool, companyDao}
    , payeeService{pool, payeeDao, transactionDao}
    , categoryService{pool, categoryDao, transactionDetailDao}
    , groupService{pool, transactionGroupDao}
    , securityService{pool, securityDao}
    , transationDetailService{pool, transactionDetailDao}
    , transationService{pool, transactionDao, transactionDetailDao}
{}

const QString ServiceContext::connectionName() const {
    return pool->displayName;
}
