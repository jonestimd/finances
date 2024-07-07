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
    // - reload?
    // - undo?
    auto addAction = finances::iconAction(finances::AddCircle, tr("Add company"), QKeySequence::New, this);
    toolbar.addAction(addAction);
    connect(addAction, SIGNAL(triggered(bool)), this, SLOT(triggerAdd()));
    // todo
    // - scroll to new row and edit name
    // - save new rows
    saveAction = finances::iconAction(finances::Save, tr("Save"), QKeySequence::Save, this);
    saveAction->setEnabled(false);
    connect(&model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    connect(saveAction, SIGNAL(triggered(bool)), this, SLOT(saveCompanies()));
    connect(dataStore, SIGNAL(companiesLoaded(QList<Company*>)), this, SLOT(setCompanies(QList<Company*>)));
    toolbar.setMovable(false);
    toolbar.addAction(saveAction);
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

void CompaniesWindow::triggerAdd() {
    int rowIndex = model.queueAdd();
    tableSort.scrollTo(rowIndex, 0);
}

void CompaniesWindow::dataChanged() {
    saveAction->setEnabled(model.hasUnsavedChanges() && model.isValid());
    setWindowModified(model.hasUnsavedChanges());
}

void CompaniesWindow::saveCompanies() {
    statusBar.showMessage(tr("Saving companies..."));
    tableSort.table.setEnabled(false);
    dataStore->updateCompanies(this, model.unsavedChanges(), model.unsavedAdds());
}

void CompaniesWindow::setCompanies(QList<Company *> companies) {
    model.setRows(companies);
    statusBar.clearMessage();
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
