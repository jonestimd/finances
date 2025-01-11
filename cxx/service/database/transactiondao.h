#ifndef TRANSACTIONDETAILDAO_H
#define TRANSACTIONDETAILDAO_H

#include "service/model/payee.h"
#include <QSqlDatabase>

class TransactionDao {
public:
    TransactionDao();

    void replacePayee(QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user);
};

static TransactionDao transactionDao;

#endif // TRANSACTIONDETAILDAO_H
