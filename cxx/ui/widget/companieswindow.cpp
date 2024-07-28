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
    , dataStore{dataStore}
    , model{dataStore->companies().values(), this}
    , tableSort{this, &model, tr("Company"), tr("Name"), SLOT(saveCompanies()), SLOT(loadCompanies())}
{
    setWindowTitle(tr("Companies[*]"));

    connect(dataStore, SIGNAL(companiesLoaded(QHash<qlonglong,const Company*>)),
            this, SLOT(setCompanies(QHash<qlonglong,const Company*>)));

    layout.addWidget(&tableSort.toolbar);
    layout.addWidget(&tableSort.table);
    layout.addWidget(&tableSort.statusBar);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    tableSort.setColumnResize({0});

    settings::restoreWindowState("companies", this, QSize{400, 500});
}

void CompaniesWindow::loadCompanies() {
    if (!dialog::confirmDiscardChanges(this, &model)) return;
    tableSort.table.setEnabled(false); // TODO save/restore selection
    tableSort.statusBar.showMessage(tr("Loading companies..."));
    dataStore->loadCompanies(this, true);
}

void CompaniesWindow::saveCompanies() {
    tableSort.statusBar.showMessage(tr("Saving companies..."));
    tableSort.table.setEnabled(false);
    dataStore->updateCompanies(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void CompaniesWindow::setCompanies(const QHash<qlonglong, const Company *> companies) {
    model.setRows(companies.values());
    tableSort.statusBar.showMessage(tr("Done loading"), 1500);
    tableSort.table.setEnabled(true);
}

bool CompaniesWindow::confirmDelete(const QSet<int> rowIndex) {
    QStringList nonEmpty;
    for (auto r : rowIndex) {
        if (model.row(r)->accounts.toInt() > 0) nonEmpty.append(model.row(r)->name.toString());
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
