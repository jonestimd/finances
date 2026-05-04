#ifndef TRANSACTIONGROUPDAO_H
#define TRANSACTIONGROUPDAO_H

#include "entitydao.h"
#include "service/model/transactiongroup.h"

class TransactionGroupDao : public NamedEntityDao<TransactionGroup> {
    const char *createTableSql;

public:
    TransactionGroupDao(const QString &dbType);

    void createTable(const QSqlDatabase &db) const;

protected:
    virtual void bindUpdateValues(QSqlQuery &query, TransactionGroup *entity) override;
    virtual void bindInsertValues(QSqlQuery &query, TransactionGroup *entity) override;
};

#endif // TRANSACTIONGROUPDAO_H
