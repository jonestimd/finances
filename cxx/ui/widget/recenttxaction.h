#ifndef RECENT_TX_ACTION_H
#define RECENT_TX_ACTION_H

#include <QWidgetAction>

class DataStore;
class PendingTransaction;
class TransactionTableModel;

class RecentTxAction : public QWidgetAction {
    Q_OBJECT
    TransactionTableModel* model;
    PendingTransaction* transaction;
public:
    RecentTxAction(QMenu* parent, TransactionTableModel* model, PendingTransaction* transaction, const DataStore* dataStore);
    ~RecentTxAction();

    /**
     * @brief eventFilter Highlights the selected menu item when the selection changes.
     */
    bool eventFilter(QObject *watched, QEvent *event) override;

private Q_SLOTS:
    void selected();

public:
    static QString label(PendingTransaction* transaction, const DataStore* dataStore);
};

#endif // RECENT_TX_ACTION_H