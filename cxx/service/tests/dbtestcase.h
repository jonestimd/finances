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
#include "service/database/transactiondao.h"
#include "service/database/transactiondetaildao.h"
#include "service/database/transactiongroupdao.h"

#define TEST_USER "test"

#define DECIMAL_VARIANT(value) QVariant::fromValue(QDecNumber{value})

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

    Daos(const QString &dbType);
};

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
    TransactionDao &transactionDao(const QString &driver);
    TransactionDetailDao &detailDao(const QString &driver);

    void createDatabases();

    QVariant addCompany(QString driver, const QString &name);

    Account *addAccount(QString driver, const QString &name, const QString &type, const QVariant companyId = QVariant{});

    const Account *loadAccount(QString driver, const QVariant &id);

    QVariant addPayee(QString driver, const QString &name);

    Transaction unsavedTransaction(QDate date = QDate::currentDate());
    Transaction unsavedTransaction(QVariant accountId, QDate date = QDate::currentDate());

    TransactionDetail unsavedDetail(const char *amount = "1.00");

    TxDetails saveTransaction(QList<const char*> detailAmounts);
    TxDetails saveTransaction(QVariant accountId, QList<const char*> detailAmounts);

    void cleanup();

private:
    void addConnection(QString name, const ConnectionSettings &settings);
};

#endif // DB_TEST_CASE_H
