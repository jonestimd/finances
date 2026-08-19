#include "transactiondetailswindow.h"
#include "ui/model/transactiondetailtablemodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"

#define SETTINGS_GROUP "transactionDetails"

TransactionDetailsWindow::TransactionDetailsWindow(UiContext* context, const DetailSearchCriteria criteria)
    : ReadOnlyEntityWindow{tr("Transaction Details"), new TransactionDetailTableModel{context->dataStore}, new QTableView, &context->dataStore->messageStore}
    , context{context}
    , criteria{criteria}
{
    setAttribute(Qt::WA_DeleteOnClose);
    entityView.addActions({
        finances::iconAction(finances::Input, tr("Goto Transaction"), tr("ctrl+g"), this, SLOT(gotoTransaction()))
    });
    auto dataStore = context->dataStore;
    setWindowTitle(tr("%1 - Transactions for {%2}").arg(dataStore->connectionName(), dataStore->toString(criteria)));

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{600, 800}, &entityView);
}

TransactionDetailsWindow::~TransactionDetailsWindow() {
    delete entityView.model();
}

void TransactionDetailsWindow::loadData() {
    context->dataStore->transactionStore->findTransactions(this, criteria);
}

void TransactionDetailsWindow::saveData() {}

void TransactionDetailsWindow::gotoTransaction() {
    auto index = entityView.selectedIndex();
    if (index.isValid()) {
        auto detail = model()->getRow(index);
        context->showTransaction(detail->accountId, detail->transactionId);
    }
}

void TransactionDetailsWindow::showEvent(QShowEvent * event) {
    loadData();
    ReadOnlyEntityWindow::showEvent(event);
}
