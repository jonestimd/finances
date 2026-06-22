#include "datastore.h"
#include <QSqlError>

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

DataStore::~DataStore() {
    delete accountStore;
    delete payeeStore;
    delete categoryStore;
    delete groupStore;
    delete securityStore;
    delete transactionStore;
}

const QString DataStore::connectionName() const {
    return services->connectionName();
}

const QString DataStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
