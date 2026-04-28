#ifndef TRANSACTIONDETAILDAO_H
#define TRANSACTIONDETAILDAO_H

#include "service/model/category.h"
#include "service/model/transactiondetail.h"
#include "service/model/transaction.h"
#include "entitydao.h"
#include <QSqlDatabase>

struct RelatedDetailIds {
    QVariant accountId;
    QVariant relatedDetailId;
};

class TransactionDetailDao : public EntityDao<TransactionDetail> {
public:
    TransactionDetailDao();

    void createTable(const QSqlDatabase &db);

    QHash<qlonglong, const TransactionDetail*> getAll(const QSqlDatabase &db, const QVariant &accountId);

    void removeByTransaction(QSqlDatabase &db, const QList<const Transaction*> transactions);

    void replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user);

    virtual QList<const TransactionDetail*> update(QSqlDatabase &db, const QList<TransactionDetail*> entities, const QString &user) override;

    void setRelatedDetailIds(QSqlDatabase &db, const QHash<TransactionDetail*, TransactionDetail*> transfers);
    
    QHash<qlonglong, RelatedDetailIds> getRelatedDetailIds(QSqlDatabase &db, const QList<TransactionDetail*> updates);

    using EntityDao<TransactionDetail>::remove;
    virtual void remove(QSqlDatabase &db, const QList<const TransactionDetail*> entities) override;

protected:
    virtual void bindInsertValues(QSqlQuery &query, TransactionDetail *entity) override;
    virtual void bindUpdateValues(QSqlQuery &query, TransactionDetail *entity) override;

};

static TransactionDetailDao transactionDetailDao;

#endif // TRANSACTIONDETAILDAO_H
