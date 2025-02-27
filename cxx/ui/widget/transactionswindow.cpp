#include "accountsmenu.h"
#include "statusmessage.h"
#include "transactionswindow.h"
#include "ui/model/formats.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>
#include <QMenu>
#include <QMenuBar>

#define TRANSACTION_SETTINGS "transactions"
#define SECURITY_TRANSACTION_SETTINGS "security." TRANSACTION_SETTINGS
#define SETTINGS_GROUP(security) (security ? SECURITY_TRANSACTION_SETTINGS : TRANSACTION_SETTINGS)
#define HIDE_CLOSED_ACCOUNTS "hideClosedAccounts"
#define CLEARED_WIDTH 30

namespace transactionwindow {
    QFrame *separator() {
        QFrame *frame = new QFrame();
        frame->setProperty("separator", "true");
        frame->setFrameStyle(QFrame::VLine | QFrame::Raised);
        return frame;
    }
}

using namespace transactionwindow;

TransactionsWindow::TransactionsWindow(UiContext *context, TransactionTableModel *model)
    : AppWindow{tr("Transaction"), model, new TreeView()}
    , context{context}
{
    setWindowTitle(QString("%1 - Transactions").arg(connectionName()));
    setAttribute(Qt::WA_DeleteOnClose, true);
    entityView.addActions({finances::iconAction(finances::NewWindow, tr("New Window"), tr("alt+n"), this, SLOT(newWindow()))});
    entityView.addActions({
        context->accountsAction(),
        context->payeesAction(),
        context->categoriesAction(),
        context->groupsAction(),
        context->securitiesAction(),
    });
    QMenuBar *menuBar = new QMenuBar();
    menuBar->addMenu(new AccountsMenu(this, context));
    QHBoxLayout *layout = new QHBoxLayout();
    layout->setContentsMargins(0, 0, 0, 0);
    layout->addWidget(menuBar, 0, Qt::AlignCenter);
    layout->addWidget(separator());
    layout->addWidget(&entityView.toolbar, 1);
    QFrame *frame = new QFrame();
    frame->setFrameStyle(QFrame::Panel | QFrame::Raised);
    frame->setLineWidth(2);
    frame->setLayout(layout);
    setMenuWidget(frame);

    entityView.statusBar.addPermanentWidget(clearedBalance);
    connect(model, SIGNAL(clearedBalanceChanged(QDecNumber)), this, SLOT(clearedBalanceChanged(QDecNumber)));

    connect(context->dataStore->accountStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(accountsLoaded()));
    connect(&entityView.sortModel, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(expandRow(QModelIndex,int,int)));
    connect(&entityView.sortModel, SIGNAL(modelReset()), this, SLOT(modelReset()));
    if (entityView.model()->rowCount() > 0) treeView()->expandAll();

    entityView.viewHeader->setSectionHidden(model->securityColumn, !security());
    settings::restoreWindowState(SETTINGS_GROUP(security()), this, QSize{800, 600}, &entityView);

    accountStore()->load(&entityView);
    accountStore()->companyStore.load(&entityView, tr(LOADING_COMPANIES));
    auto dataStore = context->dataStore;
    dataStore->categoryStore->load(&entityView, tr(LOADING_CATEGORIES));
    dataStore->groupStore->load(&entityView, tr(LOADING_GROUPS));
    dataStore->payeeStore->load(&entityView, tr(LOADING_PAYEES));
    dataStore->securityStore->load(&entityView, tr(LOADING_SECURITIES));

    entityView.sortModel.setRecursiveFilteringEnabled(true);
    entityView.sortModel.setAutoAcceptChildRows(true);
    entityView.viewHeader->setSectionResizeMode(model->clearedColumn, QHeaderView::Fixed);
    entityView.viewHeader->resizeSection(model->clearedColumn, CLEARED_WIDTH);

    TreeView *view = static_cast<TreeView*>(entityView.itemView);
    view->setChildInheritsBackground(true);
    view->setItemsExpandable(false);
    view->setRootIsDecorated(false);

    initializeData();
}

TransactionsWindow::~TransactionsWindow() {
    settings::setLastViewedAccount(model()->accountId);
    context->transactionsWindowClosed(this);
}

TransactionTableModel *TransactionsWindow::model() const {
    return entityView.model<TransactionTableModel>();
}

void TransactionsWindow::showAccount(qlonglong accountId) {
    if (entityView.confirmLoadData()) {
        auto oldModel = model();
        oldModel->clearChanges();
        disconnect(oldModel, SIGNAL(clearedBalanceChanged(QDecNumber)), this, SLOT(clearedBalanceChanged(QDecNumber)));
        entityView.sortModel.setSourceModel(context->transactionsModel(accountId));
        context->transactionsModelRemoved(oldModel);
        connect(model(), SIGNAL(clearedBalanceChanged(QDecNumber)), this, SLOT(clearedBalanceChanged(QDecNumber)));
        initializeData();
    }
}

void TransactionsWindow::loadData() {
    if (entityView.confirmLoadData()) store()->load(&entityView, model()->accountId, true);
}

void TransactionsWindow::saveData() {
    // TODO
}

void TransactionsWindow::modelReset() {
    treeView()->expandAll();
}

void TransactionsWindow::expandRow(const QModelIndex &parent, int first, int last) {
    if (!parent.isValid()) {
        auto view = treeView();
        for (int row = first; row <= last; ++row) {
            view->expand(entityView.sortModel.index(row, 0));
        }
    }
}

TransactionStore *TransactionsWindow::store() const {
    return context->dataStore->transactionStore;
}

AccountStore *TransactionsWindow::accountStore() const {
    return context->dataStore->accountStore;
}

QString TransactionsWindow::connectionName() const {
    return context->dataStore->connectionName();
}

void TransactionsWindow::initializeData() {
    auto accountId = model()->accountId;
    if (store()->load(&entityView, accountId)) {
        model()->setRows(store()->transactionIds(accountId));
        clearedBalanceChanged(model()->clearedBalance());
    }
    if (accountStore()->contains(model()->accountId)) accountsLoaded();
}

void TransactionsWindow::accountsLoaded() {
    setWindowTitle(QString("%1 - %2[*]").arg(connectionName(), accountStore()->qualifiedName(model()->accountId, ':')));
    auto hidden = entityView.viewHeader->isSectionHidden(model()->securityColumn);
    if (hidden == security()) {
        settings::saveWindowState(SETTINGS_GROUP(!hidden), this, &entityView);
        entityView.viewHeader->setSectionHidden(model()->securityColumn, !hidden);
        settings::restoreWindowState(SETTINGS_GROUP(hidden), this, QSize{800, 600}, &entityView);
    }
}

void TransactionsWindow::newWindow() {
    context->showTransactions(model()->accountId);
}

void TransactionsWindow::clearedBalanceChanged(const QDecNumber &balance) {
    clearedBalance->setText(tr("<b>Cleared Balance:</b> %1").arg(dollarFormat(balance)));
}

const char *TransactionsWindow::settingsGroup() const {
    return SETTINGS_GROUP(security());
}

TreeView *TransactionsWindow::treeView() const {
    return static_cast<TreeView*>(entityView.itemView);
}

bool TransactionsWindow::security() const {
    auto account = accountStore()->value(model()->accountId);
    return account && account->security();
}
