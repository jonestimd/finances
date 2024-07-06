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
    QList<const Account*> accounts() const;

    bool loadCompanies(QWidget *source, bool reload = false);
    QList<const Company*> companies() const;
    void updateCompanies(QWidget *source, QList<Company*> updates, const QList<Company*> adds, const QList<const Company*> deletes);

Q_SIGNALS:
    void accountsLoaded(QList<const Account*>);
    void companiesLoaded(QList<const Company*>);
};

#endif // DATASTORE_H
