#ifndef ACCOUNTSTORE_H
#define ACCOUNTSTORE_H

#include "companystore.h"
#include "entitystore.h"
#include "service/servicecontext.h"

class AccountTableModel;

class AccountStore : public EntityStore<Account, AccountService> {
    Q_OBJECT
public:
    CompanyStore companyStore;

    AccountStore(ServiceContext *services, StatusMessageStore* messageStore);

    bool load(EntityView *source, bool reload = false);

    void update(QWidget *source, AccountTableModel *model);

    QString qualifiedName(domain_id accountId, QChar delimiter) const;

public slots:
    void transactionsUpdated(const QList<TransactionChange> changes);

protected:
    void update(QWidget *source, QList<Account*> updates, const QList<const Account*> adds, const QList<const Account*> deletes);
    using EntityStore::update;
};

#endif // ACCOUNTSTORE_H
