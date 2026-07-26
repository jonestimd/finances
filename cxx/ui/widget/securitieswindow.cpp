#include "securitieswindow.h"
#include "statusmessage.h"
#include "ui/model/sortfilterproxymodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "securities"

SecuritiesWindow::SecuritiesWindow(UiContext *context)
    : EntityWindow{tr("Security"), new SecurityTableModel(context->dataStore->securityStore), new QTableView(), &context->dataStore->messageStore}
    , store{context->dataStore->securityStore}
{
    entityView.addActions({
        context->accountsAction(),
        context->accountSecuritiesAction(),
    });
    entityView.addActions({hideZeroAction});
    setWindowTitle(tr("%1 - Securities[*]").arg(context->dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setSecurities(QList<domain_id>)));

    if (store->load(&entityView, tr(LOADING_SECURITIES))) model()->setRows(store->ids());

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
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
        entityView.sortModel->addFilter(std::bind_front(&SecuritiesWindow::nonZeroShares, this));
    }
    else entityView.sortModel->clearFilters();
}

bool SecuritiesWindow::nonZeroShares(const QModelIndex &sourceIndex) const {
    auto row = model()->getRow(sourceIndex);
    auto shares = row->shares;
    return !shares.isZero() && !shares.isNegative();
}
