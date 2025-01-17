#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool)
    : pool{pool}
    , accountService{pool}
    , companyService{pool}
    , payeeService{pool}
    , categoryService{pool}
    , groupService{pool}
    , securityService{pool}
{}

const QString ServiceContext::connectionName() const {
    return pool->displayName;
}
