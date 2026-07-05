#include "uicontext.h"
#include "ui/widget/settings.h"
#include <QAbstractEventDispatcher>
#include <QThread>

class WindowMover : public QObject {
    TransactionsWindow *const window;
    const QList<TransactionsWindow*> &windows;
    int waitCount{0};

public:
    QMetaObject::Connection connection;

    WindowMover(TransactionsWindow *window, const QList<TransactionsWindow*> &windows)
        : window{window}
        , windows{windows}
    {}

    void exposeWindow() {
        if (window->frameGeometry() != window->geometry() || waitCount++ == 10) { // wait till window is decorated
            for (const auto w : windows) {
                if (w != window && w->frameGeometry() == window->frameGeometry()) {
                    window->move(window->pos() + QPoint{10, 10});
                }
            }
            deleteLater();
        }
    }
};

UiContext::UiContext(DataStore *dataStore)
    : dataStore{dataStore}
    , accountsAction_(this, finances::LibraryBooks, tr("Organize Accounts"), tr("alt+o", "accounts"), this)
    , payeesAction_(this, finances::Person, tr("Payees"), tr("alt+p", "payees"), dataStore)
    , categoriesAction_(this, finances::Category, tr("Categories"), tr("alt+k", "categories"), dataStore)
    , groupsAction_(this, finances::Workspaces, tr("Groups"), tr("alt+g", "groups"), dataStore)
    , securitiesAction_(this, finances::AreaChart, tr("Securities"), tr("alt+s", "securities"), dataStore)
{}

UiContext::UiContext(const ConnectionSettings &settings)
    : UiContext(new DataStore(new ServiceContext(new ConnectionPool(settings))))
{}

UiContext::~UiContext() {
    qDeleteAll(transactionsWindows);
    transactionsWindows.clear();
    qDeleteAll(transactionModels);
    transactionModels.clear();
    delete dataStore;
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

TransactionsWindow *UiContext::showTransactions(domain_id accountId) {
    bool accountLoaded = transactionModels.contains(accountId);
    auto model = transactionsModel(accountId);
    auto window = new TransactionsWindow(this, model, !accountLoaded);
    window->show();
    if (!transactionsWindows.isEmpty()) {
        auto mover = new WindowMover(window, transactionsWindows);
        auto dispatcher = QThread::currentThread()->eventDispatcher();
        mover->connection = connect(dispatcher, &QAbstractEventDispatcher::aboutToBlock, mover, &WindowMover::exposeWindow);
    }
    transactionsWindows.append(window);
    return window;
}

TransactionTableModel *UiContext::transactionsModel(domain_id accountId) {
    auto model = transactionModels.value(accountId);
    if (!model) {
        model = new TransactionTableModel(dataStore, accountId);
        transactionModels.insert(accountId, model);
    }
    return model;
}

int UiContext::windowCount(const TransactionTableModel *model) {
    int count = 0;
    for (const auto window : std::as_const(transactionsWindows)) if (window->model() == model) count++;
    return count;
}

void UiContext::transactionsModelRemoved(TransactionTableModel *model) {
    for (auto window : std::as_const(transactionsWindows)) {
        if (window->model() == model) return;
    }
    auto accountId = model->accountId;
    delete transactionModels.take(accountId);
    dataStore->transactionStore->clearData(accountId);
}

void UiContext::transactionsWindowClosed(TransactionsWindow *window) {
    transactionsWindows.removeOne(window);
    transactionsModelRemoved(window->model());
    shutdownIfEmpty();
}

void UiContext::windowOpened(AppWindow* window) {
    openWindows++;
}

void UiContext::windowClosed(AppWindow* window) {
    openWindows--;
    shutdownIfEmpty();
}

void UiContext::shutdownIfEmpty() {
    if (!openWindows && transactionsWindows.isEmpty()) {
        dataStore->shutdown();
        deleteLater();
    }
}
