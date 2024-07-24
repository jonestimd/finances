#ifndef DATASTORE_H
#define DATASTORE_H

#include "service/servicecontext.h"
#include <QFuture>

class DataStorePrivate;

class DataStore : public QObject
{
    Q_OBJECT
    DataStorePrivate *p;
    ServiceContext *services;
    const QString user;
public:
    DataStore(ServiceContext *services);
    ~DataStore();

    bool loadAccounts(QWidget *source, bool reload = false);
    const QHash<qlonglong, const Account*> accounts() const;
    void updateAccounts(QWidget *source, QList<Account*> updates, const QList<Account*> adds, const QList<const Account*> deletes);

    bool loadCompanies(QWidget *source, bool reload = false);
    const QHash<qlonglong, const Company*> companies() const;
    void updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes);
    void addCompany(QWidget *source, const QString &name, std::function<void(const Company*)> callback);

Q_SIGNALS:
    void accountsLoaded(const QHash<qlonglong, const Account*>);
    void companiesLoaded(const QHash<qlonglong, const Company*>);
};

#endif // DATASTORE_H
