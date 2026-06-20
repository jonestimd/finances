#ifndef PAYEE_DAO_H
#define PAYEE_DAO_H

#include "entitydao.h"
#include "../model/payee.h"
#include <QtSql/QSqlDatabase>

class PayeeDao : public NamedEntityDao<Payee> {
public:
    PayeeDao(const QString &dbType);
};

#endif // PAYEE_DAO_H
