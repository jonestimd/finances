#ifndef TRANSACTIONDETAIL_H
#define TRANSACTIONDETAIL_H

#include "basedomain.h"

class TransactionDetail : public BaseDomain {
public:
    qlonglong transactionId;
    QVariant categoryId;
    QVariant relatedDetailId;
    QVariant groupId;
    QVariant exchangeAssetId;
    QVariant amount;
    QVariant assetQuantity;
    QVariant memo;

    std::optional<qlonglong> transferAccountId;

    TransactionDetail();
    TransactionDetail(qlonglong transactionId);
    TransactionDetail(const QSqlRecord &record);

    bool isEmpty() const;

    TransactionDetail *newTransfer(const std::optional<qlonglong> &transferAccountId, qlonglong transactionId) const;
    void initTransfer(qlonglong transactionId, TransactionDetail &relatedDetail) const;

    static QVariantList transactionIds(const QList<const TransactionDetail*> details);
};

#endif // TRANSACTIONDETAIL_H
