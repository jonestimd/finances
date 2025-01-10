#include "payeeswindow.h"
#include "dialog.h"
#include "settings.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

#define LOADING_PAYEES "Loading payees..."
#define SAVING_PAYEES "Saving payees..."

PayeesWindow::PayeesWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model{dataStore, this}
    , tableSort{this, &model, tr("Payee"), tr("Name"), SLOT(savePayees()), SLOT(loadPayees())}
{
    setCentralWidget(tableSort.itemView);
    setStatusBar(&tableSort.statusBar);
    setWindowTitle(tr("Finances - Payees[*]"));

    addToolBar(&tableSort.toolbar);

    connect(dataStore, SIGNAL(payeesLoaded(QList<qlonglong>)), this, SLOT(setPayees(QList<qlonglong>)));

    if (dataStore->loadPayees(this)) model.setRows(dataStore->payees()->ids());
    else tableSort.statusBar.addMessage(tr(LOADING_PAYEES));

    tableSort.setColumnResize({0});

    settings::restoreWindowState("payees", this, QSize{400, 500}, &tableSort);
}

void PayeesWindow::enableUi() {
    tableSort.enableUi();
}

void PayeesWindow::loadPayees() {
    tableSort.loadData(tr(LOADING_PAYEES), [this]() { dataStore->loadPayees(this, true); });
}

void PayeesWindow::savePayees() {
    tableSort.saveData(tr(SAVING_PAYEES), [this]() {
        dataStore->updatePayees(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
    });
}

void PayeesWindow::setPayees(const QList<qlonglong> payeeIds) {
    model.setRows(payeeIds);
    tableSort.statusBar.removeMessage(tr(LOADING_PAYEES));
    tableSort.statusBar.removeMessage(tr(SAVING_PAYEES));
    tableSort.itemView->setEnabled(true);
}

void PayeesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState("payees", this, &tableSort);
}

void PayeesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
