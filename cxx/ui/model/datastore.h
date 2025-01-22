#ifndef DATASTORE_H
#define DATASTORE_H

#include "service/servicecontext.h"
#include "accountstore.h"
#include "categorystore.h"
#include "companystore.h"
#include "payeestore.h"
#include "securitystore.h"
#include "transactionstore.h"

typedef EntityStore<TransactionGroup, GroupService> GroupStore;

class DataStore : public QObject
{
    Q_OBJECT
    ServiceContext *services;

public:
    AccountStore *const accountStore;
    PayeeStore *const payeeStore;
    CategoryStore *const categoryStore;
    GroupStore *const groupStore;
    SecurityStore *const securityStore;
    TransactionStore *const transactionStore;

    DataStore(ServiceContext *services);
    ~DataStore();

    const QString connectionName() const;

    static const QString user;
};

#endif // DATASTORE_H
