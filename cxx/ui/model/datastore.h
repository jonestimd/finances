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
    const QString user;
public:
    DataStore(ServiceContext *services);
    ~DataStore();

    bool loadAccounts(QWidget *source);
    QList<Account*> accounts() const;

    bool loadCompanies(QWidget *source);
    QList<Company*> companies() const;
    void updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds);

Q_SIGNALS:
    void accountsLoaded(QList<Account*>);
    void companiesLoaded(QList<Company*>);
};

#endif // DATASTORE_H
