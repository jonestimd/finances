#include "datastore.h"
#include "../widget/dialog.h"
#include <QSqlError>
#include <QtConcurrent>

template<typename T>
class Holder {
    QHash<QString, T> byId;
public:
    bool loaded;

    Holder() : loaded{false} {};

    QList<T> values() const {
        return this->byId.values();
    }

    void update(QList<T> &updates) {
        for (auto updated : updates) {
            byId[updated->id.toString()] = updated;
        }
    }

    void setValues(QList<T> values) {
        for (auto value : values) {
            this->byId[value->id.toString()] = value;
        }
        this->loaded = true;
    }
};

struct DataStorePrivate {
    Holder<Account*> accounts;
    Holder<Company*> companies;
};

typedef std::function<void()> Runnable;

void handleError(QWidget *source, Runnable callback) {
    try {
        callback();
    } catch(const QString error) {
        showError(source, error);
    } catch (const char *error) {
        showError(source, source->tr(error));
    }
}

void doInBackground(QWidget *source, Runnable callback, Runnable onComplete = nullptr) {
    QThreadPool::globalInstance()->start([=]() {
        handleError(source, callback);
        if (onComplete) onComplete();
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

bool DataStore::loadAccounts(QWidget *source) {
    if (p->accounts.loaded) return true;
    doInBackground(source, [this]() {
        p->accounts.setValues(services->accountService.getAll());
        emit accountsLoaded(p->accounts.values());
    });
    return false;
}

QList<Account *> DataStore::accounts() const {
    return p->accounts.values();
}

bool DataStore::loadCompanies(QWidget *source) {
    if (p->companies.loaded) return true;
    doInBackground(source, [this]() {
        p->companies.setValues(services->companyService.getAll());
        emit companiesLoaded(p->companies.values());
    });
    return false;
}

QList<Company*> DataStore::companies() const {
    return p->companies.values();
}

void DataStore::updateCompanies(QWidget *source, QList<Company*> updates) {
    doInBackground(source, [this, updates] {
        auto companies = services->companyService.update(updates, user);
        p->companies.update(companies);
    }, [this] { emit companiesLoaded(p->companies.values()); });
}
