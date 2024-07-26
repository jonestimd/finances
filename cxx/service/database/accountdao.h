#ifndef ACCOUNTDAO_H
#define ACCOUNTDAO_H

#include "../model/account.h"
#include <QtSql/QSqlDatabase>

namespace accountDao {
    QList<const Account*> getAll(QSqlDatabase &db);

    QList<const Account*> update(QSqlDatabase &db, QList<Account*> accounts, const QString &user);

    QList<const Account*> add(QSqlDatabase &db, QList<Account*> accounts, const QString &user);

    void remove(QSqlDatabase &db, QList<const Account*> companies);
}

#endif // ACCOUNTDAO_H
