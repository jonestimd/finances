#include "accountsmenu.h"
#include "statusmessage.h"
#include "transactionswindow.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>
#include <QMenu>
#include <QMenuBar>

#define TRANSACTION_SETTINGS "transactions"
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

TransactionsWindow::TransactionsWindow(DataStore *dataStore, qlonglong accountId)
    : AppWindow{tr("Transaction"), new TransactionTableModel(dataStore), new TreeView(), TRANSACTION_SETTINGS}
    , store{dataStore->transactionStore}
    , accountStore{dataStore->accountStore}
{
    setWindowTitle(QString("%1 - %2[*]").arg(dataStore->connectionName(), accountStore->qualifiedName(accountId, ':')));
    QMenuBar *menuBar = new QMenuBar();
    menuBar->addMenu(new AccountsMenu(dataStore->accountStore));
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

    connect(store, SIGNAL(accountLoaded(qlonglong)), this, SLOT(accountLoaded(qlonglong)));
    connect(&entityView.sortModel, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(expandRow(QModelIndex,int,int)));
    connect(&entityView.sortModel, SIGNAL(modelReset()), this, SLOT(modelReset()));

    settings::restoreWindowState(TRANSACTION_SETTINGS, this, QSize{800, 600}, &entityView);

    dataStore->accountStore->load(&entityView);
    dataStore->accountStore->companyStore.load(&entityView, tr(LOADING_COMPANIES));
    dataStore->categoryStore->load(&entityView, tr(LOADING_CATEGORIES));
    dataStore->groupStore->load(&entityView, tr(LOADING_GROUPS));
    dataStore->payeeStore->load(&entityView, tr(LOADING_PAYEES));
    dataStore->securityStore->load(&entityView, tr(LOADING_SECURITIES));

    entityView.sortModel.setRecursiveFilteringEnabled(true);
    entityView.sortModel.setAutoAcceptChildRows(true);
    entityView.viewHeader->setSectionResizeMode(model()->clearedColumn, QHeaderView::Fixed);
    entityView.viewHeader->resizeSection(model()->clearedColumn, CLEARED_WIDTH);

    TreeView *view = static_cast<TreeView*>(entityView.itemView);
    view->setChildInheritsBackground(true);
    // view->setItemsExpandable(false);
    // view->setRootIsDecorated(false);

    showAccount(accountId);
}

TransactionTableModel *TransactionsWindow::model() {
    return static_cast<TransactionTableModel*>(entityView.model);
}

void TransactionsWindow::showAccount(qlonglong accountId) {
    model()->setAccountId(accountId);
    if (store->load(&entityView, accountId)) model()->setRows(store->transactionIds(accountId));
}

void TransactionsWindow::loadData() {
    if (entityView.confirmLoadData()) store->load(&entityView, model()->accountId(), true);
}

void TransactionsWindow::saveData() {
    // TODO
}

void TransactionsWindow::accountLoaded(qlonglong accountId) {
    if (accountId == model()->accountId()) {
        model()->setRows(store->transactionIds(accountId));
    }
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

TreeView *TransactionsWindow::treeView() {
    return static_cast<TreeView*>(entityView.itemView);
}
