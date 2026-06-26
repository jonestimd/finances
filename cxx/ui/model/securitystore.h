#ifndef SECURITYSTORE_H
#define SECURITYSTORE_H

#include <QDate>
#include <QDecNumber.hh>
#include "entitystore.h"
#include "service/securityservice.h"

class SecurityStore : public EntityStore<Security, SecurityService> {
    Q_OBJECT

    QMultiHash<domain_id, const StockSplit*>securitySplits{};

public:
    SecurityStore(SecurityService *service, StatusMessageStore* messageStore);

    QDecNumber adjustedShares(const QVariant &securityId, const QDate &date, const QDecNumber &shares) const;

public slots:
    void transactionsUpdated(const QList<TransactionChange> changes);

protected:
    virtual void setValues(const QHash<domain_id, const Security*> values) override;
};

#endif // SECURITYSTORE_H
