#ifndef ACCOUNTDAO_H
#define ACCOUNTDAO_H

#include "model/account.h"
#include <QtSql/QSqlDatabase>

class AccountDao {
    QSqlDatabase db;

public:
    AccountDao(QSqlDatabase db);
    QList<Account*> getAll();
};

#endif // ACCOUNTDAO_H
