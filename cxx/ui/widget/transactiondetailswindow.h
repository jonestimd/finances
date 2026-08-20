#ifndef TRANSACTION_DETAILS_WINDOW_H
#define TRANSACTION_DETAILS_WINDOW_H

#include "ui/model/transactiondetailtablemodel.h"
#include "ui/widget/appwindow.h"

class UiContext;

/**
 * @brief `TransactionDetailsWindow` displays transaction search results.
 */
class TransactionDetailsWindow : public ReadOnlyEntityWindow {
    Q_OBJECT
    UiContext* const context;
    const DetailSearchCriteria criteria;

public:
    TransactionDetailsWindow(UiContext* context, const DetailSearchCriteria criteria);
    ~TransactionDetailsWindow();

    void loadData() override;

    inline TransactionDetailTableModel* model() const {
        return static_cast<TransactionDetailTableModel*>(entityView.model());
    }

private:
    inline QTableView* tableView() const {
        return static_cast<QTableView*>(entityView.itemView);
    }

private slots:
    void gotoTransaction();

protected:
    void showEvent(QShowEvent *event) override;
};

#endif // TRANSACTION_DETAILS_WINDOW_H
