#ifndef TRANSACTIONGROUPDAO_H
#define TRANSACTIONGROUPDAO_H

#include "entitydao.h"
#include "service/model/transactiongroup.h"

class TransactionGroupDao : public NamedEntityDao<TransactionGroup> {
public:
    TransactionGroupDao(const QString &dbType);

protected:
    virtual void bindUpdateValues(QSqlQuery &query, TransactionGroup *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, TransactionGroup *entity) override;
};

#endif // TRANSACTIONGROUPDAO_H
