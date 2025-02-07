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
class WindowAction : public QAction {
    T *window{};

public:
    WindowAction(finances::FontIcon icon, const QString &title, const QString &shortcut, WindowArgs... args)
        : QAction(finances::materialIcon(icon), title)
    {
        finances::initAction(this, icon, title, QKeySequence(shortcut));
        connect(this, &QAction::triggered, this, [=, this]() {
            if (!window) window = new T(args...);
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
    QHash<qlonglong, TransactionTableModel*> transactionModels{};
    QList<TransactionsWindow*> transactionsWindows{};

public:
    DataStore *const dataStore;

    explicit UiContext(DataStore *dataStore, QObject *parent = nullptr);
    ~UiContext();

    void start();

    QAction *accountsAction();
    QAction *payeesAction();
    QAction *categoriesAction();
    QAction *groupsAction();
    QAction *securitiesAction();

    void showTransactions(qlonglong accountId);

    Q_SLOT void transactionsWindowClosed(QObject *object);
};

#endif // UICONTEXT_H
