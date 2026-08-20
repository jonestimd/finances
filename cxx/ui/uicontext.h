#ifndef UICONTEXT_H
#define UICONTEXT_H

#include "ui/store/datastore.h"
#include "ui/widget/accountsecuritieswindow.h"
#include "ui/widget/accountswindow.h"
#include "ui/widget/categorieswindow.h"
#include "ui/widget/groupswindow.h"
#include "ui/widget/payeeswindow.h"
#include "ui/widget/securitieswindow.h"
#include <QObject>


class UiContext : public QObject {
    Q_OBJECT

    template<class T, typename... WindowArgs>
        requires std::is_base_of_v<AppWindow, T>
    class WindowAction : public QAction {
        T *window{};

    public:
        WindowAction(UiContext* context, QIcon icon, const QString &title, const QString &shortcut, WindowArgs... args)
            : QAction(icon, title, context)
        {
            finances::initAction(this, icon, title, QKeySequence(shortcut));
            connect(this, &QAction::triggered, this, [=, this]() {
                if (!window) {
                    window = new T(args...);
                    connect(window, SIGNAL(closed(AppWindow*)), context, SLOT(windowClosed(AppWindow*)));
                }
                if (!window->isVisible()) {
                    context->windowOpened(window);
                    window->show();
                }
                window->raise();
            });
        }
        WindowAction(UiContext* context, finances::FontIcon icon, const QString &title, const QString &shortcut, WindowArgs... args)
            : WindowAction(context, finances::materialIcon(icon), title, shortcut, args...) {}

        ~WindowAction() {
            if (window) delete window;
        }
    };

    WindowAction<AccountsWindow, UiContext*> accountsAction_;
    WindowAction<PayeesWindow, DataStore*> payeesAction_;
    WindowAction<CategoriesWindow, DataStore*> categoriesAction_;
    WindowAction<GroupsWindow, DataStore*> groupsAction_;
    WindowAction<SecuritiesWindow, UiContext*> securitiesAction_;
    WindowAction<AccountSecuritiesWindow, UiContext*> accountSecuritiesAction_;
    QHash<domain_id, TransactionTableModel*> transactionModels{};
    QList<TransactionsWindow*> transactionsWindows{};
    int openWindows{0};

public:
    DataStore *const dataStore;

public:
    explicit UiContext(DataStore *dataStore);
    explicit UiContext(const ConnectionSettings& settings);
    ~UiContext();

    void start(QRect requestorRect = {});

    QAction *accountsAction();
    QAction *payeesAction();
    QAction *categoriesAction();
    QAction *groupsAction();
    QAction *securitiesAction();
    QAction *accountSecuritiesAction();

    TransactionsWindow *showTransactions(domain_id accountId, const QRect& requestorRect = {});
    TransactionTableModel *transactionsModel(domain_id accountId);
    int windowCount(const TransactionTableModel* model);
    
    void findTransactions(const DetailSearchCriteria criteria);
    void showTransaction(domain_id accountId, domain_id transactionId, const QRect& requestorRect = {});

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
