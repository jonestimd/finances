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
    , toolbar{this}
    , statusBar{this}
    , tableSort{this, &model, "Company filter", "Name", &statusBar}
{
    setWindowTitle(tr("Companies[*]"));
    toolbar.addAction(tableSort.addAction("Add company"));
    toolbar.addAction(tableSort.deleteAction("Delete company", [&](int rowIndex) {
        return model.row(rowIndex)->accounts.toInt() == 0;
    }));
    toolbar.addAction(tableSort.undoAction("Undo"));

    saveAction = finances::iconAction(finances::Save, tr("Save"), QKeySequence::Save, this, SLOT(saveCompanies()), false);
    toolbar.addAction(saveAction);

    auto reloadAction = finances::iconAction(finances::Refresh, tr("Reload"), QKeySequence::Refresh, this, SLOT(loadCompanies()));
    toolbar.addAction(reloadAction);

    connect(&model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(dataStore, SIGNAL(companiesLoaded(QHash<qlonglong,const Company*>)),
            this, SLOT(setCompanies(QHash<qlonglong,const Company*>)));
    toolbar.setMovable(false);
    toolbar.addWidget(&tableSort.filterInput);

    layout.addWidget(&toolbar);
    layout.addWidget(&toolbar);
    layout.addWidget(&tableSort.table);
    layout.addWidget(&statusBar);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    tableSort.setColumnResize({0});

    settings::restoreWindowState("companies", this, QSize{400, 500});
}

void CompaniesWindow::dataChanged() {
    saveAction->setEnabled(model.hasUnsavedChanges() && model.isValid());
    setWindowModified(model.hasUnsavedChanges());
}

void CompaniesWindow::loadCompanies() {
    tableSort.table.setEnabled(false); // TODO save/restore selection
    statusBar.showMessage(tr("Loading companies..."));
    dataStore->loadCompanies(this, true);
}

void CompaniesWindow::saveCompanies() {
    statusBar.showMessage(tr("Saving companies..."));
    tableSort.table.setEnabled(false);
    dataStore->updateCompanies(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void CompaniesWindow::setCompanies(const QHash<qlonglong, const Company *> companies) {
    model.setRows(companies.values());
    statusBar.showMessage(tr("Done loading"), 1500);
    tableSort.table.setEnabled(true);
}

bool CompaniesWindow::confirmDelete(const QSet<int> rowIndex) {
    QStringList nonEmpty;
    for (auto r : rowIndex) {
        if (model.row(r)->accounts.toInt() > 0) nonEmpty.append(model.row(r)->name.toString());
    }
    return dialog::confirmDelete(this, "Confirm delete companies",
            "The following companies have accounts.  "
            "The accounts will remain but will no longer be associated with a company.  "
            "Do you want to delete these companies?" DIALOG_ITEM_SEPARATOR "%1", nonEmpty);
}

void CompaniesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmClose(this, &model)) event->ignore();
    else settings::saveWindowState("companies", this);
}

void CompaniesWindow::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Escape && !dialog::confirmClose(this, &model)) return;
    if (!tableSort.focusFilter(event)) QDialog::keyPressEvent(event);
}
