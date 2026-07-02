#include "payeeswindow.h"
#include "settings.h"
#include "entityselectiondialog.h"
#include "settings.h"
#include "statusmessage.h"
#include "ui/model/comboboxmodel.h"

#define SETTINGS_GROUP "payees"

PayeesWindow::PayeesWindow(DataStore *dataStore)
    : AppWindow{tr("Payee"), new PayeeTableModel(dataStore->payeeStore), new QTableView(), &dataStore->messageStore}
    , store{dataStore->payeeStore}
    , mergeAction{finances::iconAction(finances::MergeType, tr("Merge Payees"), tr("ctrl+y", "merge payee"), this, SLOT(merge()), false)}
{
    setWindowTitle(tr("%1 - Payees[*]").arg(dataStore->connectionName()));

    entityView.insertAction(2, mergeAction);

    connect(store, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setPayees(QList<domain_id>)));
    connect(entityView.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)),
            this, SLOT(selectionChanged(QModelIndex,QModelIndex)));

    if (store->load(&entityView, tr(LOADING_PAYEES))) model()->setRows(store->ids());

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500}, &entityView);
}

PayeesWindow::~PayeesWindow() {
    delete model();
}

PayeeTableModel *PayeesWindow::model() const {
    return entityView.model<PayeeTableModel>();
}

void PayeesWindow::loadData() {
    if (entityView.confirmLoadData()) store->load(&entityView, tr(LOADING_PAYEES), true);
}

void PayeesWindow::saveData() {
    store->update(this, model(), tr(SAVING_PAYEES));
}

void PayeesWindow::setPayees(const QList<domain_id> payeeIds) {
    model()->setRows(payeeIds);
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
        if (selectedId.has_value()) store->mergePayees(this, payee, selectedId.value());
    }
}

void PayeesWindow::selectionChanged(const QModelIndex &current, const QModelIndex &previous) {
    mergeAction->setEnabled(entityView.selectedIndex().isValid());
}

const char *PayeesWindow::settingsGroup() const {
    return SETTINGS_GROUP;
}
