#ifndef DATASTORE_H
#define DATASTORE_H

#include "service/servicecontext.h"
#include "accountstore.h"
#include "categorystore.h"
#include "companystore.h"
#include "groupstore.h"
#include "payeestore.h"
#include "securitystore.h"
#include "transactionstore.h"

class ConnectionDialog;

class DataStore : public QObject {
    Q_OBJECT
    ServiceContext *services;

public:
    StatusMessageStore messageStore;
    AccountStore *const accountStore;
    PayeeStore *const payeeStore;
    CategoryStore *const categoryStore;
    GroupStore *const groupStore;
    SecurityStore *const securityStore;
    TransactionStore *const transactionStore;

    DataStore(ServiceContext *services);
    DataStore(const ConnectionSettings &settings);
    ~DataStore();

    const QString connectionName() const;
    const QString connectionConfigName() const;

    /** @brief Used by FileDialog to verify connection parameters. */
    void loadAccounts(ConnectionDialog* dialog);

    void shutdown();

    static const QString user;
};

#endif // DATASTORE_H
