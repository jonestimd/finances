#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"

class ServiceContext
{
    ConnectionPool *pool;
public:
    AccountService accountService;
    CompanyService companyService;

    ServiceContext(ConnectionPool *pool);
};

#endif // SERVICECONTEXT_H
