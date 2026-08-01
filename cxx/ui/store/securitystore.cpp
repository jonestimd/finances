#include "securitystore.h"
#include "ui/widget/statusmessage.h"

SecurityStore::SecurityStore(SecurityService *service, StatusMessageStore* messageStore)
    : EntityStore{service, messageStore}
{}

QDecNumber SecurityStore::adjustedShares(const QVariant &securityId, const QDate &date, const QDecNumber &shares) const {
    auto adjusted = shares;
    for (auto split : stockSplits.values(securityId.toLongLong())) {
        if (date <= split->date) {
            adjusted = adjusted.multiply(split->sharesOut);
            adjusted = adjusted.divide(split->sharesIn);
        }
    }
    return adjusted;
}

void SecurityStore::loadSecurities(EntityView *view, const QList<domain_id> securityIds) {
    doInBackground(view->statusBar.parentWidget(), tr(LOADING_SECURITIES), [=, this]() {
        auto securities = service->getSecurities(securityIds);
        update(securities.values());
        emit valuesLoaded(ids());
    });
}

void SecurityStore::loadAccountSecurities(EntityView *view)  {
    doInBackground(view->statusBar.parentWidget(), tr(LOADING_ACCOUNT_SECURITIES), [=, this]() {
        auto accountSecurities = service->getAccountSecurities();
        emit accountSecuritiesLoaded(accountSecurities.values());
    });
}

void SecurityStore::loadAccountSecurities(EntityView *view, const QList<domain_id> accountIds, const QList<domain_id> securityIds) {
    doInBackground(view->statusBar.parentWidget(), tr(LOADING_ACCOUNT_SECURITIES), [=, this]() {
        auto accountSecurities = service->getAccountSecurities(accountIds, securityIds);
        emit accountSecuritiesUpdated(accountSecurities.values());
        QList<AccountSecurityId> removedIds;
        for (auto accountId : accountIds) {
            for (auto securityId : securityIds) {
                auto id = AccountSecurityId{accountId, securityId};
                if (!accountSecurities.contains(id)) removedIds.append(id);
            }
        }
        if (!removedIds.isEmpty()) emit accountSecuritiesRemoved(removedIds);
    });
}

void SecurityStore::setValues(const QHash<domain_id, const Security *> values) {
    for (auto i = stockSplits.begin(); i != stockSplits.end(); i = stockSplits.erase(i)) {
        delete i.value();
    }
    if (!values.isEmpty()) {
        auto const splits = service->getSplits();
        for (auto split : splits) stockSplits.insert(split->securityId, split);
    }
    EntityStore::setValues(values);
}
