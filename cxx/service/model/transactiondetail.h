#ifndef TRANSACTIONDETAIL_H
#define TRANSACTIONDETAIL_H

#include "basedomain.h"

class TransactionDetail : public BaseDomain {
public:
    QVariant transactionId;
    QVariant categoryId;
    QVariant relatedDetailId;
    QVariant groupId;
    QVariant exchangeAssetId;
    QVariant amount;
    QVariant assetQuantity;
    QVariant memo;

    QVariant transferAccountId;

    TransactionDetail();
    TransactionDetail(const QVariant &transactionId);
    TransactionDetail(const QSqlRecord &record);

    bool isEmpty() const;

    TransactionDetail *newTransfer(const QVariant &transferAccountId, const QVariant &transactionId = QVariant{}) const;

    static QVariantList transactionIds(const QList<const TransactionDetail*> details);
};

class Transaction;

class PendingDetail : public TransactionDetail {
public:
    const Transaction* const transaction;

    PendingDetail(const Transaction* const tx);
};

#endif // TRANSACTIONDETAIL_H
