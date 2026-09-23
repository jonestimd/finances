#include "stocksplitstore.h"

StockSplitStore::StockSplitStore(StockSplitService *service, StatusMessageStore *messageStore)
    : EntityStore{service, messageStore}
{}

QList<domain_id> StockSplitStore::getSplits(domain_id securityId) const {
    QList<domain_id> splits;
    forEachEntry([&](domain_id splitId, const StockSplit* split) {
        if (split->securityId == securityId) splits.append(split->id.value());
    });
    return splits;
}

QDecNumber StockSplitStore::adjustedShares(const SecurityPurchase* purchase, const Transaction* sale, QDecNumber shares) const {
    return adjustedShares(sale->securityId.value(), purchase->transactionDate, shares, sale->date);
}

QDecNumber StockSplitStore::adjustedShares(domain_id securityId, const QDate &purchaseDate, const QDecNumber &shares, const QDate asOfDate) const {
    auto adjusted = shares;
    forEachEntry([&](domain_id splitId, const StockSplit* split) {
        if (split->securityId == securityId && purchaseDate <= split->date && (asOfDate.isNull() || split->date <= asOfDate)) {
            adjusted = adjusted.multiply(split->sharesOut);
            adjusted = adjusted.divide(split->sharesIn);
        }
    });
    return adjusted;
}

QDecNumber StockSplitStore::purchaseShares(const Transaction* sale, const QDate& purchaseDate, const QDecNumber& saleShares) const {
    auto securityId = sale->securityId.value();
    auto saleDate = sale->date;
    auto adjusted = saleShares;
    forEachEntry([&](domain_id splitId, const StockSplit* split) {
        if (split->securityId == securityId && purchaseDate <= split->date && (split->date <= saleDate)) {
            adjusted = adjusted.multiply(split->sharesIn);
            adjusted = adjusted.divide(split->sharesOut);
        }
    });
    return adjusted;
}

SplitRatio StockSplitStore::totalSplits(domain_id securityId, const QDate& purchaseDate, const QDate asOfDate) const {
    SplitRatio ratio;
    forEachEntry([&](domain_id splitId, const StockSplit* split) {
        if (split->securityId == securityId && purchaseDate <= split->date && (asOfDate.isNull() || split->date <= asOfDate)) {
            ratio.multiply(split);
        }
    });
    return ratio;
}
