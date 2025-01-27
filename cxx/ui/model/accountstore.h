#ifndef ACCOUNTSTORE_H
#define ACCOUNTSTORE_H

#include "companystore.h"
#include "entitystore.h"
#include "service/servicecontext.h"

class AccountTableModel;

class AccountStore : public EntityStore<Account, AccountService> {
public:
    CompanyStore *const companyStore;

    AccountStore(ServiceContext *services);
    ~AccountStore();

    void update(QWidget *source, AccountTableModel *model);

protected:
    void update(QWidget *source, QList<Account*> updates, const QList<Account*> adds, const QList<const Account*> deletes);
    using EntityStore::update;
};

#endif // ACCOUNTSTORE_H
