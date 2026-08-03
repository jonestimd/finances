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

QDecNumber StockSplitStore::adjustedShares(domain_id securityId, const QDate &date, const QDecNumber &shares) const {
    auto adjusted = shares;
    forEachEntry([&](domain_id splitId, const StockSplit* split) {
        if (split->securityId == securityId && date <= split->date) {
            adjusted = adjusted.multiply(split->sharesOut);
            adjusted = adjusted.divide(split->sharesIn);
        }
    });
    return adjusted;
}
