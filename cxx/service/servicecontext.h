#ifndef SERVICECONTEXT_H
#define SERVICECONTEXT_H

#include "database/connectionpool.h"

#include "accountservice.h"
#include "companyservice.h"
#include "payeeservice.h"
#include "categoryservice.h"
#include "groupservice.h"
#include "securityservice.h"
#include "service/database/securitylotdao.h"
#include "transactionservice.h"
#include "transactiondetailservice.h"

class ServiceContext
{
    ConnectionPool *pool;
    bool borrowedPool;
    CompanyDao companyDao;
    AccountDao accountDao;
    CategoryDao categoryDao;
    TransactionGroupDao transactionGroupDao;
    PayeeDao payeeDao;
    SecurityDao securityDao;
    SecurityLotDao securityLotDao;
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

#ifndef __FINANCES_TEST__
private:
#endif
    ServiceContext(ConnectionPool *pool, bool borrowedPool = false);
public:
    ServiceContext(const ConnectionSettings &settings);
    ~ServiceContext();

    const ConnectionSettings& connectionSettings() const;

    void createDatabase(const QString& user, const QString& password);
    void createDatabaseTables(const QSqlDatabase& db);

    void shutdown();
};

#endif // SERVICECONTEXT_H
