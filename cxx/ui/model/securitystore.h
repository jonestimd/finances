#ifndef SECURITYSTORE_H
#define SECURITYSTORE_H

#include <QDate>
#include <QDecNumber.hh>
#include "entitystore.h"
#include "service/securityservice.h"

class SecurityStore : public EntityStore<Security, SecurityService> {
    Q_OBJECT
    QMultiHash<domain_id, const StockSplit*> stockSplits{};

public:
    SecurityStore(SecurityService *service, StatusMessageStore* messageStore);

    QDecNumber adjustedShares(const QVariant &securityId, const QDate &date, const QDecNumber &shares) const;

    void loadSecurities(EntityView *view, const QList<domain_id> securityIds);

    void loadAccountSecurities(EntityView *view);
    void loadAccountSecurities(EntityView *view, const QList<domain_id> accountIds, const QList<domain_id> securityIds);

Q_SIGNALS:
    void accountSecuritiesLoaded(QList<const AccountSecurity*> accountSecurities); // clazy:exclude=fully-qualified-moc-types
    void accountSecuritiesUpdated(QList<const AccountSecurity*> accountSecurities); // clazy:exclude=fully-qualified-moc-types
    void accountSecuritiesRemoved(QList<AccountSecurityId> accountSecurityIds);

protected:
    virtual void setValues(const QHash<domain_id, const Security*> values) override;
};

#endif // SECURITYSTORE_H
