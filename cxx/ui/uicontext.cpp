#include "uicontext.h"
#include "ui/widget/settings.h"

UiContext::UiContext(DataStore *dataStore, QObject *parent)
    : QObject{parent}
    , dataStore{dataStore}
    , accountsAction_(finances::LibraryBooks, tr("&Organize Accounts"), tr("alt+o", "accounts"), this)
    , payeesAction_(finances::Person, tr("Payees"), tr("alt+p", "payees"), dataStore)
    , categoriesAction_(finances::Category, tr("Categories"), tr("alt+k", "categories"), dataStore)
    , groupsAction_(finances::Workspaces, tr("Groups"), tr("alt+g", "groups"), dataStore)
    , securitiesAction_(finances::AreaChart, tr("Categories"), tr("alt+s", "securities"), dataStore)
{}

UiContext::~UiContext() {
    qDeleteAll(transactionsWindows);
    transactionsWindows.clear();
    qDeleteAll(transactionModels);
    transactionModels.clear();
}

void UiContext::start() {
    auto lastViewed = settings::lastViewedAccount();
    if (lastViewed.isValid()) showTransactions(lastViewed.toLongLong());
    else accountsAction_.trigger();
}

QAction *UiContext::accountsAction() {
    return &accountsAction_;
}

QAction *UiContext::payeesAction() {
    return &payeesAction_;
}

QAction *UiContext::categoriesAction() {
    return &categoriesAction_;
}

QAction *UiContext::groupsAction() {
    return &groupsAction_;
}

QAction *UiContext::securitiesAction() {
    return &securitiesAction_;
}

void UiContext::showTransactions(qlonglong accountId) {
    auto model = transactionModels.value(accountId);
    if (!model) {
        model = new TransactionTableModel(dataStore, accountId);
        transactionModels.insert(accountId, model);
    }
    auto transactionsWindow = new TransactionsWindow(this, model);
    transactionsWindows.append(transactionsWindow);
    transactionsWindow->show();
    connect(transactionsWindow, SIGNAL(destroyed(QObject*)), this, SLOT(transactionsWindowClosed(QObject*)));
}

void UiContext::transactionsWindowClosed(QObject *object) {
    auto model = static_cast<TransactionsWindow*>(object)->model();
    settings::setLastViewedAccount(model->accountId);
    transactionsWindows.removeAll(object);
    for (auto window : std::as_const(transactionsWindows)) {
        if (window->model() == model) return;
    }
    delete transactionModels.take(model->accountId);
}
