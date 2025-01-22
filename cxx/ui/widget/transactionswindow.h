#ifndef TRANSACTIONSWINDOW_H
#define TRANSACTIONSWINDOW_H

#include "appwindow.h"
#include "treeview.h"
#include "ui/model/transactionstore.h"
#include "ui/model/transactiontablemodel.h"
#include <QTreeView>

class TransactionsWindow : public AppWindow {
    Q_OBJECT
    TransactionStore *store;
    AccountStore *accountStore;

public:
    TransactionsWindow(DataStore *dataStore, qlonglong accountId);

    TransactionTableModel *model();

    void showAccount(qlonglong accountId);

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void accountLoaded(qlonglong accountId);
    void modelReset();
    void expandRow(const QModelIndex &parent, int first, int last);

private:
    inline TreeView *treeView();
};

#endif // TRANSACTIONSWINDOW_H
