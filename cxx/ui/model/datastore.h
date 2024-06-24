#ifndef DATASTORE_H
#define DATASTORE_H

#include "../../service/servicecontext.h"
#include <QFuture>

class DataStorePrivate;

class DataStore : public QObject
{
    Q_OBJECT
    DataStorePrivate *p;
    ServiceContext *services;
public:
    DataStore(ServiceContext *services);
    ~DataStore();

    bool loadAccounts();
    QList<Account*> accounts() const;

    bool loadCompanies();
    QList<Company*> companies() const;

Q_SIGNALS:
    void accountsLoaded(QList<Account*>);
    void companiesLoaded(QList<Company*>);
};

#endif // DATASTORE_H
