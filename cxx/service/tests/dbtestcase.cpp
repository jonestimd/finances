#include "dbtestcase.h"
#include "service/database/dbdialect.h"
#include "service/database/transactiongroupdao.h"
#include "service/database/transactiondetaildao.h"
#include "qtcommon.h"
#include "service/database/daocontext.h"
#include <QFile>
#include <QSqlError>
#include <QSqlResult>
#include <QtTest/qtestcase.h>

static const QList<const char*> dropTableQueries = {
    "drop view if exists account_security",
    "drop function if exists adjust_shares",
    "drop table if exists security_lot",
    "drop table if exists stock_split",
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

static const QHash<const QString, const char*> driverEnvSettings{
    {PG_DRIVER, "TEST_PSQL_CONNECTION"},
    {MYSQL_DRIVER, "TEST_MYSQL_CONNECTION"},
};

static const QHash<const QString, int> driverDefaultPort{
    {PG_DRIVER, 5432},
    {MYSQL_DRIVER, 3306},
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
    , stockSplitDao{dbType}
    , securityLotDao{dbType}
{};

namespace factory {
    Transaction* transaction(domain_id accountId, optional_id payeeId, optional_id securityId, const QDate &date) {
        Transaction* tx = new Transaction{accountId};
        tx->payeeId = payeeId;
        tx->securityId = securityId;
        tx->date = date;
        return tx;
    }

    PendingTransaction* pendingTransaction(domain_id accountId, QList<const char*> amounts, optional_id payeeId, optional_id securityId, const QDate &date) {
        PendingTransaction *tx = new PendingTransaction;
        tx->accountId = accountId;
        tx->payeeId = payeeId;
        tx->securityId = securityId;
        tx->date = date;
        for (auto amount : amounts) tx->details.append(detail(amount));
        return tx;
    }

    TransactionDetail* detail(const char *amount, const optional_id& categoryId, const optional_id& groupId) {
        TransactionDetail* detail = new TransactionDetail;
        detail->amount = QDecNumber(amount);
        detail->categoryId = categoryId;
        detail->groupId = groupId;
        return detail;
    }
}

#define DAOS(driver) (driver == PG_DRIVER ? pgDaos : driver == MYSQL_DRIVER ? mysqlDaos : sqliteDaos)

DbTestCase::DbTestCase()
    : pgDaos{PG_DRIVER}
    , mysqlDaos{MYSQL_DRIVER}
    , sqliteDaos{SQLITE_DRIVER}
{
    qtcommon::registerConverters();

    auto sqliteFile = qEnvironmentVariable("TEST_SQLITE_FILE", "file::memory:?cache=shared");
    addConnection(SQLITE_DRIVER, {SQLITE_DRIVER, "", 0, sqliteFile, "", ""});
    auto pgSettings = envSettings(PG_DRIVER);
    if (pgSettings.isComplete(false)) {
        addConnection(PG_DRIVER, pgSettings);
    }
    auto mysqlSettings = envSettings(MYSQL_DRIVER);
    if (mysqlSettings.isComplete(false)) {
        addConnection(MYSQL_DRIVER, mysqlSettings);
    }
}

QList<QString> DbTestCase::connectionPoolNames() {
    return connectionPools.keys();
}

ConnectionPool *DbTestCase::connectionPool(const QString &driver) {
    return connectionPools.value(driver);
}

const ConnectionSettings DbTestCase::settings(const QString &driver) const {
    return connectionPools.value(driver)->settings;
}

ConnectionSettings DbTestCase::envSettings(const char* driverName) {
    auto props = qgetenv(driverEnvSettings.value(driverName)).split('|');
    if (props.length() >= 5) {
        auto host = props[0];
        auto port = props[1].toInt();
        auto schema = props[2];
        auto user = props[3];
        auto password = props[4];
        return ConnectionSettings{driverName, host, port, schema, user, password};
    }
    return ConnectionSettings{};
}

int DbTestCase::port(const char *driverName) {
    auto settings = envSettings(driverName);
    return settings.isComplete(false) ? settings.port : driverDefaultPort.value(driverName);
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

TransactionGroupDao &DbTestCase::groupDao(const QString &driver) {
    return DAOS(driver).transactionGroupDao;
}

PayeeDao &DbTestCase::payeeDao(const QString &driver) {
    return DAOS(driver).payeeDao;
}

SecurityDao &DbTestCase::securityDao(const QString &driver) {
    return DAOS(driver).securityDao;
}

StockSplitDao &DbTestCase::stockSplitDao(const QString &driver) {
    return DAOS(driver).stockSplitDao;
}

TransactionDao &DbTestCase::transactionDao(const QString &driver) {
    return DAOS(driver).transactionDao;
}

TransactionDetailDao &DbTestCase::detailDao(const QString &driver) {
    return DAOS(driver).detailDao;
}

#define not_starts_with(str, prefix) std::strncmp(str, prefix, sizeof(prefix)-1)
#define CLASS_NAME "DbTestCase"

void DbTestCase::createDatabases() {
    for (auto [driver, pool] : connectionPools.asKeyValueRange()) {
        auto conn = Connection(pool);
        for (auto const dropQuery : dropTableQueries) {
            if (driver != SQLITE_DRIVER || not_starts_with(dropQuery, "drop function")) {
                sql::exec(conn.db, dropQuery, CLASS_NAME, "dropObject");
            }
        }
        DaoContext{pool->dbType()}.createDatabaseTables(conn.db);
    }
}

domain_id DbTestCase::addCompany(const QString &driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Company company{};
    company.name = name;
    companyDao(driver).add(conn.db, QList{&company}, TEST_USER);
    return company.id.value();
}

template<typename Entity, typename Dao, Dao &(DbTestCase::*dao)(const QString &)>
void save(DbTestCase *test, const QString &driver, Entity *entity, QList<const Entity*> list) {
    auto conn = Connection(test->connectionPool(driver));
    ((*test).*dao)(driver).add(conn.db, QList{entity}, TEST_USER);
    list.append(entity);
}

Account *DbTestCase::addAccount(const QString &driver, const QString &name, const QString &type, const optional_id companyId) {
    Account *account = new Account;
    account->name = name;
    account->type = AccountType::values.value(type);
    account->companyId = companyId;
    save<Account, AccountDao, &DbTestCase::accountDao>(this, driver, account, accounts);
    return account;
}

template<typename Entity, typename Dao, Dao &(DbTestCase::*dao)(const QString &)>
const Entity* load(DbTestCase *test, const QString &driver, domain_id id, QList<const Entity*> list) {
    auto conn = Connection(test->connectionPool(driver));
    auto rows = ((*test).*dao)(driver).get(conn.db, QList<domain_id>{id});
    list.append(rows.values());
    return rows.value(id);
}

domain_id DbTestCase::addPayee(const QString &driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Payee payee{name};
    payeeDao(driver).add(conn.db, QList{&payee}, TEST_USER);
    return payee.id.value();
}

Security* DbTestCase::addSecurity(const QString &driver, const QString &name, const char *type) {
    Security* security = new Security();
    security->name = name;
    security->securityType = SecurityType::values.value(QString{type});
    save<Security, SecurityDao, &DbTestCase::securityDao>(this, driver, security, securities);
    return security;
}

domain_id DbTestCase::addCategory(const QString &driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    Category category;
    category.name = name;
    categoryDao(driver).add(conn.db, QList{&category}, TEST_USER);
    return category.id.value();
}

domain_id DbTestCase::addGroup(const QString &driver, const QString &name) {
    auto conn = Connection(connectionPool(driver));
    TransactionGroup group;
    group.name = name;
    groupDao(driver).add(conn.db, QList{&group}, TEST_USER);
    return group.id.value();
}

const Account *DbTestCase::loadAccount(const QString &driver, domain_id id) {
    return load<Account, AccountDao, &DbTestCase::accountDao>(this, driver, id, accounts);
}

const Security *DbTestCase::loadSecurity(const QString &driver, domain_id id) {
    return load<Security, SecurityDao, &DbTestCase::securityDao>(this, driver, id, securities);
}

#define freeObjects(list) \
    qDeleteAll(list); \
    list.clear();

void DbTestCase::cleanup() {
    freeObjects(accounts)
    freeObjects(securities)
    freeObjects(transactions)
    freeObjects(details)
}

void DbTestCase::resetDatabase(const QString& driver) {
    Connection conn(connectionPool(driver));
    QSqlQuery query{conn.db};
    query.exec("delete from tx_detail");
    query.exec("delete from tx");
    query.exec("delete from stock_split");
}

QList<DbTestCase::TxDetails> DbTestCase::saveTransfer(const QString& driver, domain_id accountId, domain_id altAccountId, QList<const char *> amounts) {
    const char *transferAmount = amounts.at(0);
    QString relatedAmount = transferAmount[0] == '-' ? QString(transferAmount[1]) : QString(transferAmount).prepend('-');
    auto tx = saveTransaction(driver, factory::transaction(accountId), amounts);
    auto relatedTx = saveTransaction(driver, factory::transaction(altAccountId), QList{relatedAmount.toLocal8Bit().constData()});
    auto detail = GET_DETAILS(tx).at(0);
    detail->transferAccountId = altAccountId;
    auto relatedDetail = GET_DETAILS(relatedTx).at(0);
    relatedDetail->transferAccountId = accountId;
    detail->relatedDetailId = relatedDetail->id.value();
    relatedDetail->relatedDetailId = detail->id.value();

    auto &detailDao = this->detailDao(driver);
    Connection conn(connectionPool(driver));
    detailDao.setRelatedDetailIds(conn.db, {{relatedDetail, detail->id.value()}});
    detailDao.setRelatedDetailIds(conn.db, {{detail, relatedDetail->id.value()}});
    return QList{tx, relatedTx};
}

DbTestCase::TxDetails DbTestCase::saveTransaction(const Transaction* unsaved, const QList<const char*> &detailAmounts, const QList<const char*> &detailShares) {
    QFETCH_GLOBAL(QString, driver);
    return saveTransaction(driver, unsaved, detailAmounts, detailShares);
}

DbTestCase::TxDetails DbTestCase::saveTransaction(const QString &driver, const Transaction *unsaved, const QList<const char *> &detailAmounts, const QList<const char *> &detailShares) {
    Transaction *tx = new Transaction(*unsaved);
    QList<TransactionDetail*> details{};
    for (auto &amount : detailAmounts) {
        auto i = details.size();
        TransactionDetail *detail = factory::detail(amount);
        if (tx->securityId.has_value() && detailShares.size() > i) detail->assetQuantity = QDecNumber(detailShares.at(i));
        details.append(detail);
    }
    saveTransaction(driver, tx, details);
    return TxDetails{tx, details};
}

void DbTestCase::saveTransaction(const QString &driver, Transaction *tx, const QList<TransactionDetail *> details) {
    Connection conn(connectionPool(driver));
    transactionDao(driver).add(conn.db, QList<Transaction*>{tx}, TEST_USER);
    for (auto detail : details) detail->transactionId = tx->id.value();
    detailDao(driver).add(conn.db, details, TEST_USER);
    tx->detailIds = getEntityIds(details);
    this->transactions.append(tx);
    for (auto detail : std::as_const(details)) this->details.append(detail);
}

void DbTestCase::addConnection(const QString &name, const ConnectionSettings &settings) {
    auto connectionPool = new ConnectionPool(settings);
    connectionPools.insert(name, connectionPool);
}
