#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"
#include "payeeservice.h"
#include "categoryservice.h"
#include "groupservice.h"
#include "securityservice.h"
#include "transactionservice.h"
#include "transactiondetailservice.h"

class ServiceContext
{
    ConnectionPool *pool;
    CompanyDao companyDao;
    AccountDao accountDao;
    CategoryDao categoryDao;
    TransactionGroupDao transactionGroupDao;
    PayeeDao payeeDao;
    SecurityDao securityDao;
    StockSplitDao stockSplitDao;
    TransactionDao transactionDao;
    TransactionDetailDao transactionDetailDao;

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
