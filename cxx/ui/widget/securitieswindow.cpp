#include "securitieswindow.h"
#include "statusmessage.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "securities"

SecuritiesWindow::SecuritiesWindow(DataStore *dataStore)
    : AppWindow{tr("Security"), new SecurityTableModel(dataStore->securityStore), new QTableView()}
    , store{dataStore->securityStore}
{
    entityView.addActions({hideZeroAction});
    setWindowTitle(tr("%1 - Securities[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setSecurities(QList<qlonglong>)));

    if (store->load(&entityView, tr(LOADING_SECURITIES))) model()->setRows(store->ids());

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

SecuritiesWindow::~SecuritiesWindow() {
    delete model();
}

SecurityTableModel *SecuritiesWindow::model() const {
    return entityView.model<SecurityTableModel>();
}

void SecuritiesWindow::loadData() {
    if (entityView.confirmLoadData()) store->load(&entityView, tr(LOADING_SECURITIES), true);
}

void SecuritiesWindow::saveData() {
    entityView.disableUi(tr(SAVING_SECURITIES));
    store->update(this, model());
}

void SecuritiesWindow::setSecurities(const QList<qlonglong> ids) {
    model()->setRows(ids);
    entityView.enableUi();
}

void SecuritiesWindow::toggleZeroShares(bool hide) {
    if (hide) {
        entityView.sortModel.addFilter(std::bind_front(&SecuritiesWindow::nonZeroShares, this));
    }
    else entityView.sortModel.clearFilters();
}

bool SecuritiesWindow::nonZeroShares(const QModelIndex &sourceIndex) const {
    auto row = model()->getRow(sourceIndex);
    auto shares = row->shares;
    return shares.isNull() || shares.value<QDecNumber>().toDouble() > 0;
}

const char *SecuritiesWindow::settingsGroup() const {
    return SETTINGS_GROUP;
}
