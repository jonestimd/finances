#include "stocksplitswindow.h"
#include "ui/widget/dialog.h"
#include "ui/widget/statusmessage.h"

#define SETTINGS_GROUP "stockSplits"

StockSplitsWindow::StockSplitsWindow(QMainWindow *parent, SecurityStore *securityStore, StatusMessageStore *messageStore, domain_id securityId)
    : EntityDialog{parent, tr("Stock Split"), SETTINGS_GROUP, new StockSplitTableModel{&securityStore->stockSplitStore, securityId}, new QTableView(), messageStore}
    , store{&securityStore->stockSplitStore}
{
    setWindowTitle(tr("%1 Splits[*]").arg(securityStore->value(securityId)->name));

    connect(&securityStore->stockSplitStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setSplits()));

    if (store->load(&entityView, tr(LOADING_STOCK_SPLITS))) {
        setSplits();
    }
}

void StockSplitsWindow::loadData() {
    if (!dialog::confirmDiscardChanges(this, model())) return;
    entityView.itemView->setEnabled(false);
    store->load(&entityView, tr(LOADING_STOCK_SPLITS), true);
}

void StockSplitsWindow::saveData() {
    store->update(this, model(), tr(SAVING_STOCK_SPLITS));
}

void StockSplitsWindow::setSplits() {
    model()->setRows();
}
