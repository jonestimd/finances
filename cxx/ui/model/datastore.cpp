#include "datastore.h"
#include "../widget/dialog.h"
#include <QSqlError>
#include <QtConcurrent>

template<typename T, class Service>
class Holder
{
    DataStore *dataStore;
    QHash<qlonglong, T> byId;
    Service *service;

public:
    bool loaded;

    Holder(DataStore *dataStore, Service *service)
        : dataStore{dataStore}
        , loaded{false}, service{service} {};

    const QHash<qlonglong, T> values() const {
        return this->byId;
    }

    bool load(QWidget *source, bool reload, void (DataStore::*valuesLoaded)(QHash<qlonglong, T>)) {
        if (!reload && loaded) return true;
        doInBackground(source, [valuesLoaded, this]() {
            setValues(service->getAll());
            (dataStore->*valuesLoaded)(values());
        });
        return false;
    }

    void update(const QList<T> &updates, const QList<T> deletes = QList<T>{}) {
        for (auto updated : updates) {
            delete byId.take(updated->id.toLongLong());
            byId[updated->id.toLongLong()] = updated;
        }
        for (auto i : deletes) delete byId.take(i->id.toLongLong());
    }

    void setValues(QList<T> values) {
        for (auto value : values) {
            this->byId[value->id.toLongLong()] = value;
        }
        this->loaded = true;
    }
};

struct DataStorePrivate {
    Holder<const Account*, AccountService> accounts;
    Holder<const Company*, CompanyService> companies;
    Holder<const Payee*, PayeeService> payees;
    Holder<const Category*, CategoryService> categories;

    DataStorePrivate(DataStore *dataStore, ServiceContext *services)
        : accounts{dataStore, &services->accountService}
        , companies{dataStore, &services->companyService}
        , payees{dataStore, &services->payeeService}
        , categories{dataStore, &services->categoryService} {}
};

typedef std::function<void()> Runnable;
typedef std::function<void(bool)> OnComplete;

bool handleError(QWidget *source, Runnable task) {
    try {
        task();
        return true;
    } catch(const QString error) {
        dialog::showError(source, error);
    }
    return false;
}

void doInBackground(QWidget *source, Runnable task, OnComplete onComplete = nullptr) {
    QThreadPool::globalInstance()->start([=]() {
        auto result = handleError(source, task);
        if (onComplete) onComplete(result);
    });
}

DataStore::DataStore(ServiceContext *services)
    : p{new DataStorePrivate(this, services)}
    , services{services}
    , user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))}
{}

DataStore::~DataStore() {
    delete p;
}

bool DataStore::loadAccounts(QWidget *source, bool reload) {
    return p->accounts.load(source, reload, &DataStore::accountsLoaded);
}

const QHash<qlonglong, const Account *> DataStore::accounts() const {
    return p->accounts.values();
}

void DataStore::updateAccounts(QWidget *source, QList<Account *> updates, const QList<Account *> adds, const QList<const Account *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
            auto changes = BulkUpdate{updates, adds, deletes};
            QList<const Company*> companies;
            auto accounts = services->accountService.update(changes, user, &companies);
            p->accounts.update(accounts, deletes);
            p->companies.update(companies);
        }, [this](bool success) {
            emit accountsLoaded(p->accounts.values());
            emit companiesLoaded(p->companies.values());
        });
}

bool DataStore::loadCompanies(QWidget *source, bool reload) {
    return p->companies.load(source, reload, &DataStore::companiesLoaded);
}

const QHash<qlonglong, const Company*> DataStore::companies() const {
    return p->companies.values();
}

void DataStore::updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto companies = services->companyService.update(changes, user);
        p->companies.update(companies, deletes);
    }, [this](bool success) { emit companiesLoaded(p->companies.values()); });
}

void DataStore::addCompany(QWidget *source, const QString &name, std::function<void(const Company*)> callback) {
    doInBackground(source, [=, this] {
        auto company = services->companyService.add(name, user);
        p->companies.update(QList{company});
        if (callback) callback(company);
    }, [=, this](bool success) {
        emit companiesLoaded(p->companies.values());
        if (!success) callback(nullptr);
    });
}

bool DataStore::loadPayees(QWidget *source, bool reload) {
    return p->payees.load(source, reload, &DataStore::payeesLoaded);
}

const QHash<qlonglong, const Payee *> DataStore::payees() const {
    return p->payees.values();
}

void DataStore::updatePayees(QWidget *source, QList<Payee *> updates, const QList<Payee *> adds, const QList<const Payee *> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto payees = services->payeeService.update(changes, user);
        p->payees.update(payees, deletes);
    }, [this](bool success) { emit payeesLoaded(p->payees.values()); });
}

bool DataStore::loadCategories(QWidget *source, bool reload) {
    return p->categories.load(source, reload, &DataStore::categoriesLoaded);
}

const QHash<qlonglong, const Category *> DataStore::categories() const {
    return p->categories.values();
}

#include "datastore.moc"
