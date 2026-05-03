#ifndef ACCOUNT_DAO_H
#define ACCOUNT_DAO_H

#include "entitydao.h"
#include "../model/account.h"
#include <QtSql/QSqlDatabase>

class AccountDao : public NamedEntityDao<Account> {
public:
    AccountDao();

    void createTable(const QSqlDatabase &db);

    using NamedEntityDao::getAll;
    virtual const char *getLoadAllQuery(QSqlDatabase &db) const override;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Account *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Account *entity) override;
};

static AccountDao accountDao;

#endif // ACCOUNT_DAO_H
