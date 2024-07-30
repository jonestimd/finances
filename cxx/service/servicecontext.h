#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"
#include "payeeservice.h"

class ServiceContext
{
    ConnectionPool *pool;
public:
    AccountService accountService;
    CompanyService companyService;
    PayeeService payeeService;

    ServiceContext(ConnectionPool *pool);
};

#endif // SERVICECONTEXT_H
