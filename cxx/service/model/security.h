#ifndef SECURITY_H
#define SECURITY_H

#include "asset.h"
#include "securitytype.h"

class Security : public Asset {
public:
    QVariant securityType{SecurityType::stock.code};
    QVariant firstAcquired{};
    QVariant shares{};
    QVariant costBasis{};
    QVariant dividends{};

    Security();
    Security(const QSqlRecord &record);
};

#endif // SECURITY_H
