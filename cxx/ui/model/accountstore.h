#ifndef ACCOUNTSTORE_H
#define ACCOUNTSTORE_H

#include "companystore.h"
#include "entitystore.h"
#include "service/servicecontext.h"

class AccountTableModel;

class AccountStore : public EntityStore<Account, AccountService> {
public:
    CompanyStore companyStore;

    AccountStore(ServiceContext *services);

    bool load(EntityView *source, bool reload = false);

    void update(QWidget *source, AccountTableModel *model);

    QString qualifiedName(const QVariant &accountId, QChar delimiter) const;

protected:
    void update(QWidget *source, QList<Account*> updates, const QList<const Account*> adds, const QList<const Account*> deletes);
    using EntityStore::update;
};

#endif // ACCOUNTSTORE_H
