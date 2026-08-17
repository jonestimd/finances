#ifndef TRANSACTIONSWINDOW_H
#define TRANSACTIONSWINDOW_H

#include "appwindow.h"
#include "treeview.h"
#include "ui/store/transactionstore.h"
#include "ui/model/transactiontablemodel.h"
#include <QTreeView>

class UiContext;

class TransactionsWindow : public EntityWindow<> {
    Q_OBJECT
    UiContext* const context;
    QLabel* const clearedBalance{new QLabel()};
    QAction* moveAction;
    QAction* searchAction;

public:
    TransactionsWindow(UiContext* context, TransactionTableModel* model, bool initializeModel = true);
    ~TransactionsWindow();

    TransactionTableModel* model() const;

    void showAccount(domain_id accountId);

    Q_INVOKABLE void loadData() override;
    Q_INVOKABLE void saveData() override;

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex& parent, int first, int last);
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);
    void showRecentsMenu(const QList<PendingTransaction*> transactions); // clazy:exclude=fully-qualified-moc-types
    void showMoveDialog();
    void showSearchDialog();

private:
    TransactionStore* store() const;
    AccountStore* accountStore() const;
    QString connectionName() const;

    void connectModel(TransactionTableModel* model);
    void initializeData();

    inline TreeView* treeView() const;
    bool isSecurity() const;

private Q_SLOTS:
    void accountsLoaded();
    void companiesLoaded();
    void transactionsLoaded();
    void newWindow();
    void clearedBalanceChanged(const QDecNumber& balance);

protected:
    virtual void keyPressEvent(QKeyEvent* event) override;
};

#endif // TRANSACTIONSWINDOW_H
