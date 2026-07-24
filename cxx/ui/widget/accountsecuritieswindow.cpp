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
    // entityView.addActions({showAccountsAction});
    setWindowTitle(tr("%1 - Account Securities").arg(dataStore->connectionName()));
    auto view = treeView();
    view->setItemsExpandable(false);
    view->setRootIsDecorated(false);
    view->setIndentation(5);
    view->setRootSpansAllColumns();
    // TODO update TreeView to handle column spans and row colors

    connect(dataStore->securityStore, SIGNAL(accountSecuritiesLoaded(QList<const AccountSecurity*>)), entityView.model(), SLOT(setRows(QList<const AccountSecurity*>)));
    connect(entityView.model(), SIGNAL(modelReset()), this, SLOT(modelReset()));
    loadData(); // NOLINT(clang-analyzer-optin.cplusplus.VirtualCall)

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

AccountSecuritiesWindow::~AccountSecuritiesWindow() {
    delete entityView.model();
}

void AccountSecuritiesWindow::loadData() {
    dataStore->securityStore->loadAccountSecurities(&entityView);
    dataStore->accountStore->load(&entityView, true);
    dataStore->securityStore->load(&entityView, tr(LOADING_SECURITIES), true);
}

void AccountSecuritiesWindow::saveData() {
}

void AccountSecuritiesWindow::modelReset() {
    auto view = treeView();
    view->expandAll();
    // auto rootRows = entityView.model()->rowCount();
    // for (int i = 0; i < rootRows; i++) view->setFirstColumnSpanned(i, {}, true);
}
