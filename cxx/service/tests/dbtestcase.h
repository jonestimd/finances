#ifndef DB_TEST_CASE_H
#define DB_TEST_CASE_H

#include <QDate>
#include <QVariant>
#include "QDecNumber.hh" // IWYU pragma: keep
#include "service/database/connectionpool.h"
#include "service/database/accountdao.h"
#include "service/database/companydao.h"
#include "service/database/categorydao.h"
#include "service/database/payeedao.h"
#include "service/database/securitydao.h"
#include "service/database/securitylotdao.h"
#include "service/database/stocksplitdao.h"
#include "service/database/transactiondao.h"
#include "service/database/transactiondetaildao.h"
#include "service/database/transactiongroupdao.h"

#define TEST_USER "test"

#define DECIMAL_VARIANT(value) QVariant::fromValue(QDecNumber{value})

#define GET_DETAILS(txDetails) std::get<1>(txDetails)

#define SKIP_FOR(...) \
QFETCH_GLOBAL(QString, driver); \
    if (QStringList{__VA_ARGS__}.contains(driver)) QSKIP("wrong driver");

#define RUN_FOR(...) \
QFETCH_GLOBAL(QString, driver); \
    if (!QStringList{__VA_ARGS__}.contains(driver)) QSKIP("wrong driver");

class Transaction;
class TransactionDetail;

struct Daos {
    CompanyDao companyDao;
    AccountDao accountDao;
    CategoryDao categoryDao;
    TransactionGroupDao transactionGroupDao;
    PayeeDao payeeDao;
    SecurityDao securityDao;
    TransactionDao transactionDao;
    TransactionDetailDao detailDao;
    StockSplitDao stockSplitDao;
    SecurityLotDao securityLotDao;

    Daos(const QString &dbType);
};

namespace factory {
    Transaction *transaction(QVariant accountId, QVariant payeeId = QVariant{}, QVariant securityId = QVariant{}, const QDate &date = QDate::currentDate());
    PendingTransaction *pendingTransaction(QVariant accountId, QList<const char*> amounts, QVariant payeeId = QVariant{}, QVariant securityId = QVariant{}, const QDate &date = QDate::currentDate());
    TransactionDetail *detail(const QVariant &txId, const char *amount = "1.00");
}

class DbTestCase {
    QHash<QString, ConnectionPool*> connectionPools{};

    Daos pgDaos;
    Daos mysqlDaos;
    Daos sqliteDaos;

public:
    QList<const Account*> accounts{};
    QList<const Transaction*> transactions{};
    QList<const TransactionDetail*> details{};

    typedef std::tuple<Transaction*, QList<TransactionDetail*>> TxDetails;

    DbTestCase();

    QList<QString> connectionPoolNames();

    ConnectionPool *connectionPool(QString name);

    CompanyDao &companyDao(const QString &driver);
    AccountDao &accountDao(const QString &driver);
    CategoryDao &categoryDao(const QString &driver);
    PayeeDao &payeeDao(const QString &driver);
    SecurityDao &securityDao(const QString &driver);
    StockSplitDao &stockSplitDao(const QString &driver);
    TransactionDao &transactionDao(const QString &driver);
    TransactionDetailDao &detailDao(const QString &driver);
    
    void createDatabases();

    QVariant addCompany(const QString &driver, const QString &name);
    Account *addAccount(const QString &driver, const QString &name, const QString &type, const QVariant companyId = QVariant{});
    QVariant addPayee(const QString &driver, const QString &name);
    QVariant addSecurity(const QString &driver, const QString &name, const char *type = SecurityType::stock.code);
    QVariant addCategory(const QString &driver, const QString &name);

    const Account *loadAccount(const QString &driver, QVariant id);

    QList<TxDetails> saveTransfer(const QString& driver, const QVariant& accountId, const QVariant& altAccountId, QList<const char*> amounts);
    TxDetails saveTransaction(const Transaction* unsaved, const QList<const char*> &detailAmounts, const QList<const char*> &detailShares = QList<const char*>{});
    TxDetails saveTransaction(const QString& driver, const Transaction* unsaved, const QList<const char*> &detailAmounts, const QList<const char*> &detailShares = QList<const char*>{});

    void cleanup();
    void resetDatabase(const QString& driver);

private:
    void addConnection(const QString &name, const ConnectionSettings &settings);
};

#endif // DB_TEST_CASE_H
