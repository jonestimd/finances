#ifndef SECURITY_H
#define SECURITY_H

#include "asset.h"
#include "securitytype.h"
#include <QDate>

class Security : public Asset {
public:
    const SecurityType* securityType{&SecurityType::stock};
    std::optional<QDate> firstAcquired{};
    QDecNumber shares{0};
    QDecNumber costBasis{0};
    QDecNumber dividends{0};

    Security();
    Security(const QSqlRecord &record);
};

#endif // SECURITY_H
