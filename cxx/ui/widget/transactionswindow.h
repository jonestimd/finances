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
    QLabel *const clearedBalance{new QLabel()};

public:
    TransactionsWindow(UiContext* context, TransactionTableModel* model, bool initializeModel = true);
    ~TransactionsWindow();

    TransactionTableModel* model() const;

    void showAccount(qlonglong accountId);

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex& parent, int first, int last);
    // TODO transactions/details loaded

private:
    TransactionStore* store() const;
    AccountStore* accountStore() const;
    QString connectionName() const;

    void connectModel(TransactionTableModel* model);
    void initializeData();

    inline TreeView* treeView() const;
    bool security() const;

private Q_SLOTS:
    void accountsLoaded();
    void companiesLoaded();
    void transactionsLoaded();
    void newWindow();
    void clearedBalanceChanged(const QDecNumber& balance);

protected:
    const char* settingsGroup() const override;

    virtual void keyPressEvent(QKeyEvent* event) override;
};

#endif // TRANSACTIONSWINDOW_H
