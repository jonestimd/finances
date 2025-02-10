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

    TransactionTableModel *model() const;

    void showAccount(qlonglong accountId);

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex &parent, int first, int last);

private:
    TransactionStore *store() const;
    AccountStore *accountStore() const;
    QString connectionName() const;

    void initializeData();

    inline TreeView *treeView() const;
    bool security() const;

private Q_SLOTS:
    void accountsLoaded();
    void newWindow();

protected:
    const char *settingsGroup() const override;
};

#endif // TRANSACTIONSWINDOW_H
