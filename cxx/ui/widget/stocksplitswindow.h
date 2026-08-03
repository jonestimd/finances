#ifndef STOCK_SPLITS_WINDOW_H
#define STOCK_SPLITS_WINDOW_H

#include "appwindow.h"
#include "entityview.h"
#include "ui/model/stocksplitmodel.h"
#include "ui/store/securitystore.h"
#include <QBoxLayout>
#include <QDialog>
#include <QMainWindow>
#include <QMessageBox>
#include <QStatusBar>
#include <QTableView>

class StockSplitsWindow : public EntityDialog {
    Q_OBJECT
    StockSplitStore *store;

public:
    StockSplitsWindow(QMainWindow *parent, SecurityStore *securityStore, StatusMessageStore* messageStore, domain_id securityId);

    void loadData();
    void saveData();

protected Q_SLOTS:
    void setSplits();

protected:
    inline StockSplitTableModel* model() {
        return entityView.model<StockSplitTableModel>();
    }
};

#endif // STOCK_SPLITS_WINDOW_H
