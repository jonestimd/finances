#include "companieswindow.h"

#include "settings.h"
#include "dialog.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QPushButton>
#include <QTimer>

CompaniesWindow::CompaniesWindow(QMainWindow *parent, DataStore *dataStore)
    : QDialog(parent)
    , layout{this}
    , store{dataStore->accountStore->companyStore}
    , model{dataStore->accountStore->companyStore, this}
    , tableSort{this, &model, itemView, &statusBar, tr("Company"), tr("Name"), SLOT(saveCompanies()), SLOT(loadCompanies())}
{
    setWindowTitle(tr("Companies[*]"));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setCompanies(QList<qlonglong>)));

    layout.addWidget(&tableSort.toolbar);
    layout.addWidget(itemView);
    layout.addWidget(&statusBar);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    tableSort.setColumnResize({0});

    settings::restoreWindowState("companies", this, QSize{400, 500});
}

void CompaniesWindow::enableUi() {
    tableSort.enableUi();
}

void CompaniesWindow::loadCompanies() {
    if (!dialog::confirmDiscardChanges(this, &model)) return;
    itemView->setEnabled(false); // TODO save/restore selection
    statusBar.showMessage(tr("Loading companies..."));
    store->load(this, true);
}

void CompaniesWindow::saveCompanies() {
    statusBar.showMessage(tr("Saving companies..."));
    itemView->setEnabled(false);
    store->update(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void CompaniesWindow::setCompanies(const QList<qlonglong> companyIds) {
    model.setRows(companyIds);
    statusBar.showMessage(tr("Done loading"), 1500);
    itemView->setEnabled(true);
}

bool CompaniesWindow::confirmDelete(const QSet<const QModelIndex> indexes) {
    QStringList nonEmpty;
    for (auto i : indexes) {
        if (model.getRow(i)->accounts.toInt() > 0) nonEmpty.append(model.getRow(i)->name.toString());
    }
    // FIXME: delete is disabled for non-empty company
    return dialog::confirmDelete(this, tr("Confirm delete companies"),
            tr("The following companies have accounts.  "
            "The accounts will remain but will no longer be associated with a company.  "
            "Do you want to delete these companies?" DIALOG_ITEM_SEPARATOR "%1"), nonEmpty);
}

void CompaniesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState("companies", this);
}

void CompaniesWindow::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Escape && !dialog::confirmDiscardChanges(this, &model)) return;
    if (!tableSort.focusFilter(event)) QDialog::keyPressEvent(event);
}
