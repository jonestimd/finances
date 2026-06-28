#ifndef TRANSACTIONDETAIL_H
#define TRANSACTIONDETAIL_H

#include "basedomain.h"

class TransactionDetail : public BaseDomain {
public:
    domain_id transactionId;
    optional_id categoryId;
    QVariant relatedDetailId;
    QVariant groupId;
    QVariant exchangeAssetId;
    QVariant amount;
    QVariant assetQuantity;
    QVariant memo;

    optional_id transferAccountId;

    TransactionDetail();
    TransactionDetail(domain_id transactionId);
    TransactionDetail(const QSqlRecord &record);

    bool isEmpty() const;

    TransactionDetail *newTransfer(const optional_id& transferAccountId, domain_id transactionId) const;
    void initTransfer(domain_id transactionId, TransactionDetail &relatedDetail) const;

    static QVariantList transactionIds(const QList<const TransactionDetail*> details);
};

#endif // TRANSACTIONDETAIL_H
