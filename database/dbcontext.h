#ifndef CONNECTION_H
#define CONNECTION_H

#include "accountdao.h"
#include "companydao.h"
#include <QtSql/QSqlDatabase>

class DbContext {
    QString name;
    QSqlDatabase db;

public:
    CompanyDao *companyDao;
    AccountDao *accountDao;

    DbContext(const QString &dbType, const QString &host, const QString &schema, const QString &user, const QString &password);
    ~DbContext();
};

#endif // CONNECTION_H
