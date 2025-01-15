#include "datastore.h"
#include <QSqlError>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , accountStore{new AccountStore(services)}
    , payeeStore{new PayeeStore(&services->payeeService)}
    , categoryStore{new CategoryStore(&services->categoryService)}
{}

DataStore::~DataStore() {
    delete accountStore;
    delete payeeStore;
    delete categoryStore;
}

const QString DataStore::connectionName() const {
    return services->connectionName();
}

const QString DataStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
