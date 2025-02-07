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
    const QString connectionName;
    TransactionStore *store;
    AccountStore *accountStore;

public:
    TransactionsWindow(UiContext *context, TransactionTableModel *model);

    TransactionTableModel *model();

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex &parent, int first, int last);

private:
    Q_SLOT void accountsLoaded();
    inline TreeView *treeView();
};

#endif // TRANSACTIONSWINDOW_H
