#ifndef SECURITY_H
#define SECURITY_H

#include "asset.h"
#include "securitytype.h"
#include <QDate>

class SecurityStats {
public:
    mutable std::optional<QDate> firstAcquired{};
    mutable QDecNumber shares{0}; // TODO update when transactions added/removed/updated
    mutable QDecNumber costBasis{0};
    mutable QDecNumber dividends{0};
    mutable int transactions{0};

    SecurityStats();
    SecurityStats(const QSqlRecord &record);
};

class Security : public Asset, public SecurityStats {
public:
    const SecurityType* securityType{&SecurityType::stock};

    Security(const QString &name = "");
    Security(const QSqlRecord &record);

    bool deletable() const;
};

struct AccountSecurityId {
    const domain_id accountId;
    const domain_id securityId;

    AccountSecurityId(domain_id accountId, domain_id securityId);
    AccountSecurityId(const QSqlRecord &record);

    bool operator==(const AccountSecurityId that) const;
};

size_t qHash(const AccountSecurityId& key, size_t seed = 0);

class AccountSecurity : public SecurityStats {
public:
    const AccountSecurityId id;

    AccountSecurity(const AccountSecurityId &id);
    AccountSecurity(const QSqlRecord &record);
};

#endif // SECURITY_H
