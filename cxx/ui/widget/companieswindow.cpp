#include "companieswindow.h"

#include "dialog.h"
#include "statusmessage.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QPushButton>
#include <QTimer>

#define SETTINGS_GROUP "companies"

CompaniesWindow::CompaniesWindow(QMainWindow *parent, DataStore *dataStore)
    : EntityDialog(parent, tr("Company"), SETTINGS_GROUP, new CompanyTableModel(&dataStore->accountStore->companyStore),
        new QTableView(), &dataStore->messageStore)
    , store{&dataStore->accountStore->companyStore}
{
    setWindowTitle(tr("Companies[*]"));

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setCompanies(QList<domain_id>)));

    if (store->load(&entityView, tr(LOADING_COMPANIES))) {
        model()->setRows(store->ids());
    }
}

void CompaniesWindow::loadData() {
    if (!dialog::confirmDiscardChanges(this, model())) return;
    entityView.itemView->setEnabled(false);
    store->load(&entityView, tr(LOADING_COMPANIES), true);
}

void CompaniesWindow::saveData() {
    store->update(this, model(), tr("Saving companies..."));
}

void CompaniesWindow::setCompanies(const QList<domain_id> companyIds) {
    model()->setRows(companyIds);
}

bool CompaniesWindow::confirmDelete(const QSet<const QModelIndex> indexes) {
    QStringList nonEmpty;
    for (auto i : indexes) {
        if (model()->getRow(i)->accounts > 0) nonEmpty.append(model()->getRow(i)->name);
    }
    // delete is disabled for non-empty company, so the dialog should never be displayed
    return dialog::confirmDelete(this, tr("Confirm delete companies"),
            tr("The following companies have accounts.  "
            "The accounts will remain but will no longer be associated with a company.  "
            "Do you want to delete these companies?" DIALOG_ITEM_SEPARATOR "%1"), nonEmpty);
}
