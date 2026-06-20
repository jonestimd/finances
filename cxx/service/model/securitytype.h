#ifndef SECURITYTYPE_H
#define SECURITYTYPE_H

#include "basedomain.h"

#define BOND_SECURITY "Bond"
#define MONEY_MARKET_SECURITY "Money Market"
#define MUTUAL_FUND_SECURITY "Mutual Fund"
#define STOCK_SECURITY "Stock"

class SecurityType : public EnumValue {
    SecurityType(const char *code, const QString name);
public:
    static const SecurityType bond;
    static const SecurityType moneyMarket;
    static const SecurityType mutualFund;
    static const SecurityType stock;

    static QHash<const QString, const SecurityType*> values;
};

#endif // SECURITYTYPE_H
