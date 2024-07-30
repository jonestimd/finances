#ifndef PAYEE_DAO_H
#define PAYEE_DAO_H

#include "entitydao.h"
#include "../model/payee.h"
#include <QtSql/QSqlDatabase>

class PayeeDao : public QObject, public EntityDao<Payee> {
    Q_OBJECT
public:
    PayeeDao();
};

Q_GLOBAL_STATIC(PayeeDao, payeeDao)

#endif // PAYEE_DAO_H
