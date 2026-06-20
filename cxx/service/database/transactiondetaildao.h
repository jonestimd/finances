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
    QVariant relatedTransactionId;
    QVariant transferAccountId;

    RelatedDetailIds() = default;
    RelatedDetailIds(QSqlRecord& record);
};

class TransactionDetailDao : public EntityDao<TransactionDetail> {
    const char *deleteIdsByTransactionSql;
    const char *deleteByIdsSql;
    const char *updateTransferAmountSql;
    const char *getRelatedIdsSql;

public:
    TransactionDetailDao(const QString &dbType);

    QHash<qlonglong, const TransactionDetail*> getAll(const QSqlDatabase &db, const QVariant &accountId);

    const TransactionDetail* addRelatedDetail(QSqlDatabase& db, const QVariant& txId, const TransactionDetail* detail, const QString& user);

    /**
     *  @param relatedTransactionIds Output list for IDs of related transactions.
     *  @returns IDs of deleted related transaction details.
     */
    QVariantList removeByTransaction(QSqlDatabase &db, const QList<const Transaction*> transactions, QVariantList& relatedTransactionIds);

    void replaceCategory(QSqlDatabase &db, const Category *category, const QVariant newCategoryId, const QString &user);

    /**
     * @brief update Saves `details` to the database and updates related details (transfer amounts).
     * @return input `details` and related details for transfers.
     */
    virtual QList<const TransactionDetail*> update(QSqlDatabase &db, const QList<TransactionDetail*> details, const QString &user) override;

    void setRelatedDetailIds(QSqlDatabase &db, const QHash<TransactionDetail*, qlonglong> relatedIds);
    
    QHash<qlonglong, RelatedDetailIds> getRelatedDetailIds(QSqlDatabase &db, const QList<TransactionDetail*> updates);

    using EntityDao<TransactionDetail>::remove;
    virtual void remove(QSqlDatabase &db, const QList<const TransactionDetail*> entities) override;

protected:
    virtual void bindInsertValues(QSqlQuery &query, TransactionDetail *entity) override;
    virtual void bindUpdateValues(QSqlQuery &query, TransactionDetail *entity) override;

private:
    void removeByIds(QSqlDatabase &db, const QVariantList ids);
};

#endif // TRANSACTIONDETAILDAO_H
