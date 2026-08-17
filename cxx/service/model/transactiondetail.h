#ifndef TRANSACTIONDETAIL_H
#define TRANSACTIONDETAIL_H

#include "basedomain.h"

class TransactionDetail : public BaseDomain {
public:
    domain_id transactionId;
    optional_id categoryId;
    optional_id relatedDetailId;
    optional_id groupId;
    optional_id exchangeAssetId;
    QDecNumber amount{"NaN"};
    std::optional<QDecNumber> assetQuantity;
    QString memo;

    optional_id transferAccountId;

    TransactionDetail();
    TransactionDetail(domain_id transactionId);
    TransactionDetail(const QSqlRecord &record);

    bool isEmpty() const;

    TransactionDetail *newTransfer(const optional_id& transferAccountId, domain_id transactionId) const;
    void initTransfer(domain_id transactionId, TransactionDetail &relatedDetail) const;

    static TransactionDetail* copyRecent(const TransactionDetail* detail);
};

class SearchTransactionDetail : public TransactionDetail {
public:
    QDate transactionDate{};
    domain_id accountId;
    optional_id payeeId;
    optional_id securityId;
    QString transactionMemo;

    SearchTransactionDetail();
    SearchTransactionDetail(const QSqlRecord &record);

    bool deletable() const;
};

#endif // TRANSACTIONDETAIL_H
