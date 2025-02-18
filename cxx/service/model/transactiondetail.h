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

    TransactionDetail(const QVariant &transactionId);
    TransactionDetail(const QSqlRecord &record);
};

struct TransactionDetailUpdate {
    TransactionDetail *detail;
    QVariant transferAccountId{};
    QVariant categoryId{};
};

#endif // TRANSACTIONDETAIL_H
