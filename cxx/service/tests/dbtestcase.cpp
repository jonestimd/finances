#include "dbtestcase.h"
#include "service/database/dbdialect.h"
#include "service/database/transactiongroupdao.h"
#include "service/database/transactiondetaildao.h"
#include <QFile>
#include <QtTest/qtestcase.h>

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

Daos::Daos(const QString &dbType)
    : companyDao{dbType}
    , accountDao{dbType}
    , categoryDao{dbType}
    , transactionGroupDao{dbType}
    , payeeDao{dbType}
    , securityDao{dbType}
    , transactionDao{dbType}
    , detailDao{dbType}
{};

#define DAOS(driver) (driver == PG_DRIVER ? pgDaos : driver == MYSQL_DRIVER ? mysqlDaos : sqliteDaos)

DbTestCase::DbTestCase()
    : pgDaos{PG_DRIVER}
    , mysqlDaos{MYSQL_DRIVER}
    , sqliteDaos{SQLITE_DRIVER}
{
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
            addConnection(PG_DRIVER, {PG_DRIVER, host, port, schema, user, password});
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

CompanyDao &DbTestCase::companyDao(const QString &driver) {
    return DAOS(driver).companyDao;
}

AccountDao &DbTestCase::accountDao(const QString &driver) {
    return DAOS(driver).accountDao;
}

CategoryDao &DbTestCase::categoryDao(const QString &driver) {
    return DAOS(driver).categoryDao;
}

PayeeDao &DbTestCase::payeeDao(const QString &driver) {
    return DAOS(driver).payeeDao;
}

SecurityDao &DbTestCase::securityDao(const QString &driver) {
    return DAOS(driver).securityDao;
}

TransactionDao &DbTestCase::transactionDao(const QString &driver) {
    return DAOS(driver).transactionDao;
}

TransactionDetailDao &DbTestCase::detailDao(const QString &driver) {
    return DAOS(driver).detailDao;
}

void DbTestCase::createDatabases() {
    for (auto [driver, pool] : connectionPools.asKeyValueRange()) {
        auto conn = Connection(pool);
        for (auto &dropQuery : dropTableQueries) {
            QSqlQuery{conn.db}.exec(dropQuery);
        }
        auto daos = DAOS(driver);
        daos.securityDao.createTable(conn.db);
        daos.companyDao.createTable(conn.db);
        daos.accountDao.createTable(conn.db);
        daos.payeeDao.createTable(conn.db);
        daos.categoryDao.createTable(conn.db);
        daos.transactionGroupDao.createTable(conn.db);
        daos.transactionDao.createTable(conn.db);
        daos.detailDao.createTable(conn.db);

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
    companyDao(driver).add(conn.db, QList{&company}, TEST_USER);
    return company.id;
}

template<typename Entity, typename Dao, Dao &(DbTestCase::*dao)(const QString &)>
void save(DbTestCase *test, const QString &driver, Entity *entity, QList<const Entity*> list) {
    auto conn = Connection(test->connectionPool(driver));
    ((*test).*dao)(driver).add(conn.db, QList{entity}, TEST_USER);
    list.append(entity);
}

Account *DbTestCase::addAccount(QString driver, const QString &name, const QString &type, const QVariant companyId) {
    Account *account = new Account;
    account->name = name;
    account->type = type;
    account->companyId = companyId;
    save<Account, AccountDao, &DbTestCase::accountDao>(this, driver, account, accounts);
    return account;
}

template<typename Entity, typename Dao, Dao &(DbTestCase::*dao)(const QString &)>
const Entity* load(DbTestCase *test, const QString &driver, const QVariant &id, QList<const Entity*> list) {
    auto conn = Connection(test->connectionPool(driver));
    auto rows = ((*test).*dao)(driver).get(conn.db, {id});
    list.append(rows.values());
    return rows.value(id.toLongLong());
}

const Account *DbTestCase::loadAccount(QString driver, const QVariant &id) {
    return load<Account, AccountDao, &DbTestCase::accountDao>(this, driver, id, accounts);
}

QVariant DbTestCase::addPayee(QString driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Payee payee{};
    payee.name = name;
    payeeDao(driver).add(conn.db, QList{&payee}, TEST_USER);
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

#define freeObjects(list) \
    for (auto item : std::as_const(list)) delete item; \
    list.clear();

void DbTestCase::cleanup() {
    freeObjects(accounts)
    freeObjects(transactions)
    freeObjects(details)
}

DbTestCase::TxDetails DbTestCase::saveTransaction(QVariant accountId, QList<const char *> detailAmounts) {
    QFETCH_GLOBAL(QString, driver);
    Transaction *tx = new Transaction(unsavedTransaction(accountId));
    Connection conn(connectionPool(driver));
    transactionDao(driver).add(conn.db, QList<Transaction*>{tx}, TEST_USER);
    QList<TransactionDetail*> details{};
    for (auto &amount : detailAmounts) {
        TransactionDetail *detail = new TransactionDetail(unsavedDetail(amount));
        detail->transactionId = tx->id;
        details.append(detail);
    }
    detailDao(driver).add(conn.db, details, TEST_USER);
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
