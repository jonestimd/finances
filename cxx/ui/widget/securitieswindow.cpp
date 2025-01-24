#include "securitieswindow.h"
#include "ui/widget/dialog.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define LOADING_SECURITIES "Loading Securities..."
#define SAVING_SECURITIES "Saving Securities..."
#define SETTINGS_GROUP "securities"

using namespace std::placeholders;

SecuritiesWindow::SecuritiesWindow(DataStore *dataStore)
    : StatusWindow{}
    , store{dataStore->securityStore}
    , model{dataStore->securityStore, this}
    , tableSort{this, &model, itemView, &statusBar, tr("Securities"), tr("Name"), SLOT(saveSecurities()), SLOT(loadSecurities()),
                QList{hideZeroAction}}
{
    setCentralWidget(itemView);
    setWindowTitle(tr("%1 - Securities[*]").arg(dataStore->connectionName()));
    addToolBar(&tableSort.toolbar);

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setSecurities(QList<qlonglong>)));

    if (store->load(this)) model.setRows(store->ids());
    else statusBar.addMessage(tr(LOADING_SECURITIES));

    finances::setColumnResize(tableSort.viewHeader);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &tableSort);
}

void SecuritiesWindow::loadSecurities() {
    if (tableSort.confirmLoadData(tr(LOADING_SECURITIES))) store->load(this, true);
}

void SecuritiesWindow::saveSecurities() {
    disableUi(tr(SAVING_SECURITIES));
    store->update(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void SecuritiesWindow::setSecurities(const QList<qlonglong> ids) {
    model.setRows(ids);
    statusBar.removeMessage(tr(LOADING_SECURITIES));
    statusBar.removeMessage(tr(SAVING_SECURITIES));
    itemView->setEnabled(true);
}

void SecuritiesWindow::toggleZeroShares(bool hide) {
    if (hide) {
        tableSort.sortModel.addFilter(std::bind(&SecuritiesWindow::nonZeroShares, this, _1));
    }
    else tableSort.sortModel.clearFilters();
}

void SecuritiesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState(SETTINGS_GROUP, this, &tableSort);
}

void SecuritiesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}

bool SecuritiesWindow::nonZeroShares(const QModelIndex &sourceIndex) const {
    auto row = model.getRow(sourceIndex);
    auto shares = row->shares;
    return shares.isNull() || shares.value<QDecNumber>().toDouble() > 0;
}
