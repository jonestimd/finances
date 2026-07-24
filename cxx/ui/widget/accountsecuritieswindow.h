#ifndef ACCOUNT_SECURITIES_WINDOW_H
#define ACCOUNT_SECURITIES_WINDOW_H

#include "treeview.h"
#include "ui/model/datastore.h"
#include "ui/widget/appwindow.h"

class UiContext;

class AccountSecuritiesWindow : public ReadOnlyEntityWindow {
    Q_OBJECT
    DataStore *const dataStore;

public:
    AccountSecuritiesWindow(UiContext* context);
    ~AccountSecuritiesWindow();

    virtual void loadData() override;
    virtual void saveData() override;

private:
    inline TreeView* treeView() const {
        return static_cast<TreeView*>(entityView.itemView);
    }

public Q_SLOTS:
    void modelReset();
};

#endif // ACCOUNT_SECURITIES_WINDOW_H