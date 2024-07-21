#include "datastore.h"
#include "../widget/dialog.h"
#include <QSqlError>
#include <QtConcurrent>

template<typename T>
class Holder {
    QHash<qlonglong, T> byId;
public:
    bool loaded;

    Holder() : loaded{false} {};

    const QHash<qlonglong, T> values() const {
        return this->byId;
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
    Holder<const Account*> accounts;
    Holder<const Company*> companies;
};

typedef std::function<void()> Runnable;
typedef std::function<void(bool)> OnComplete;

bool handleError(QWidget *source, Runnable task) {
    try {
        task();
        return true;
    } catch(const QString error) {
        dialog::showError(source, error);
    } catch (const char *error) {
        dialog::showError(source, source->tr(error));
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
    : p{new DataStorePrivate}
    , services{services}
    , user{std::optional(std::getenv("USER")).value_or(std::getenv("USERNAME"))}
{}

DataStore::~DataStore() {
    delete p;
}

bool DataStore::loadAccounts(QWidget *source, bool reload) {
    if (!reload && p->accounts.loaded) return true;
    doInBackground(source, [this]() {
        p->accounts.setValues(services->accountService.getAll());
        emit accountsLoaded(p->accounts.values());
    });
    return false;
}

const QHash<qlonglong, const Account *> DataStore::accounts() const {
    return p->accounts.values();
}

bool DataStore::loadCompanies(QWidget *source, bool reload) {
    if (!reload && p->companies.loaded) return true;
    doInBackground(source, [this]() {
        p->companies.setValues(services->companyService.getAll());
        emit companiesLoaded(p->companies.values());
    });
    return false;
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
