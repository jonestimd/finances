#ifndef ACCOUNT_SECURITIES_WINDOW_H
#define ACCOUNT_SECURITIES_WINDOW_H

#include "treeview.h"
#include "ui/store/datastore.h"
#include "ui/widget/appwindow.h"

class UiContext;

/**
 * @brief `AccountSecuritiesWindow` displays a summary of securities held in each account.
 * @details The data in the window is updated when the window is shown or when transactions
 * are saved while the window is visible.
 */
class AccountSecuritiesWindow : public ReadOnlyEntityWindow {
    Q_OBJECT
    DataStore *const dataStore;

public:
    AccountSecuritiesWindow(UiContext* context);
    ~AccountSecuritiesWindow();

    virtual void loadData() override;

private:
    inline TreeView* treeView() const {
        return static_cast<TreeView*>(entityView.itemView);
    }

public Q_SLOTS:
    void modelReset();
    void transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges);

protected:
    void showEvent(QShowEvent *event) override;
};

#endif // ACCOUNT_SECURITIES_WINDOW_H