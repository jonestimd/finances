#ifndef ACCOUNTDAO_H
#define ACCOUNTDAO_H

#include "../model/account.h"
#include <QtSql/QSqlDatabase>

namespace accountDao {
    QList<Account*> getAll(QSqlDatabase db);
}

#endif // ACCOUNTDAO_H
