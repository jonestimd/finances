#ifndef TRANSACTIONDAO_H
#define TRANSACTIONDAO_H

#include "service/model/payee.h"
#include "service/model/transaction.h"
#include "entitydao.h"
#include <QSqlDatabase>

class TransactionDao : public EntityDao<Transaction> {
    const char *getByAccountSql;

public:
    TransactionDao(const QString &dbType);

    virtual void createTable(const QSqlDatabase &db) const override;

    QHash<qlonglong, const Transaction*> getAll(const QSqlDatabase &db, qlonglong accountId);

    using EntityDao::add;
    /**
     * @brief TransactionDao::add Saves transactions to the database and sets `transactionId` on the details.
     * @return `adds` with updated details.
     */
    const QList<PendingTransaction*> add(QSqlDatabase &db, const QList<PendingTransaction*> adds, const QString &user);

    void setAccountId(const QSqlDatabase& db, const QVariant& transactionId, const QVariant& oldAccountId, const QVariant& newAccountId, const QString& user);

    void replacePayee(const QSqlDatabase& db, const Payee* payee, const QVariant newPayeeId, const QString& user);

    QList<QVariant> removeEmpty(QSqlDatabase &db);
    
    Transaction *addRelatedTransaction(QSqlDatabase &db, TransactionDetail *detail, const QString &user);

protected:
    virtual void bindInsertValues(QSqlQuery &query, Transaction *transaction) override;
    virtual void bindUpdateValues(QSqlQuery &query, Transaction *entity) override;
};

#endif // TRANSACTIONDAO_H
