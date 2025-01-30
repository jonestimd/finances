#include "payeeswindow.h"
#include "settings.h"
#include "entityselectiondialog.h"
#include <ui/model/comboboxmodel.h>

#define LOADING_PAYEES "Loading payees..."
#define SAVING_PAYEES "Saving payees..."
#define SETTINGS_GROUP "payees"

PayeesWindow::PayeesWindow(DataStore *dataStore)
    : AppWindow{tr("Payee"), new PayeeTableModel(dataStore->payeeStore), new QTableView(), SETTINGS_GROUP}
    , store{dataStore->payeeStore}
    , mergeAction{finances::iconAction(finances::MergeType, tr("Merge Payees"), tr("ctrl+y", "merge payee"), this, SLOT(merge()), false)}
{
    setWindowTitle(tr("%1 - Payees[*]").arg(dataStore->connectionName()));

    entityView.insertAction(2, mergeAction);

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setPayees(QList<qlonglong>)));
    connect(entityView.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    if (store->load(this)) model()->setRows(store->ids());
    else disableUi(tr(LOADING_PAYEES));

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

PayeeTableModel *PayeesWindow::model() const {
    return static_cast<PayeeTableModel*>(entityView.model);
}

void PayeesWindow::loadData() {
    if (entityView.confirmLoadData(tr(LOADING_PAYEES))) store->load(this, true);
}

void PayeesWindow::saveData() {
    disableUi(tr(SAVING_PAYEES));
    store->update(this, model());
}

void PayeesWindow::setPayees(const QList<qlonglong> payeeIds) {
    model()->setRows(payeeIds);
    entityView.enableUi();
}

void PayeesWindow::merge() {
    auto payee = model()->getRow(entityView.selectedIndex());
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
    mergeAction->setEnabled(entityView.selectedIndex().isValid());
}
