#include "companieswindow.h"

#include "settings.h"
#include "dialog.h"
#include "statusmessage.h"
#include <QHeaderView>
#include <QKeyEvent>
#include <QPushButton>
#include <QTimer>

#define SETTINGS_GROUP "companies"

CompaniesWindow::CompaniesWindow(QMainWindow *parent, DataStore *dataStore)
    : QDialog(parent)
    , layout{this}
    , store{&dataStore->accountStore->companyStore}
    , model{&dataStore->accountStore->companyStore, this}
    , entityView{this, &model, itemView, tr("Company")}
{
    setWindowTitle(tr("Companies[*]"));

    connect(store, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setCompanies(QList<qlonglong>)));

    layout.addWidget(&entityView.toolbar);
    layout.addWidget(itemView);
    layout.addWidget(&entityView.statusBar);
    layout.setSpacing(0);
    layout.setContentsMargins(0, 0, 0, 0);

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{400, 500});
}

void CompaniesWindow::enableUi() {
    entityView.enableUi();
}

void CompaniesWindow::loadData() {
    if (!dialog::confirmDiscardChanges(this, &model)) return;
    itemView->setEnabled(false);
    store->load(&entityView, tr(LOADING_COMPANIES), true);
}

void CompaniesWindow::saveData() {
    entityView.disableUi(tr("Saving companies..."));
    itemView->setEnabled(false);
    store->update(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void CompaniesWindow::setCompanies(const QList<qlonglong> companyIds) {
    model.setRows(companyIds);
    entityView.enableUi();
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
    entityView.confirmClose(event, SETTINGS_GROUP);
}

void CompaniesWindow::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Escape && !dialog::confirmDiscardChanges(this, &model)) return;
    if (!entityView.focusFilter(event)) QDialog::keyPressEvent(event);
}
