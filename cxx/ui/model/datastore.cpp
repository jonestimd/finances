#include "datastore.h"
#include "ui/widget/settings.h"
#include <QSqlError>
#include <QThreadPool>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , accountStore{new AccountStore(services, &messageStore)}
    , payeeStore{new PayeeStore(&services->payeeService, this)}
    , categoryStore{new CategoryStore(&services->categoryService, this)}
    , groupStore{new GroupStore{&services->groupService, &messageStore}}
    , securityStore{new SecurityStore{&services->securityService, &messageStore}}
    , transactionStore{new TransactionStore{services, this}}
{
    connect(transactionStore, SIGNAL(transactionsUpdated(const QList<TransactionChange>)),
            accountStore, SLOT(transactionsUpdated(const QList<TransactionChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(transactionsUpdated(const QList<TransactionChange>)),
            payeeStore, SLOT(transactionsUpdated(const QList<TransactionChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(transactionsUpdated(const QList<TransactionChange>)),
            securityStore, SLOT(transactionsUpdated(const QList<TransactionChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(detailsUpdated(const QList<DetailChange>)),
            categoryStore, SLOT(detailsUpdated(const QList<DetailChange>)), Qt::DirectConnection);
    connect(transactionStore, SIGNAL(detailsUpdated(const QList<DetailChange>)),
            groupStore, SLOT(detailsUpdated(const QList<DetailChange>)), Qt::DirectConnection);
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

void DataStore::loadAccounts(OpenHandler* handler) {
    QThreadPool::globalInstance()->start([=, this]() {
        QString message;
        try {
            accountStore->setValues(services->accountService.getAll(), AccountStore::FriendKey{});
        } catch(const QString error) {
            message = error;
        }
        QMetaObject::invokeMethod(this, [=, this]() {
            handler->handleOpenResult(this, message);
        }, Qt::QueuedConnection);
    });
}

void DataStore::shutdown() {
    services->shutdown();
    settings::addRecentName(services->connectionSettings().configName());
}

const QString DataStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
