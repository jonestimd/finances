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
    mutable std::optional<QDecNumber> lotShares;

    optional_id transferAccountId;

    TransactionDetail();
    TransactionDetail(domain_id transactionId);
    TransactionDetail(const QSqlRecord& record);

    bool isEmpty() const;
    bool isMissingLots() const;

    TransactionDetail *newTransfer(const optional_id& transferAccountId, domain_id transactionId) const;
    void initTransfer(domain_id transactionId, TransactionDetail &relatedDetail) const;

    static TransactionDetail* copyRecent(const TransactionDetail* detail);
};

/**
 * @brief The SecurityPurchase class provides account specific data for a purchase.
 * @sa TransactionDetailDao::findPreviousPurchases.
 */
class SecurityPurchase : public TransactionDetail {
public:
    QDate transactionDate{};
    /** @brief Shares purchased in or transfered to the account (unadjusted for splits). */
    QDecNumber accountShares{0};
    /** @brief Shares allocated to sales in the account (unadjusted for splits). */
    mutable QDecNumber allocatedShares{0};

    SecurityPurchase();
    SecurityPurchase(const QSqlRecord& record);

    /** @return unallocated purchase shares (unadjusted for splits). */
    QDecNumber availableShares() const;
    /** @return cost of the shares in the account. */
    QDecNumber cost() const;
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

struct DetailSearchCriteria {
    QString text;
    optional_id payeeId;
    optional_id securityId;
    optional_id categoryId;
    bool missingLots{false};

    DetailSearchCriteria() = default;
    DetailSearchCriteria(const QString text, optional_id payeeId, optional_id securityId, optional_id categoryId);
    DetailSearchCriteria(bool missingLots);
};

#endif // TRANSACTIONDETAIL_H
