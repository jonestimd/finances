#ifndef DAOCONTEXT_H
#define DAOCONTEXT_H

#include "accountdao.h"
#include "categorydao.h"
#include "companydao.h"
#include "payeedao.h"
#include "securitydao.h"
#include "securitylotdao.h"
#include "stocksplitdao.h"
#include "transactiondao.h"
#include "transactiondetaildao.h"
#include "transactiongroupdao.h"

struct DaoContext {
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

    explicit DaoContext(const QString& dbType);

    void createDatabase(const ConnectionSettings& settings, const QString& adminUser, const QString& adminPassword);
    void createDatabaseTables(const QSqlDatabase& db);
};

#endif // DAOCONTEXT_H