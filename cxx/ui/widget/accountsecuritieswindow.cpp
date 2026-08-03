#include "accountsecuritieswindow.h"
#include "statusmessage.h"
#include "ui/model/accountsecuritymodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"

#define SETTINGS_GROUP "accountSecuritiess"

AccountSecuritiesWindow::AccountSecuritiesWindow(UiContext *context)
    : ReadOnlyEntityWindow{tr("Account Securities"), new AccountSecurityTableModel(context->dataStore), new TreeView(), &context->dataStore->messageStore}
    , dataStore{context->dataStore}
{
    entityView.addActions({
        context->accountsAction(),
        context->securitiesAction(),
    });
    setWindowTitle(tr("%1 - Account Securities").arg(dataStore->connectionName()));
    auto view = treeView();
    view->setItemsExpandable(false);
    view->setRootIsDecorated(false);
    view->setIndentation(5);
    view->setRootSpansAllColumns();

    connect(dataStore->securityStore, SIGNAL(accountSecuritiesLoaded(QList<const AccountSecurity*>)),
        entityView.model(), SLOT(setRows(QList<const AccountSecurity*>)));
    connect(dataStore->securityStore, SIGNAL(accountSecuritiesUpdated(QList<const AccountSecurity*>)),
        entityView.model(), SLOT(updateRows(QList<const AccountSecurity*>)));
    connect(dataStore->securityStore, SIGNAL(accountSecuritiesRemoved(QList<AccountSecurityId>)),
        entityView.model(), SLOT(removeRows(QList<AccountSecurityId>)));
    connect(dataStore->transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
        this, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)));
    connect(entityView.model(), SIGNAL(modelReset()), this, SLOT(modelReset()));

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

AccountSecuritiesWindow::~AccountSecuritiesWindow() {
    delete entityView.model();
}

void AccountSecuritiesWindow::loadData() {
    dataStore->securityStore->loadAccountSecurities(&entityView);
    dataStore->accountStore->load(&entityView);
    dataStore->securityStore->load(&entityView);
}

void AccountSecuritiesWindow::saveData() {
}

void AccountSecuritiesWindow::modelReset() {
    auto view = treeView();
    view->expandAll();
}

void AccountSecuritiesWindow::transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges) {
    if (isVisible()) {
        QSet<domain_id> accountIds, securityIds;
        QList<domain_id> txIds;
        for (auto i = txChanges.begin(); i != txChanges.end(); i++) {
            i.value().appendIds(txIds, accountIds, securityIds);
        }
        for (auto i = detailChanges.begin(); i != detailChanges.end(); i++) {
            auto txId = i.value().oldDetail ? i.value().oldDetail->transactionId : i.value().newDetail->transactionId;
            if (!txIds.contains(txId) && i.value().isSecurityChange()) {
                dataStore->transactionStore->value(txId)->appendIds(accountIds, securityIds);
            }
        }
        if (!securityIds.empty()) {
            dataStore->securityStore->loadAccountSecurities(&entityView, accountIds.values(), securityIds.values());
        }
    }
}

void AccountSecuritiesWindow::showEvent(QShowEvent *event) {
    loadData();
    ReadOnlyEntityWindow::showEvent(event);
}
