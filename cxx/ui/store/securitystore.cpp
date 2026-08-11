#include "securitystore.h"
#include "ui/widget/statusmessage.h"

SecurityStore::SecurityStore(SecurityService *service, StatusMessageStore* messageStore, StockSplitService* stockSplitService)
    : EntityStore{service, messageStore}
    , stockSplitStore{stockSplitService, messageStore}
{}

bool SecurityStore::load(EntityView *view, bool reload) {
    bool loaded = EntityStore::load(view, tr(LOADING_SECURITIES), reload);
    if (!loaded) stockSplitStore.load(view, tr(LOADING_STOCK_SPLITS), true);
    return loaded;
}

void SecurityStore::loadSecurities(EntityView *view, const QList<domain_id> securityIds) {
    doInBackground(view->statusBar.parentWidget(), tr(LOADING_SECURITIES), [=, this]() {
        auto securities = service->getSecurities(securityIds);
        update(securities);
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
