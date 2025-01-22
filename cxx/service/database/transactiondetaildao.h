#ifndef TRANSACTIONDETAILDAO_H
#define TRANSACTIONDETAILDAO_H

#include "service/model/category.h"
#include "service/model/transactiondetail.h"
#include "entitydao.h"
#include <QSqlDatabase>

class TransactionDetailDao : public EntityDao<TransactionDetail> {
public:
    TransactionDetailDao();

    QHash<qlonglong, const TransactionDetail*> getAll(const QSqlDatabase &db, const QVariant &accountId);

    void replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user);

protected:
    virtual void bindInsertValues(QSqlQuery &query, TransactionDetail *entity) override;
};

static TransactionDetailDao transactionDetailDao;

#endif // TRANSACTIONDETAILDAO_H
