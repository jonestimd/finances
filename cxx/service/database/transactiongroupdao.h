#ifndef TRANSACTIONGROUPDAO_H
#define TRANSACTIONGROUPDAO_H

#include "entitydao.h"
#include "service/model/transactiongroup.h"

class TransactionGroupDao : public EntityDao<TransactionGroup> {
public:
    TransactionGroupDao();

protected:
    virtual void bindUpdateValues(QSqlQuery &query, TransactionGroup *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, TransactionGroup *entity) override;
};

static TransactionGroupDao transactionGroupDao;

#endif // TRANSACTIONGROUPDAO_H
