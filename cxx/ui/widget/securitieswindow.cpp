#include "securitieswindow.h"
#include "statusmessage.h"
#include "stocksplitswindow.h"
#include "ui/model/sortfilterproxymodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"
#include <QCloseEvent>

#define SETTINGS_GROUP "securities"

using namespace finances;

SecuritiesWindow::SecuritiesWindow(UiContext *context)
    : EntityWindow{tr("Security"), new SecurityTableModel(context->dataStore->securityStore), new QTableView(), &context->dataStore->messageStore}
    , store{context->dataStore->securityStore}
    , messageStore{&context->dataStore->messageStore}
    , transactionStore{context->dataStore->transactionStore}
{
    entityView.addActions({
        context->accountsAction(),
        context->accountSecuritiesAction(),
    });
    entityView.addActions({showSplitsAction});
    entityView.addActions({hideZeroAction});
    setWindowTitle(tr("%1 - Securities[*]").arg(context->dataStore->connectionName()));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setSecurities(QList<domain_id>)));
    connect(context->dataStore->transactionStore, SIGNAL(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)),
            this, SLOT(transactionsUpdated(QHash<domain_id,TransactionChange>,QHash<domain_id,DetailChange>)), Qt::DirectConnection);

    if (store->load(&entityView)) model()->setRows(store->ids());

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
    if (entityView.confirmLoadData()) store->load(&entityView, true);
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

void SecuritiesWindow::transactionsUpdated(const QHash<domain_id, TransactionChange> txChanges, const QHash<domain_id, DetailChange> detailChanges) {
    if (isVisible()) {
        QSet<domain_id> accountIds, securityIds;
        QList<domain_id> txIds;
        for (auto i = txChanges.begin(); i != txChanges.end(); i++) {
            i.value().appendIds(txIds, accountIds, securityIds);
        }
        for (auto i = detailChanges.begin(); i != detailChanges.end(); i++) {
            auto txId = i.value().oldDetail ? i.value().oldDetail->transactionId : i.value().newDetail->transactionId;
            if (!txIds.contains(txId) && i.value().isSecurityChange()) {
                transactionStore->value(txId)->appendIds(accountIds, securityIds);
            }
        }
        if (!securityIds.empty()) {
            store->loadSecurities(&entityView, securityIds.values());
        }
    }
}

void SecuritiesWindow::showSplits() {
    if (entityView.selectedIndex().isValid()) {
        auto securityId = model()->getRow(entityView.selectedIndex())->id.value();
        auto splitsWindow = new StockSplitsWindow{this, store, messageStore, securityId};
        splitsWindow->setAttribute(Qt::WA_DeleteOnClose, true);
        splitsWindow->setWindowModality(Qt::WindowModal);
        splitsWindow->show();
    }
}

bool SecuritiesWindow::nonZeroShares(const QModelIndex &sourceIndex) const {
    auto row = model()->getRow(sourceIndex);
    auto shares = row->shares;
    return !shares.isZero() && !shares.isNegative();
}

void SecuritiesWindow::showEvent(QShowEvent *event) {
    loadData();
    EntityWindow::showEvent(event);
}
