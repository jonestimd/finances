#ifndef UICONTEXT_H
#define UICONTEXT_H

#include "ui/model/datastore.h"
#include "ui/widget/accountswindow.h"
#include "ui/widget/categorieswindow.h"
#include "ui/widget/groupswindow.h"
#include "ui/widget/payeeswindow.h"
#include "ui/widget/securitieswindow.h"
#include <QObject>

template<class T, typename... WindowArgs>
requires std::is_base_of_v<AppWindow, T>
class WindowAction : public QAction {
    T *window{};

public:
    WindowAction(QObject* context, finances::FontIcon icon, const QString &title, const QString &shortcut, WindowArgs... args)
        : QAction(finances::materialIcon(icon), title, context)
    {
        finances::initAction(this, icon, title, QKeySequence(shortcut));
        connect(this, &QAction::triggered, this, [=, this]() {
            if (!window) {
                window = new T(args...);
                connect(window, SIGNAL(opened(AppWindow*)), context, SLOT(windowOpened(AppWindow*)));
                connect(window, SIGNAL(closed(AppWindow*)), context, SLOT(windowClosed(AppWindow*)));
            }
            window->show();
        });
    }

    ~WindowAction() {
        if (window) delete window;
    }
};

class UiContext : public QObject {
    Q_OBJECT
    WindowAction<AccountsWindow, UiContext*> accountsAction_;
    WindowAction<PayeesWindow, DataStore*> payeesAction_;
    WindowAction<CategoriesWindow, DataStore*> categoriesAction_;
    WindowAction<GroupsWindow, DataStore*> groupsAction_;
    WindowAction<SecuritiesWindow, DataStore*> securitiesAction_;
    QHash<domain_id, TransactionTableModel*> transactionModels{};
    QList<TransactionsWindow*> transactionsWindows{};
    int openWindows{0};

public:
    DataStore *const dataStore;

public:
    explicit UiContext(DataStore *dataStore);
    explicit UiContext(const ConnectionSettings& settings);
    ~UiContext();

    void start();

    QAction *accountsAction();
    QAction *payeesAction();
    QAction *categoriesAction();
    QAction *groupsAction();
    QAction *securitiesAction();

    TransactionsWindow *showTransactions(domain_id accountId);
    TransactionTableModel *transactionsModel(domain_id accountId);
    int windowCount(const TransactionTableModel* model);

    /**
     * @brief transactionsModelRemoved Signals that a window is no longer using a model.
     */
    void transactionsModelRemoved(TransactionTableModel* model);

    void transactionsWindowClosed(TransactionsWindow *window);

public slots:
    void windowOpened(AppWindow*);
    void windowClosed(AppWindow*);

private:
    void shutdownIfEmpty();
};

#endif // UICONTEXT_H
