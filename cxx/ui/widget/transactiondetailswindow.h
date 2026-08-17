#ifndef TRANSACTION_DETAILS_WINDOW_H
#define TRANSACTION_DETAILS_WINDOW_H

#include "ui/store/datastore.h"
#include "ui/widget/appwindow.h"

class UiContext;

/**
 * @brief `TransactionDetailsWindow` displays transaction search results.
 */
class TransactionDetailsWindow : public ReadOnlyEntityWindow {
    Q_OBJECT
    DataStore *const dataStore;
    const QString searchText;

public:
    TransactionDetailsWindow(UiContext* context, const QString searchText);
    ~TransactionDetailsWindow();

    void loadData() override;
    void saveData() override;

private:
    inline QTableView* tableView() const {
        return static_cast<QTableView*>(entityView.itemView);
    }

protected:
    void showEvent(QShowEvent *event) override;
};

#endif // TRANSACTION_DETAILS_WINDOW_H
