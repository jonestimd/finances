#ifndef SECURITY_H
#define SECURITY_H

#include "asset.h"
#include "securitytype.h"
#include <QDate>

class Security : public Asset {
public:
    const SecurityType* securityType{&SecurityType::stock};
    std::optional<QDate> firstAcquired{};
    QDecNumber shares{0}; // TODO update when transactions added/removed/updated
    QDecNumber costBasis{0};
    QDecNumber dividends{0};

    Security();
    Security(const QSqlRecord &record);
};

struct AccountSecurityId {
    const domain_id accountId;
    const domain_id securityId;

    AccountSecurityId(domain_id accountId, domain_id securityId);
    AccountSecurityId(const QSqlRecord &record);

    bool operator==(const AccountSecurityId that) const;
};

size_t qHash(const AccountSecurityId& key, size_t seed = 0);

class AccountSecurity {
public:
    const AccountSecurityId id;
    QDate firstAcquired{};
    mutable QDecNumber shares{0}; // TODO update when transactions added/removed/updated
    mutable QDecNumber costBasis{0};
    mutable QDecNumber dividends{0};
    mutable int transactions{0};

    AccountSecurity(const QSqlRecord &record);
};

#endif // SECURITY_H
