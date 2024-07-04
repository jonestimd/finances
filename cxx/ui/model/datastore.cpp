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
            delete byId.take(updated->id.toString());
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
    Holder<const Account*> accounts;
    Holder<const Company*> companies;
};

typedef std::function<void()> Runnable;
typedef std::function<void(bool)> Callback;

bool handleError(QWidget *source, Runnable task) {
    try {
        task();
        return true;
    } catch(const QString error) {
        showError(source, error);
    } catch (const char *error) {
        showError(source, source->tr(error));
    }
    return false;
}

void doInBackground(QWidget *source, Runnable task, Callback onComplete = nullptr) {
    QThreadPool::globalInstance()->start([=]() {
        bool success = handleError(source, task);
        if (onComplete) onComplete(success);
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

QList<const Account *> DataStore::accounts() const {
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

QList<const Company*> DataStore::companies() const {
    return p->companies.values();
}

void DataStore::updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds) {
    doInBackground(source, [this, updates, adds] {
        auto companies = services->companyService.update(updates, adds, user);
        p->companies.update(companies);
    }, [=, this](bool success) {
            if (!success) {
                for (auto company : updates) delete company;
                for (auto company : adds) delete company;
            }
            emit companiesLoaded(p->companies.values());
        }
    );
}
