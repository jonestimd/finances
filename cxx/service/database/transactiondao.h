#ifndef TRANSACTIONDAO_H
#define TRANSACTIONDAO_H

#include "service/model/payee.h"
#include "service/model/transaction.h"
#include "entitydao.h"
#include <QSqlDatabase>

class TransactionDao : public EntityDao<Transaction> {
public:
    TransactionDao();

    void createTable(const QSqlDatabase &db);

    QHash<qlonglong, const Transaction*> getAll(const QSqlDatabase &db, qlonglong accountId);

    void replacePayee(const QSqlDatabase &db, const Payee *payee, const QVariant newPayeeId, const QString &user);

    void removeEmpty(const QSqlDatabase &db);
    
    Transaction *addRelatedTransaction(QSqlDatabase &db, TransactionDetail *detail, const QString &user);

protected:
    virtual void bindInsertValues(QSqlQuery &query, Transaction *transaction) override;
    virtual void bindUpdateValues(QSqlQuery &query, Transaction *entity) override;
};

static TransactionDao transactionDao;

#endif // TRANSACTIONDAO_H
