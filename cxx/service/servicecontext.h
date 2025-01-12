#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"
#include "payeeservice.h"
#include "categoryservice.h"

class ServiceContext
{
    ConnectionPool *pool;
public:
    AccountService accountService;
    CompanyService companyService;
    PayeeService payeeService;
    CategoryService categoryService;

    ServiceContext(ConnectionPool *pool);

    const QString connectionName() const;
};

#endif // SERVICECONTEXT_H
