#include "payeeswindow.h"
#include "dialog.h"
#include "settings.h"
#include "entityselectiondialog.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>
#include <ui/model/comboboxmodel.h>

#define LOADING_PAYEES "Loading payees..."
#define SAVING_PAYEES "Saving payees..."

PayeesWindow::PayeesWindow(DataStore *dataStore)
    : StatusWindow()
    , dataStore{dataStore}
    , model{dataStore, this}
    , tableSort{this, &model, &statusBar, tr("Payee"), tr("Name"), SLOT(savePayees()), SLOT(loadPayees())}
{
    setCentralWidget(tableSort.itemView);
    setWindowTitle(tr("%1 - Payees[*]").arg(dataStore->connectionName()));

    addToolBar(&tableSort.toolbar);

    mergeAction = finances::iconAction(finances::MergeType, tr("Merge Payees"), tr("ctrl+y", "merge payee"), this, SLOT(merge()));
    mergeAction->setEnabled(false);
    tableSort.toolbar.insertAction(tableSort.toolbar.actions()[2], mergeAction);

    connect(dataStore, SIGNAL(payeesLoaded(QList<qlonglong>)), this, SLOT(setPayees(QList<qlonglong>)));
    connect(tableSort.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    if (dataStore->loadPayees(this)) model.setRows(dataStore->payees()->ids());
    else statusBar.addMessage(tr(LOADING_PAYEES));

    tableSort.setColumnResize({0});

    settings::restoreWindowState("payees", this, QSize{400, 500}, &tableSort);
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
    statusBar.removeMessage(tr(LOADING_PAYEES));
    statusBar.removeMessage(tr(SAVING_PAYEES));
    tableSort.itemView->setEnabled(true);
}

void PayeesWindow::merge() {
    auto payee = model.getRow(tableSort.selectedIndex());
    auto store = dataStore->payees();
    QList<const NamedEntity*> options;
    for (auto id : store->ids()) {
        auto option = store->value(id);
        if (option != payee) options.append(option);
    }
    auto model = new ComboBoxModel(options, NamedEntity::getName);
    EntitySelectionDialog dialog(this, model, tr("Merge Payees"), tr("Select destination payee:"));
    auto result = dialog.exec();
    if (result == QDialog::Accepted) {
        auto selectedId = dialog.selectedId();
        if (!selectedId.isNull()) {
            tableSort.saveData(tr(SAVING_PAYEES), [this, payee, selectedId]() {
                dataStore->mergePayees(this, payee, selectedId);
            });
        }
    }
}

void PayeesWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    mergeAction->setEnabled(tableSort.selectedIndex().isValid());
}

void PayeesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState("payees", this, &tableSort);
}

void PayeesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
