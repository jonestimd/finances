#include "accountsmenu.h"
#include "entityselectiondialog.h"
#include "filemenu.h"
#include "recenttxaction.h"
#include "statusmessage.h"
#include "transactionswindow.h"
#include "ui/finances.h"
#include "ui/model/formats.h"
#include "ui/model/sortfilterproxymodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>
#include <QInputDialog>
#include <QMenu>
#include <QMenuBar>
#include <QWidgetAction>

#define TRANSACTION_SETTINGS "transactions"
#define SECURITY_TRANSACTION_SETTINGS "security." TRANSACTION_SETTINGS
#define SETTINGS_GROUP(security) (security ? SECURITY_TRANSACTION_SETTINGS : TRANSACTION_SETTINGS)
#define HIDE_CLOSED_ACCOUNTS "hideClosedAccounts"
#define CLEARED_WIDTH 30

TransactionsWindow::TransactionsWindow(UiContext *context, TransactionTableModel *model, bool initializeModel)
    : EntityWindow{tr("Detail"), model, new TreeView(), &context->dataStore->messageStore}
    , context{context}
    , moveAction{finances::iconAction(finances::MoveItem, tr("Move Transaction"), tr("ctrl+m"), this, SLOT(showMoveDialog()))}
    , searchAction{finances::iconAction(finances::Search, tr("Search Transactions"), tr("ctrl+shift+f"), this, SLOT(showSearchDialog()))}
{
    setWindowTitle(QString("%1 - Transactions").arg(connectionName()));
    setAttribute(Qt::WA_DeleteOnClose, true);
    moveAction->setEnabled(false);
    entityView.insertAction(2, moveAction);
    entityView.addActions({finances::iconAction(finances::NewWindow, tr("New Window"), tr("alt+n"), this, SLOT(newWindow()))});
    entityView.addActions({
        context->accountsAction(),
        context->payeesAction(),
        context->categoriesAction(),
        context->groupsAction(),
        context->securitiesAction(),
        context->accountSecuritiesAction(),
    });
    entityView.addActions({searchAction});
    QMenuBar *menuBar = new QMenuBar();
    menuBar->addMenu(new FileMenu(this, context->dataStore->connectionSettings().configName()));
    menuBar->addMenu(new AccountsMenu(this, context));
    QHBoxLayout *layout = new QHBoxLayout();
    layout->setContentsMargins(0, 0, 0, 0);
    layout->addWidget(menuBar, 0, Qt::AlignCenter);
    layout->addWidget(finances::separator());
    layout->addWidget(&entityView.toolbar, 1);
    QFrame *frame = new QFrame();
    frame->setFrameStyle(QFrame::Panel | QFrame::Raised);
    frame->setLineWidth(2);
    frame->setLayout(layout);
    setMenuWidget(frame);

    entityView.statusBar.addPermanentWidget(clearedBalance);
    connectModel(model);

    auto dataStore = context->dataStore;
    connect(dataStore->accountStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(accountsLoaded()));
    connect(&dataStore->accountStore->companyStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(companiesLoaded()));
    connect(dataStore->transactionStore, SIGNAL(showRecents(QList<PendingTransaction*>)), this, SLOT(showRecentsMenu(QList<PendingTransaction*>)));
    connect(entityView.sortModel, SIGNAL(rowsInserted(QModelIndex,int,int)), this, SLOT(expandRow(QModelIndex,int,int)));
    connect(entityView.sortModel, SIGNAL(modelReset()), this, SLOT(modelReset()));
    connect(entityView.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(selectionChanged(QModelIndex,QModelIndex)));
    if (entityView.model()->rowCount() > 0) treeView()->expandAll();

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP(isSecurity()));
    entityView.viewHeader->setSectionHidden(model->securityColumn, !isSecurity());
    settings::restoreWindowState(SETTINGS_GROUP(isSecurity()), this, QSize{800, 600}, &entityView);

    accountStore()->load(&entityView);
    accountStore()->companyStore.load(&entityView, tr(LOADING_COMPANIES));
    dataStore->categoryStore->load(&entityView, tr(LOADING_CATEGORIES));
    dataStore->groupStore->load(&entityView, tr(LOADING_GROUPS));
    dataStore->payeeStore->load(&entityView, tr(LOADING_PAYEES));
    dataStore->securityStore->load(&entityView);

    entityView.sortModel->setRecursiveFilteringEnabled(true);
    entityView.sortModel->setAutoAcceptChildRows(true);
    entityView.viewHeader->setSectionResizeMode(model->clearedColumn, QHeaderView::Fixed);
    entityView.viewHeader->resizeSection(model->clearedColumn, CLEARED_WIDTH);

    TreeView *view = static_cast<TreeView*>(entityView.itemView);
    view->setChildInheritsBackground(true);
    view->setItemsExpandable(false);
    view->setRootIsDecorated(false);
    view->setIndentation(0);

    if (initializeModel) initializeData();
    else transactionsLoaded();
}

TransactionsWindow::~TransactionsWindow() {
    finances::App::setLastViewedAccount(model()->accountId, context->dataStore->connectionSettings().configName());
    context->transactionsWindowClosed(this);
}

TransactionTableModel *TransactionsWindow::model() const {
    return entityView.model<TransactionTableModel>();
}

void TransactionsWindow::showAccount(domain_id accountId) {
    auto oldModel = model();
    if (accountId != oldModel->accountId) {
        auto windowCount = context->windowCount(oldModel);
        if (windowCount > 1 || entityView.confirmLoadData()) {
            if (windowCount == 1) oldModel->clearChanges();
            disconnect(oldModel, SIGNAL(clearedBalanceChanged(QDecNumber)), this, SLOT(clearedBalanceChanged(QDecNumber)));
            disconnect(oldModel, SIGNAL(dataLoaded()), this, SLOT(transactionsLoaded()));
            entityView.setModel(context->transactionsModel(accountId));
            context->transactionsModelRemoved(oldModel);
            connectModel(model());
            initializeData();
        }
    }
}

void TransactionsWindow::loadData() {
    if (entityView.confirmLoadData()) store()->load(&entityView, model()->accountId, true);
}

void TransactionsWindow::saveData() {
    store()->update(this, model(), tr(SAVING_TRANSACTIONS));
}

void TransactionsWindow::modelReset() {
    treeView()->expandAll();
}

void TransactionsWindow::expandRow(const QModelIndex &parent, int first, int last) {
    if (!parent.isValid()) {
        auto view = treeView();
        for (int row = first; row <= last; ++row) {
            view->expand(entityView.sortModel->index(row, 0));
        }
    }
}

void TransactionsWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    moveAction->setEnabled(!model()->transactionHasChanges(current));
}

void TransactionsWindow::showRecentsMenu(const QList<PendingTransaction*> transactions) {
    if (isActiveWindow() && !transactions.isEmpty()) {
        QMenu popup{};
        popup.setObjectName("recents");
        for (auto transaction : transactions) {
            popup.addAction(new RecentTxAction{&popup, model(), transaction, context->dataStore});
        }
        auto cellIndex = entityView.itemView->currentIndex().siblingAtColumn(0);
        auto rect = entityView.itemView->visualRect(cellIndex);
        popup.exec(entityView.itemView->viewport()->mapToGlobal(rect.bottomLeft()));
    }
}

void TransactionsWindow::showMoveDialog() {
    QList<const NamedEntity*> options;
    QHash<domain_id, QString> disabledOptions;
    auto transaction = model()->getRow(entityView.selectedIndex());
    bool hasSecurity = transaction->securityId.has_value();
    auto accountStore = context->dataStore->accountStore;
    accountStore->forEachEntry([&](domain_id id, const Account* account) {
        auto message = tr("\"%1\" does not support security transactions").arg(account->name);
        if (account->id.value() != transaction->accountId) {
            options.append(account);
            if (hasSecurity && !account->type->security) disabledOptions.insert(account->id.value(), message);
        }
    });
    auto getName = [accountStore](const NamedEntity* entity) {
        return accountStore->qualifiedName(entity->id.value());
    };
    auto model = new ComboBoxModel(options, getName);
    EntitySelectionDialog dialog(this, model, tr("Move Transaction"), tr("Select an account"), disabledOptions);
    if (dialog.exec() == QDialog::Accepted) {
        auto selectedId = dialog.selectedId();
        if (selectedId.has_value()) context->dataStore->transactionStore->moveTransaction(this, transaction, selectedId.value());
    }
}

void TransactionsWindow::showSearchDialog() {
    auto text = QInputDialog::getText(this, tr("Enter search text"), tr("Find:"));
    if (!text.isEmpty()) context->findTransactions(text);
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

void TransactionsWindow::connectModel(TransactionTableModel *model) {
    connect(model, SIGNAL(clearedBalanceChanged(QDecNumber)), this, SLOT(clearedBalanceChanged(QDecNumber)));
    connect(model, SIGNAL(dataLoaded()), this, SLOT(transactionsLoaded()));
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
    companiesLoaded();
    auto hidden = entityView.viewHeader->isSectionHidden(model()->securityColumn);
    if (hidden == isSecurity()) {
        setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP(hidden));
        settings::saveWindowState(SETTINGS_GROUP(!hidden), this, &entityView);
        entityView.viewHeader->setSectionHidden(model()->securityColumn, !hidden);
        settings::restoreWindowState(SETTINGS_GROUP(hidden), this, QSize{800, 600}, &entityView);
    }
}

void TransactionsWindow::companiesLoaded() {
    setWindowTitle(QString("%1 - %2[*]").arg(connectionName(), accountStore()->qualifiedName(model()->accountId)));
}

void TransactionsWindow::transactionsLoaded() {
    auto m = model();
    entityView.itemView->setCurrentIndex(entityView.sortModel->mapFromSource(m->index(m->rowCount()-1, 0)));
    entityView.focusItemView();
}

void TransactionsWindow::newWindow() {
    context->showTransactions(model()->accountId);
}

void TransactionsWindow::clearedBalanceChanged(const QDecNumber &balance) {
    clearedBalance->setText(tr("<b>Cleared Balance:</b> %1").arg(dollarFormat(balance)));
}

static bool isEnter(const QKeyEvent *event) {
    auto key = event->key();
    return !(event->modifiers() & ~Qt::KeypadModifier) && (key == Qt::Key_Enter || key == Qt::Key_Return);
}

void TransactionsWindow::keyPressEvent(QKeyEvent *event) {
    if (isEnter(event) && focusWidget() == entityView.itemView) {
        auto index = entityView.sortModel->mapToSource(entityView.itemView->currentIndex());
        if (model()->transactionHasChanges(index) && model()->transactionIsValid(index)) {
            auto txRow = index.parent().isValid() ? index.parent().row() : index.row();
            store()->update(this, model(), tr(SAVING_TRANSACTION), txRow);
        }
    }
    AppWindow::keyPressEvent(event);
}

TreeView *TransactionsWindow::treeView() const {
    return static_cast<TreeView*>(entityView.itemView);
}

bool TransactionsWindow::isSecurity() const {
    auto account = accountStore()->value(model()->accountId);
    return account && account->security();
}
