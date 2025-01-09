#ifndef ACCOUNT_DAO_H
#define ACCOUNT_DAO_H

#include "entitydao.h"
#include "../model/account.h"
#include <QtSql/QSqlDatabase>

class AccountDao : public EntityDao<Account> {
public:
    AccountDao();

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Account *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Account *entity) override;
};

static AccountDao accountDao;

#endif // ACCOUNT_DAO_H
