#include "securitieswindow.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define LOADING_SECURITIES "Loading Securities..."
#define SAVING_SECURITIES "Saving Securities..."
#define SETTINGS_GROUP "securities"

using namespace std::placeholders;

SecuritiesWindow::SecuritiesWindow(DataStore *dataStore)
    : AppWindow{tr("Security"), new SecurityTableModel(dataStore->securityStore), new QTableView(), SETTINGS_GROUP}
    , store{dataStore->securityStore}
{
    entityView.addActions({hideZeroAction});
    setWindowTitle(tr("%1 - Securities[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setSecurities(QList<qlonglong>)));

    if (store->load(this)) model()->setRows(store->ids());
    else disableUi(tr(LOADING_SECURITIES));

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

SecurityTableModel *SecuritiesWindow::model() const {
    return static_cast<SecurityTableModel*>(entityView.model);
}

void SecuritiesWindow::loadData() {
    if (entityView.confirmLoadData(tr(LOADING_SECURITIES))) store->load(this, true);
}

void SecuritiesWindow::saveData() {
    disableUi(tr(SAVING_SECURITIES));
    store->update(this, model()->unsavedChanges(), model()->unsavedAdds(), model()->unsavedDeletes());
}

void SecuritiesWindow::setSecurities(const QList<qlonglong> ids) {
    model()->setRows(ids);
    entityView.enableUi();
}

void SecuritiesWindow::toggleZeroShares(bool hide) {
    if (hide) {
        entityView.sortModel.addFilter(std::bind(&SecuritiesWindow::nonZeroShares, this, _1));
    }
    else entityView.sortModel.clearFilters();
}

bool SecuritiesWindow::nonZeroShares(const QModelIndex &sourceIndex) const {
    auto row = model()->getRow(sourceIndex);
    auto shares = row->shares;
    return shares.isNull() || shares.value<QDecNumber>().toDouble() > 0;
}
