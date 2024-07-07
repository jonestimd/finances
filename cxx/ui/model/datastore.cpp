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

    void update(QList<T> &updates, QList<T> deletes) {
        for (auto updated : updates) {
            delete byId.take(updated->id.toString());
            byId[updated->id.toString()] = updated;
        }
        for (auto i : deletes) delete byId.take(i->id.toString());
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

void handleError(QWidget *source, Runnable task) {
    try {
        task();
    } catch(const QString error) {
        dialog::showError(source, error);
    } catch (const char *error) {
        dialog::showError(source, source->tr(error));
    }
}

void doInBackground(QWidget *source, Runnable task, Runnable onComplete = nullptr) {
    QThreadPool::globalInstance()->start([=]() {
        handleError(source, task);
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

QList<const Account *> DataStore::accounts() const {
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

QList<const Company*> DataStore::companies() const {
    return p->companies.values();
}

void DataStore::updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes) {
    doInBackground(source, [this, updates, adds, deletes] {
        auto changes = BulkUpdate{updates, adds, deletes};
        auto companies = services->companyService.update(changes, user);
        p->companies.update(companies, deletes);
    }, [this]() { emit companiesLoaded(p->companies.values()); });
}
