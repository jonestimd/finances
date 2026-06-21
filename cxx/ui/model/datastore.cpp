#include "datastore.h"
#include <QSqlError>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , accountStore{new AccountStore(services)}
    , payeeStore{new PayeeStore(&services->payeeService, this)}
    , categoryStore{new CategoryStore(&services->categoryService, this)}
    , groupStore{new GroupStore{&services->groupService}}
    , securityStore{new SecurityStore{&services->securityService}}
    , transactionStore{new TransactionStore{services, categoryStore}}
{
    connect(transactionStore, SIGNAL(transactionsUpdated(const QList<TransactionChange>)),
            payeeStore, SLOT(transactionsUpdated(const QList<TransactionChange>)), Qt::DirectConnection);
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
