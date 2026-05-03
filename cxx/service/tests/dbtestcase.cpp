#include "dbtestcase.h"
#include "service/database/accountdao.h"
#include "service/database/companydao.h"
#include "service/database/payeedao.h"
#include "service/database/securitydao.h"
#include "service/database/categorydao.h"
#include "service/database/transactiongroupdao.h"
#include "service/database/transactiondao.h"
#include "service/database/transactiondetaildao.h"
#include <QFile>
#include <QtTest/qtestcase.h>

#define SQLITE_DRIVER "QSQLITE"
#define PSQL_DRIVER "QPSQL"
#define MYSQL_DRIVER "QMYSQL"

Q_LOGGING_CATEGORY(sqlLogger, "sql")

static const auto addCurencyQuery = R"(
insert into asset (name, type, scale, symbol, change_user, version)
values ('USD', 'Currency', 2, '$', :user, 0))";

static const QList<const char *> dropTableQueries = {
    "drop table if exists tx_detail",
    "drop table if exists tx",
    "drop table if exists tx_group",
    "drop table if exists tx_category",
    "drop table if exists security",
    "drop table if exists payee",
    "drop table if exists account",
    "drop table if exists company",
    "drop table if exists asset",
};

DbTestCase::DbTestCase() {
    QMetaType::registerConverter<QDecNumber, QString>(
        [](const QDecNumber &value) -> QString { return QString(value.toString()); }
    );

    addConnection(SQLITE_DRIVER, {SQLITE_DRIVER, "", 0, "test.db", "", ""});
    auto psqlConnection = qgetenv("TEST_PSQL_CONNECTION");
    if (!psqlConnection.isEmpty()) {
        auto props = psqlConnection.split('|');
        if (props.length() >= 5) {
            auto host = props[0];
            auto port = props[1].toInt();
            auto schema = props[2];
            auto user = props[3];
            auto password = props[4];
            addConnection(PSQL_DRIVER, {PSQL_DRIVER, host, port, schema, user, password});
        }
    }
    auto mysqlConnection = qgetenv("TEST_MYSQL_CONNECTION");
    if (!mysqlConnection.isEmpty()) {
        auto props = mysqlConnection.split('|');
        if (props.length() >= 5) {
            auto host = props[0];
            auto port = props[1].toInt();
            auto schema = props[2];
            auto user = props[3];
            auto password = props[4];
            addConnection(MYSQL_DRIVER, {MYSQL_DRIVER, host, port, schema, user, password});
        }
    }
}

QList<QString> DbTestCase::connectionPoolNames() {
    return connectionPools.keys();
}

ConnectionPool *DbTestCase::connectionPool(QString name) {
    return connectionPools.value(name);
}

void DbTestCase::createDatabases() {
    for (auto [driver, pool] : connectionPools.asKeyValueRange()) {
        auto conn = Connection(pool);
        for (auto &dropQuery : dropTableQueries) {
            QSqlQuery{conn.db}.exec(dropQuery);
        }
        securityDao.createTable(conn.db);
        companyDao.createTable(conn.db);
        accountDao.createTable(conn.db);
        payeeDao.createTable(conn.db);
        categoryDao.createTable(conn.db);
        transactionGroupDao.createTable(conn.db);
        transactionDao.createTable(conn.db);
        transactionDetailDao.createTable(conn.db);

        QSqlQuery query(conn.db);
        query.prepare(addCurencyQuery);
        query.bindValue(":user", TEST_USER);
        sql::exec(query, "DbTestCase", "addCurency");
    }
}

QVariant DbTestCase::addCompany(QString driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Company company{};
    company.name = name;
    companyDao.add(conn.db, QList{&company}, TEST_USER);
    return company.id;
}

QVariant DbTestCase::addAccount(QString driver, const QString &name, const QString &type, const QVariant companyId) {
    auto conn = Connection(connectionPool(driver));
    Account account{};
    account.name = name;
    account.type = type;
    account.companyId = companyId;
    accountDao.add(conn.db, QList{&account}, TEST_USER);
    return account.id;
}

QVariant DbTestCase::addPayee(QString driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Payee payee{};
    payee.name = name;
    payeeDao.add(conn.db, QList{&payee}, TEST_USER);
    return payee.id;
}

Transaction DbTestCase::unsavedTransaction(QDate date) {
    QFETCH_GLOBAL(QVariant, accountId);
    return unsavedTransaction(accountId, date);
}

Transaction DbTestCase::unsavedTransaction(QVariant accountId, QDate date) {
    QFETCH_GLOBAL(QVariant, payeeId);
    Transaction tx{accountId};
    tx.payeeId = payeeId;
    tx.date = date;
    return tx;
}

TransactionDetail DbTestCase::unsavedDetail(const char *amount) {
    TransactionDetail detail{};
    detail.amount = DECIMAL_VARIANT(amount);
    return detail;
}

void DbTestCase::cleanup() {
    for (auto tx : std::as_const(transactions)) delete tx;
    transactions.clear();
    for (auto detail : std::as_const(details)) delete detail;
    details.clear();
}

DbTestCase::TxDetails DbTestCase::saveTransaction(QVariant accountId, QList<const char *> detailAmounts) {
    QFETCH_GLOBAL(QString, driver);
    Transaction *tx = new Transaction(unsavedTransaction(accountId));
    Connection conn(connectionPool(driver));
    transactionDao.add(conn.db, QList<Transaction*>{tx}, TEST_USER);
    QList<TransactionDetail*> details{};
    for (auto &amount : detailAmounts) {
        TransactionDetail *detail = new TransactionDetail(unsavedDetail(amount));
        detail->transactionId = tx->id;
        details.append(detail);
    }
    transactionDetailDao.add(conn.db, details, TEST_USER);
    tx->detailIds = getEntityIds(details);
    this->transactions.append(tx);
    for (auto detail : std::as_const(details)) this->details.append(detail);
    return TxDetails{tx, details};
}

DbTestCase::TxDetails DbTestCase::saveTransaction(QList<const char *> detailAmounts) {
    QFETCH_GLOBAL(QVariant, accountId);
    return saveTransaction(accountId, detailAmounts);
}

void DbTestCase::addConnection(QString name, const ConnectionSettings &settings) {
    auto connectionPool = new ConnectionPool(settings);
    connectionPools.insert(name, connectionPool);
}
