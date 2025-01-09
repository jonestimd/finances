#ifndef TRANSACTIONDETAILDAO_H
#define TRANSACTIONDETAILDAO_H

#include "service/model/category.h"
#include <QSqlDatabase>

class TransactionDetailDao {
public:
    TransactionDetailDao();

    void replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user);
};

static TransactionDetailDao transactionDetailDao;

#endif // TRANSACTIONDETAILDAO_H
