#ifndef TRANSACTIONSWINDOW_H
#define TRANSACTIONSWINDOW_H

#include "appwindow.h"
#include "treeview.h"
#include "ui/store/transactionstore.h"
#include "ui/model/transactiontablemodel.h"
#include <QTreeView>

class UiContext;

class TransactionsWindow : public EntityWindow<TransactionTableModel> {
    Q_OBJECT
    UiContext* const context;
    QLabel* const clearedBalance{new QLabel()};
    QAction* moveAction;
    QAction* searchAction;
    QAction* editLotsAction;

public:
    TransactionsWindow(UiContext* context, TransactionTableModel* model, bool initializeModel = true);
    ~TransactionsWindow();

    void showAccount(domain_id accountId);

    Q_INVOKABLE void loadData() override;
    Q_INVOKABLE void saveData() override;

    void select(domain_id transactionId);

public Q_SLOTS:
    void modelReset();
    void expandRow(const QModelIndex& parent, int first, int last);
    void selectionChanged(const QModelIndex &current);
    void showRecentsMenu(const QList<PendingTransaction*> transactions); // clazy:exclude=fully-qualified-moc-types
    void showMoveDialog();
    void showSearchDialog();
    void showEditLotsDialog();

private:
    TransactionStore* store() const;
    AccountStore* accountStore() const;
    QString connectionName() const;

    void connectModel(TransactionTableModel* model);
    void initializeData();

    inline TreeView* treeView() const;
    bool isSecurity() const;

    void scrollTo(const QModelIndex& index);

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
