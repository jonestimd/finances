#ifndef DB_TEST_CASE_H
#define DB_TEST_CASE_H

#include <QDate>
#include <QVariant>
#include "QDecNumber.hh" // IWYU pragma: keep
#include "service/database/connectionpool.h"

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

class DbTestCase {
    QHash<QString, ConnectionPool*> connectionPools{};

public:
    QList<const Transaction*> transactions{};
    QList<const TransactionDetail*> details{};

    typedef std::tuple<Transaction*, QList<TransactionDetail*>> TxDetails;

    DbTestCase();

    QList<QString> connectionPoolNames();

    ConnectionPool *connectionPool(QString name);

    void createDatabases();

    QVariant addCompany(QString driver, const QString &name);
    QVariant addAccount(QString driver, const QString &name, const QString &type, const QVariant companyId = QVariant{});
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
