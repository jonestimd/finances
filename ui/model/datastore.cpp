#include "datastore.h"
#include <QtConcurrent>

template<typename T>
struct Holder {
    bool loaded;
    QList<T> values;

    Holder() : loaded{false} {};

    void setValues(QList<T> values) {
        this->values = values;
        this->loaded = true;
    }
};

struct DataStorePrivate {
    Holder<Account*> accounts;
    Holder<Company*> companies;
};

DataStore::DataStore(ServiceContext *services) : p{new DataStorePrivate}, services{services} {}

DataStore::~DataStore() {
    delete p;
}

bool DataStore::loadAccounts() {
    if (p->accounts.loaded) return true;
    QThreadPool::globalInstance()->start([this]() {
        p->accounts.setValues(services->accountService.getAll());
        emit accountsLoaded(p->accounts.values);
    });
    return false;
}

QList<Account *> DataStore::accounts() const {
    return p->accounts.values;
}

bool DataStore::loadCompanies() {
    if (p->companies.loaded) return true;
    QThreadPool::globalInstance()->start([this]() {
        p->companies.setValues(services->companyService.getAll());
        emit companiesLoaded(p->companies.values);
    });
    return false;
}

QList<Company*> DataStore::companies() const {
    return p->companies.values;
}
