#include "datastore.h"
#include "ui/finances.h"
#include <QSqlError>
#include <QThreadPool>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , accountStore{new AccountStore(services, &messageStore)}
    , payeeStore{new PayeeStore(&services->payeeService, this)}
    , categoryStore{new CategoryStore(&services->categoryService, this)}
    , groupStore{new GroupStore{&services->groupService, &messageStore}}
    , securityStore{new SecurityStore{&services->securityService, &messageStore, &services->stockSplitService}}
    , transactionStore{new TransactionStore{services, this}}
{
    connect(transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
            accountStore, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
            payeeStore, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
            categoryStore, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
            groupStore, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)), Qt::DirectConnection);
}

DataStore::DataStore(const ConnectionSettings &settings) : DataStore{new ServiceContext(settings)} {}

DataStore::~DataStore() {
    delete accountStore;
    delete payeeStore;
    delete categoryStore;
    delete groupStore;
    delete securityStore;
    delete transactionStore;
    delete services;
}

const ConnectionSettings& DataStore::connectionSettings() const {
    return services->connectionSettings();
}

QString DataStore::connectionName() const {
    return connectionSettings().displayName();
}

void DataStore::loadAccounts(OpenHandler handler) {
    QThreadPool::globalInstance()->start([=, this]() {
        QString message;
        try {
            accountStore->setValues(services->accountService.getAll(), AccountStore::FriendKey{});
        } catch(const QString error) {
            message = error;
        }
        QMetaObject::invokeMethod(this, [=, this]() {
            handler(this, message);
        }, Qt::QueuedConnection);
    });
}

QString DataStore::toString(const DetailSearchCriteria& criteria) const {
    QStringList result;
    if (!criteria.text.isEmpty()) result.append(QString{"contains \"%1\""}.arg(criteria.text));
    if (criteria.payeeId.has_value()) result.append(QString{"payee = %1"}.arg(payeeStore->value(criteria.payeeId.value())->name));
    if (criteria.securityId.has_value()) result.append(QString{"security = %1"}.arg(securityStore->value(criteria.securityId.value())->name));
    if (criteria.categoryId.has_value()) result.append(QString{"category = %1"}.arg(categoryStore->displayName(criteria.categoryId.value())));
    return result.join(" and ");
}

void DataStore::shutdown() {
    services->shutdown();
    finances::App::addRecentName(services->connectionSettings().configName());
}

const QString DataStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
