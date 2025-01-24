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
#define SETTINGS_GROUP "payees"

PayeesWindow::PayeesWindow(DataStore *dataStore)
    : StatusWindow()
    , store{dataStore->payeeStore}
    , model{dataStore->payeeStore, this}
    , tableSort{this, &model, itemView, &statusBar, tr("Payee"), tr("Name"), SLOT(savePayees()), SLOT(loadPayees())}
{
    setCentralWidget(itemView);
    setWindowTitle(tr("%1 - Payees[*]").arg(dataStore->connectionName()));

    addToolBar(&tableSort.toolbar);

    mergeAction = finances::iconAction(finances::MergeType, tr("Merge Payees"), tr("ctrl+y", "merge payee"), this, SLOT(merge()));
    mergeAction->setEnabled(false);
    tableSort.toolbar.insertAction(tableSort.toolbar.actions()[2], mergeAction);

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setPayees(QList<qlonglong>)));
    connect(itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    if (store->load(this)) model.setRows(store->ids());
    else statusBar.addMessage(tr(LOADING_PAYEES));

    finances::setColumnResize(tableSort.viewHeader);

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &tableSort);
}

void PayeesWindow::loadPayees() {
    if (tableSort.confirmLoadData(tr(LOADING_PAYEES))) store->load(this, true);
}

void PayeesWindow::savePayees() {
    disableUi(tr(SAVING_PAYEES));
    store->update(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void PayeesWindow::setPayees(const QList<qlonglong> payeeIds) {
    model.setRows(payeeIds);
    statusBar.removeMessage(tr(LOADING_PAYEES));
    statusBar.removeMessage(tr(SAVING_PAYEES));
    itemView->setEnabled(true);
}

void PayeesWindow::merge() {
    auto payee = model.getRow(tableSort.selectedIndex());
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
            disableUi(tr(SAVING_PAYEES));
            store->mergePayees(this, payee, selectedId);
        }
    }
}

void PayeesWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    mergeAction->setEnabled(tableSort.selectedIndex().isValid());
}

void PayeesWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState(SETTINGS_GROUP, this, &tableSort);
}

void PayeesWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
