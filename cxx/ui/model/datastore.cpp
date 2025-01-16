#include "datastore.h"
#include <QSqlError>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , accountStore{new AccountStore(services)}
    , payeeStore{new PayeeStore(&services->payeeService)}
    , categoryStore{new CategoryStore(&services->categoryService)}
    , groupStore{new GroupStore{&services->groupService}}
{}

DataStore::~DataStore() {
    delete accountStore;
    delete payeeStore;
    delete categoryStore;
    delete groupStore;
}

const QString DataStore::connectionName() const {
    return services->connectionName();
}

const QString DataStore::user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))};
