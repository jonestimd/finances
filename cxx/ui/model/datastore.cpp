#include "datastore.h"
#include <QSqlError>

DataStore::DataStore(ServiceContext *services)
    : services{services}
    , user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))}
    , accountStore{new EntityStore<const Account*>}
    , companyStore{new EntityStore<const Company*>}
    , payeeStore{new EntityStore<const Payee*>}
    , categoryStore{new CategoryStore}
{}

DataStore::~DataStore() {
    delete accountStore;
    delete companyStore;
    delete payeeStore;
    delete categoryStore;
}

bool DataStore::loadAccounts(QWidget *source, bool reload) {
    return load(source, reload, accountStore, &services->accountService, &DataStore::accountsLoaded);
}

const EntityStore<const Account *> *DataStore::accounts() const {
    return accountStore;
}

void DataStore::updateAccounts(QWidget *source, QList<Account *> updates, const QList<Account *> adds, const QList<const Account *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        QList<const Company*> companies;
        auto accounts = services->accountService.update(changes, user, &companies);
        accountStore->update(accounts, deletes);
        companyStore->update(companies);
        emit accountsLoaded(accountStore->ids());
        emit companiesLoaded(companyStore->ids());
    });
}

bool DataStore::loadCompanies(QWidget *source, bool reload) {
    return load(source, reload, companyStore, &services->companyService, &DataStore::companiesLoaded);
}

const EntityStore<const Company*> *DataStore::companies() const {
    return companyStore;
}

void DataStore::updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto companies = services->companyService.update(changes, user);
        companyStore->update(companies, deletes);
        emit companiesLoaded(companyStore->ids());
    });
}

void DataStore::addCompany(QWidget *source, const QString &name, const char *callback) {
    doInBackground(source, [=, this] {
        auto company = services->companyService.add(name, user);
        companyStore->update(QList{company});
        QMetaObject::invokeMethod(source, callback, company);
        emit companiesLoaded(companyStore->ids());
    }, [=]() {
        QMetaObject::invokeMethod(source, callback, nullptr);
    });
}

bool DataStore::loadPayees(QWidget *source, bool reload) {
    return load(source, reload, payeeStore, &services->payeeService, &DataStore::payeesLoaded);
}

const EntityStore<const Payee *> *DataStore::payees() const {
    return payeeStore;
}

void DataStore::updatePayees(QWidget *source, QList<Payee *> updates, const QList<Payee *> adds, const QList<const Payee *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto payees = services->payeeService.update(changes, user);
        payeeStore->update(payees, deletes);
        emit payeesLoaded(payeeStore->ids());
    });
}

bool DataStore::loadCategories(QWidget *source, bool reload) {
    return load(source, reload, categoryStore, &services->categoryService, &DataStore::categoriesLoaded);
}

const CategoryStore *DataStore::categories() const {
    return categoryStore;
}

void DataStore::updateCategories(QWidget *source, QList<Category *> updates, const QList<Category *> adds, const QList<const Category *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto categories = services->categoryService.update(changes, user);
        categoryStore->update(categories, deletes);
        emit categoriesLoaded(categoryStore->ids());
    });
}

void DataStore::setParent(QWidget *source, const Category *category, const QVariant parentId) {
    doInBackground(source, [this, category, parentId] {
        auto categories = services->categoryService.setParent(category, parentId, user);
        categoryStore->update(categories);
        emit categoriesLoaded(categoryStore->ids());
    });
}
