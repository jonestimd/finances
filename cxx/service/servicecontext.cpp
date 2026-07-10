#include "servicecontext.h"

ServiceContext::ServiceContext(ConnectionPool *pool, bool borrowedPool)
    : pool{pool}
    , borrowedPool{borrowedPool}
    , companyDao{pool->dbType()}
    , accountDao{pool->dbType()}
    , categoryDao{pool->dbType()}
    , transactionGroupDao{pool->dbType()}
    , payeeDao{pool->dbType()}
    , securityDao{pool->dbType()}
    , securityLotDao{pool->dbType()}
    , stockSplitDao{pool->dbType()}
    , transactionDao{pool->dbType()}
    , transactionDetailDao{pool->dbType()}
    , accountService{pool, accountDao, companyDao}
    , companyService{pool, companyDao}
    , payeeService{pool, payeeDao, transactionDao}
    , categoryService{pool, categoryDao, transactionDetailDao}
    , groupService{pool, transactionGroupDao}
    , securityService{pool, securityDao, stockSplitDao}
    , transationDetailService{pool, transactionDetailDao}
    , transationService{pool, transactionDao, transactionDetailDao}
{}

ServiceContext::ServiceContext(const ConnectionSettings &settings) : ServiceContext{new ConnectionPool(settings)} {}

ServiceContext::~ServiceContext() {
    if (!borrowedPool) delete pool;
}

const ConnectionSettings& ServiceContext::connectionSettings() const {
    return pool->settings;
}

void ServiceContext::createDatabase(const QString &user, const QString &password) {
    auto conn = Connection(pool);
    dbDialect::createSchema(conn.db, pool->settings.schema);
    createDatabaseTables(conn.db);
    if (!user.isEmpty() && !password.isEmpty()) {
        dbDialect::addUser(conn.db, pool->settings.schema, user, password);
    }
}

void ServiceContext::createDatabaseTables(const QSqlDatabase &db) {
    securityDao.createTable(db);
    companyDao.createTable(db);
    accountDao.createTable(db);
    payeeDao.createTable(db);
    categoryDao.createTable(db);
    transactionGroupDao.createTable(db);
    transactionDao.createTable(db);
    transactionDetailDao.createTable(db);
    stockSplitDao.createTable(db);
    securityLotDao.createTable(db);

    securityDao.createViews(db);
    securityDao.addCurrency(db);
}

void ServiceContext::shutdown() {
    if (!borrowedPool) pool->shutdown();
}
