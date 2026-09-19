#ifndef STOCK_SPLIT_STORE_H
#define STOCK_SPLIT_STORE_H

#include <QDate>
#include <QDecNumber.hh>
#include "entitystore.h"
#include "service/servicecontext.h"

class StockSplitStore : public EntityStore<StockSplit, StockSplitService> {
    Q_OBJECT
public:
    StockSplitStore(StockSplitService *service, StatusMessageStore* messageStore);

    QList<domain_id> getSplits(domain_id securityId) const;
    
    QDecNumber adjustedShares(const SecurityPurchase* purchase, const Transaction* sale, QDecNumber shares) const;
    QDecNumber adjustedShares(domain_id securityId, const QDate &purchaseDate, const QDecNumber &shares, const QDate asOfDate = {}) const;
    QDecNumber purchaseShares(const Transaction* sale, const QDate &purchaseDate, const QDecNumber &saleShares) const;

    SplitRatio totalSplits(domain_id securityId, const QDate &purchaseDate, const QDate asOfDate = {}) const;
};

#endif // STOCK_SPLIT_STORE_H
