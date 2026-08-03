#ifndef SECURITYSTORE_H
#define SECURITYSTORE_H

#include <QDate>
#include <QDecNumber.hh>
#include "entitystore.h"
#include "stocksplitstore.h"
#include "service/securityservice.h"

class SecurityStore : public EntityStore<Security, SecurityService> {
    Q_OBJECT
public:
    StockSplitStore stockSplitStore;

    SecurityStore(SecurityService *service, StatusMessageStore* messageStore, StockSplitService* stockSplitService);

    bool load(EntityView* view, bool reload = false);
    void loadSecurities(EntityView *view, const QList<domain_id> securityIds);

    void loadAccountSecurities(EntityView *view);
    void loadAccountSecurities(EntityView *view, const QList<domain_id> accountIds, const QList<domain_id> securityIds);

Q_SIGNALS:
    void accountSecuritiesLoaded(QList<const AccountSecurity*> accountSecurities); // clazy:exclude=fully-qualified-moc-types
    void accountSecuritiesUpdated(QList<const AccountSecurity*> accountSecurities); // clazy:exclude=fully-qualified-moc-types
    void accountSecuritiesRemoved(QList<AccountSecurityId> accountSecurityIds);
};

#endif // SECURITYSTORE_H
