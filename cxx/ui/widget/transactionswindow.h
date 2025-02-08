#ifndef TRANSACTIONSWINDOW_H
#define TRANSACTIONSWINDOW_H

#include "appwindow.h"
#include "treeview.h"
#include "ui/model/transactionstore.h"
#include "ui/model/transactiontablemodel.h"
#include <QTreeView>

class UiContext;

class TransactionsWindow : public AppWindow {
    Q_OBJECT
    UiContext *const context;

public:
    TransactionsWindow(UiContext *context, TransactionTableModel *model);

    TransactionTableModel *model();

    void showAccount(qlonglong accountId);

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex &parent, int first, int last);

private:
    TransactionStore *store();
    AccountStore *accountStore();
    QString connectionName();

    void initializeData();

    Q_SLOT void accountsLoaded();
    inline TreeView *treeView();
};

#endif // TRANSACTIONSWINDOW_H
