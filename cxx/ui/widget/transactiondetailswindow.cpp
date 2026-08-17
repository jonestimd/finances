#include "transactiondetailswindow.h"
#include "ui/model/transactiondetailtablemodel.h"
#include "ui/uicontext.h"
#include "ui/widget/settings.h"

#define SETTINGS_GROUP "transactionDetails"

TransactionDetailsWindow::TransactionDetailsWindow(UiContext* context, const QString searchText)
    : ReadOnlyEntityWindow{tr("Transaction Details"), new TransactionDetailTableModel{context->dataStore}, new QTableView, &context->dataStore->messageStore}
    , dataStore{context->dataStore}
    , searchText{searchText}
{
    // entityView.addActions({
    //     gotoTransactionAction
    // });
    setWindowTitle(tr("%1 - Transactions for \"%2\"").arg(dataStore->connectionName(), searchText));

    connect(dataStore->transactionStore, SIGNAL(showTransactions(QList<const SearchTransactionDetail*>)),
        entityView.model(), SLOT(setRows(QList<const SearchTransactionDetail*>)));

    setProperty(SETTINGS_GROUP_PROP, SETTINGS_GROUP);
    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{600, 800}, &entityView);
}

TransactionDetailsWindow::~TransactionDetailsWindow() {
    delete entityView.model();
}

void TransactionDetailsWindow::loadData() {
    dataStore->transactionStore->findTransactions(this, searchText);
}

void TransactionDetailsWindow::saveData() {}

void TransactionDetailsWindow::showEvent(QShowEvent * event) {
    loadData();
    ReadOnlyEntityWindow::showEvent(event);
}
