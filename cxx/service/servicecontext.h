#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"
#include "payeeservice.h"
#include "categoryservice.h"
#include "groupservice.h"
#include "securityservice.h"
#include "service/database/daocontext.h"
#include "transactionservice.h"
#include "transactiondetailservice.h"

class ServiceContext
{
    ConnectionPool *pool;
    DaoContext daos;

public:
    AccountService accountService;
    CompanyService companyService;
    PayeeService payeeService;
    CategoryService categoryService;
    GroupService groupService;
    SecurityService securityService;
    TransactionDetailService transationDetailService;
    TransactionService transationService;

    ServiceContext(ConnectionPool *pool);
    ServiceContext(const ConnectionSettings &settings);
    ~ServiceContext();

    const ConnectionSettings& connectionSettings() const;

    void shutdown();
};

#endif // SERVICECONTEXT_H
