#include "securitieswindow.h"
#include "statusmessage.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "securities"

SecuritiesWindow::SecuritiesWindow(DataStore *dataStore)
    : AppWindow{tr("Security"), new SecurityTableModel(dataStore->securityStore), new QTableView(), &dataStore->messageStore}
    , store{dataStore->securityStore}
{
    entityView.addActions({hideZeroAction});
    setWindowTitle(tr("%1 - Securities[*]").arg(dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setSecurities(QList<domain_id>)));

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
    store->update(this, model(), tr(SAVING_SECURITIES));
}

void SecuritiesWindow::setSecurities(const QList<domain_id> ids) {
    model()->setRows(ids);
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
