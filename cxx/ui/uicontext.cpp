#include "uicontext.h"
#include "ui/widget/transactiondetailswindow.h"
#include <QAbstractEventDispatcher>
#include <QThread>

class WindowMover : public QObject {
    TransactionsWindow* window;
    const QList<TransactionsWindow*> &windows;
    const QRect requestorRect;
    int waitCount{0}; // number of ignored events

    QMetaObject::Connection waitConnection;
    QMetaObject::Connection closeConnection;

public:
    WindowMover(TransactionsWindow *window, const QList<TransactionsWindow*> &windows, QRect requestorRect)
        : window{window}
        , windows{windows}
        , requestorRect{requestorRect}
    {
        auto dispatcher = QThread::currentThread()->eventDispatcher();
        waitConnection = connect(dispatcher, &QAbstractEventDispatcher::aboutToBlock, this, &WindowMover::exposeWindow);
        closeConnection = connect(window, &AppWindow::closed, this, &WindowMover::cleanup);
    }

    void exposeWindow() {
        if (window->frameGeometry() != window->geometry() || waitCount++ == 10) { // wait till window is decorated
            if (waitCount >= 10) qDebug("mover gave up waiting");
            if (windows.size() <= 1 && !requestorRect.isEmpty()) {
                if (window->frameGeometry().topLeft() == requestorRect.topLeft()) {
                    window->move(window->pos() + QPoint{10, 10});
                }
            } else {
                for (const auto w : windows) {
                    if (w != window && w->frameGeometry() == window->frameGeometry()) {
                        window->move(window->pos() + QPoint{10, 10});
                    }
                }
            }
            cleanup();
        }
    }

private:
    void cleanup() {
        disconnect(waitConnection);
        disconnect(closeConnection);
        deleteLater();
    }
};

using namespace finances;

UiContext::UiContext(DataStore *dataStore)
    : dataStore{dataStore}
    , accountsAction_(this, LibraryBooks, tr("Organize Accounts"), tr("alt+o", "accounts"), this)
    , payeesAction_(this, Person, tr("Payees"), tr("alt+p", "payees"), dataStore)
    , categoriesAction_(this, finances::Category, tr("Categories"), tr("alt+k", "categories"), dataStore)
    , groupsAction_(this, Workspaces, tr("Groups"), tr("alt+g", "groups"), dataStore)
    , securitiesAction_(this, AreaChart, tr("Securities"), tr("alt+s", "securities"), this)
    , accountSecuritiesAction_(this, materialIcon(LibraryBooks, {}, AreaChart), tr("Account Securities"), tr("ctrl+shift+s", "account securities"), this)
{}

UiContext::UiContext(const ConnectionSettings &settings) : UiContext(new DataStore(settings)) {}

UiContext::~UiContext() {
    if (openWindows) qWarning("~UiContext(): open windows? %d", openWindows);
    if (!transactionsWindows.empty()) qWarning("~UiContext(): open transaction windows? %lld", transactionsWindows.size());
    qDeleteAll(transactionsWindows);
    transactionsWindows.clear();
    qDeleteAll(transactionModels);
    transactionModels.clear();
    delete dataStore;
}

void UiContext::start(QRect requestorRect) {
    auto lastViewed = App::lastViewedAccount(dataStore->connectionSettings().configName());
    if (lastViewed.isValid()) showTransactions(lastViewed.toLongLong(), requestorRect);
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

QAction *UiContext::accountSecuritiesAction() {
    return &accountSecuritiesAction_;
}

TransactionsWindow *UiContext::showTransactions(domain_id accountId, const QRect& requestorRect) {
    bool accountLoaded = transactionModels.contains(accountId);
    auto model = transactionsModel(accountId);
    auto window = new TransactionsWindow(this, model, !accountLoaded);
    window->show();
    if (!transactionsWindows.isEmpty() || !requestorRect.isEmpty()) {
        new WindowMover(window, transactionsWindows, requestorRect);
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

void UiContext::findTransactions(const DetailSearchCriteria criteria) {
    openWindows++;
    auto resultWindow = new TransactionDetailsWindow{this, criteria};
    connect(resultWindow, SIGNAL(closed(AppWindow*)), this, SLOT(windowClosed(AppWindow*)));
    resultWindow->show();
}

void UiContext::showTransaction(domain_id accountId, domain_id transactionId, const QRect &requestorRect) {
    for (auto window : std::as_const(transactionsWindows)) {
        if (window->model()->accountId == accountId) {
            window->raise();
            window->select(transactionId);
            return;
        }
    }
    auto window = showTransactions(accountId, requestorRect);
    window->select(transactionId);
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
