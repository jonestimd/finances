#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool)
    : pool{pool}
    , accountService{pool}
    , companyService{pool}
    , payeeService{pool} {}
