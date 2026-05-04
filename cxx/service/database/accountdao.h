#ifndef ACCOUNT_DAO_H
#define ACCOUNT_DAO_H

#include "entitydao.h"
#include "../model/account.h"
#include <QtSql/QSqlDatabase>

class AccountDao : public NamedEntityDao<Account> {
    const char *createTableSql;

public:
    AccountDao(const QString &dbType);

    void createTable(const QSqlDatabase &db) const;

    using NamedEntityDao::getAll;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, Account *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, Account *entity) override;
};

#endif // ACCOUNT_DAO_H
