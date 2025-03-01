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

    TransactionDetail();
    TransactionDetail(const QVariant &transactionId);
    TransactionDetail(const QSqlRecord &record);

    bool isEmpty() const;
};

struct TransactionDetailUpdate {
    TransactionDetail *detail;
    QVariant transferAccountId{};

    TransactionDetailUpdate(const QVariant &transactionId = QVariant{});
    TransactionDetailUpdate(TransactionDetail *detail);
};

#endif // TRANSACTIONDETAIL_H
