#include "accountswindow.h"
#include "settings.h"
#include "statusmessage.h"
#include "ui/uicontext.h"

#define SETTINGS_GROUP "accounts"

using namespace finances;

AccountsWindow::AccountsWindow(UiContext *context)
    : AppWindow{
        tr("Account"),
        new AccountTableModel(context->dataStore->accountStore, std::bind_front(&AccountsWindow::addCompany, this)),
        new QTableView(),
        &context->dataStore->messageStore
    }
    , context{context}
    , dataStore{context->dataStore}
    , showAccount{iconAction(FontIcon::Table, tr("Transactions"), tr("alt+t", "transactions"), this, SLOT(showTransactions()), false)}
{
    entityView.addActions({
        iconAction(FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this, SLOT(showCompanies())),
        context->payeesAction(),
        context->categoriesAction(),
        context->groupsAction(),
        context->securitiesAction(),
        showAccount,
    });
    setWindowTitle(tr("%1 - Accounts[*]").arg(dataStore->connectionName()));

    auto companyStore = &dataStore->accountStore->companyStore;
    connect(dataStore->accountStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setAccounts(QList<qlonglong>)));
    connect(companyStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setCompanies(QList<qlonglong>)));
    connect(entityView.itemView->selectionModel(), SIGNAL(currentChanged(QModelIndex,QModelIndex)), this, SLOT(selectionChanged()));


    if (dataStore->accountStore->load(&entityView)) {
        model()->setRows(dataStore->accountStore->ids());
        model()->companiesLoaded(dataStore->accountStore->companyStore.ids());
    }

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

AccountsWindow::~AccountsWindow() {
    delete model();
    if (companiesDialog) delete companiesDialog;
}

AccountTableModel *AccountsWindow::model() {
    return entityView.model<AccountTableModel>();
}

void AccountsWindow::loadData() {
    if (entityView.confirmLoadData()) dataStore->accountStore->load(&entityView, true);
}

void AccountsWindow::saveData() {
    dataStore->accountStore->update(this, model());
}

void AccountsWindow::setCompanies(const QList<qlonglong> companyIds) {
    model()->companiesLoaded(companyIds);
}

void AccountsWindow::setAccounts(const QList<qlonglong> accountIds) {
    model()->setRows(accountIds);
}

void AccountsWindow::showCompanies() {
    if (!companiesDialog) {
        companiesDialog = new CompaniesWindow(this, dataStore);
        companiesDialog->setWindowModality(Qt::WindowModal);
    }
    companiesDialog->show();
}

void AccountsWindow::showTransactions() {
    if (entityView.selectedIndex().isValid()) {
        auto accountId = model()->getRow(entityView.selectedIndex())->id.toLongLong();
        context->showTransactions(accountId);
    }
}

void AccountsWindow::selectionChanged() {
    showAccount->setEnabled(entityView.selectedIndex().isValid());
}

void AccountsWindow::addCompany(const QString &name) {
    dataStore->accountStore->companyStore.addCompany(this, name, "newCompany");
}

void AccountsWindow::newCompany(const Company *company) {
    if (company) {
        auto index = entityView.selectedIndex();
        model()->setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
    }
    entityView.itemView->setFocus(Qt::ActiveWindowFocusReason);
}

const char *AccountsWindow::settingsGroup() const {
    return SETTINGS_GROUP;
}
