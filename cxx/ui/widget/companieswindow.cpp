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
    , model{dataStore->companies(), this}
    , toolbar{this}
    , statusBar{this}
    , tableSort{this, &model, "Company filter", "Name", &statusBar}
{
    setWindowTitle(tr("Companies[*]"));
    // todo add buttons to toolbar
    // - remove
    // - undo?
    toolbar.addAction(tableSort.addAction("Add company"));

    saveAction = finances::iconAction(finances::Save, tr("Save"), QKeySequence::Save, this, SLOT(saveCompanies()), false);
    toolbar.addAction(saveAction);

    auto reloadAction = finances::iconAction(finances::Refresh, tr("Reload"), QKeySequence::Refresh, this, SLOT(loadCompanies()));
    toolbar.addAction(reloadAction);

    connect(&model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(dataStore, SIGNAL(companiesLoaded(QList<const Company*>)), this, SLOT(setCompanies(QList<const Company*>)));
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
    dataStore->updateCompanies(this, model.unsavedChanges(), model.unsavedAdds());
}

void CompaniesWindow::setCompanies(QList<const Company *> companies) {
    model.setRows(companies);
    statusBar.showMessage(tr("Done loading"), 1500);
    tableSort.table.setEnabled(true);
}

void CompaniesWindow::closeEvent(QCloseEvent *event) {
    if (!confirmClose(this, &model)) event->ignore();
    else settings::saveWindowState("companies", this);
}

void CompaniesWindow::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Escape && !confirmClose(this, &model)) return;
    if (!tableSort.focusFilter(event)) QDialog::keyPressEvent(event);
}
