#ifndef DATASTORE_H
#define DATASTORE_H

#include "service/servicecontext.h"
#include "background.h"
#include "entitystore.h"
#include "categorystore.h"

class DataStore : public QObject
{
    Q_OBJECT
    ServiceContext *services;
    const QString user;

    EntityStore<const Account*> *const accountStore;
    EntityStore<const Company*> *const companyStore;
    EntityStore<const Payee*> *const payeeStore;
    CategoryStore *const categoryStore;

    template<typename T, class Service>
    bool load(QWidget *source, bool reload, EntityStore<const T*> *store, Service *service, void (DataStore::*valuesLoaded)(QList<qlonglong>)) {
        if (!reload && store->loaded) return true;
        doInBackground(source, [=, this]() {
            store->setValues(service->getAll());
            (this->*valuesLoaded)(store->ids());
        });
        return false;
    }

public:
    DataStore(ServiceContext *services);
    ~DataStore();

    const QString connectionName() const;

    bool loadAccounts(QWidget *source, bool reload = false);
    const EntityStore<const Account*> *accounts() const;
    void updateAccounts(QWidget *source, QList<Account*> updates, const QList<Account*> adds, const QList<const Account*> deletes);

    bool loadCompanies(QWidget *source, bool reload = false);
    const EntityStore<const Company*> *companies() const;
    void updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes);
    void addCompany(QWidget *source, const QString &name, const char *callback);

    bool loadPayees(QWidget *source, bool reload = false);
    const EntityStore<const Payee*> *payees() const;
    void updatePayees(QWidget *source, QList<Payee*> updates, const QList<Payee*> adds, const QList<const Payee*> deletes);
    void mergePayees(QWidget *source, const Payee *payee, const QVariant destinationId);

    bool loadCategories(QWidget *source, bool reload = false);
    const CategoryStore *categories() const;
    void updateCategories(QWidget *source, QList<Category*> updates, const QList<Category*> adds, const QList<const Category*> deletes);
    void setParent(QWidget *source, const Category *category, const QVariant parentId);
    void mergeCategories(QWidget *source, const Category *category, const QVariant destinationId);

Q_SIGNALS:
    void accountsLoaded(const QList<qlonglong>);
    void companiesLoaded(const QList<qlonglong>);
    void payeesLoaded(const QList<qlonglong>);
    void categoriesLoaded(const QList<qlonglong>);
};

#endif // DATASTORE_H
