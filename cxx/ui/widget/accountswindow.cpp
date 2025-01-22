#include "accountswindow.h"
#include "settings.h"
#include "statusmessage.h"

#define SETTINGS_GROUP "accounts"

using namespace std::placeholders;
using namespace finances;

AccountsWindow::AccountsWindow(DataStore *dataStore)
    : AppWindow{
        tr("Account"),
        new AccountTableModel(dataStore->accountStore, std::bind(&AccountsWindow::addCompany, this, _1)),
        new QTableView(),
        SETTINGS_GROUP
    }
    , dataStore{dataStore}
    , showAccount{iconAction(FontIcon::Table, tr("Transactions"), tr("alt+t", "transactions"), this, SLOT(showTransactions()), false)}
{
    entityView.addActions({
        iconAction(FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this, SLOT(showCompanies())),
        iconAction(FontIcon::Person, tr("Payees"), tr("alt+p", "payees"), this, SLOT(showPayees())),
        iconAction(FontIcon::Category, tr("Categories"), tr("alt+k", "categories"), this, SLOT(showCategories())),
        iconAction(FontIcon::Workspaces, tr("Groups"), tr("alt+g", "groups"), this, SLOT(showGroups())),
        iconAction(FontIcon::AreaChart, tr("Securities"), tr("alt+s", "securities"), this, SLOT(showSecurities())),
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
    if (companiesDialog) delete companiesDialog;
    if (payeesWindow) delete payeesWindow;
    if (categoriesWindow) delete categoriesWindow;
    if (groupsWindow) delete groupsWindow;
    if (securitiesWindow) delete securitiesWindow;
    if (transactionsWindow) delete transactionsWindow;
}

AccountTableModel *AccountsWindow::model() {
    return static_cast<AccountTableModel*>(entityView.model);
}

void AccountsWindow::loadData() {
    if (entityView.confirmLoadData()) dataStore->accountStore->load(&entityView, true);
}

void AccountsWindow::saveData() {
    entityView.disableUi(tr(SAVING_ACCOUNTS));
    dataStore->accountStore->update(this, model());
}

void AccountsWindow::setCompanies(const QList<qlonglong> companyIds) {
    entityView.removeMessage(tr(LOADING_COMPANIES));
    model()->companiesLoaded(companyIds);
}

void AccountsWindow::setAccounts(const QList<qlonglong> accountIds) {
    model()->setRows(accountIds);
    entityView.removeMessage(tr(LOADING_ACCOUNTS));
    entityView.removeMessage(tr(SAVING_ACCOUNTS));
}

void AccountsWindow::showCompanies() {
    if (!companiesDialog) {
        companiesDialog = new CompaniesWindow(this, dataStore);
        companiesDialog->setWindowModality(Qt::WindowModal);
    }
    companiesDialog->show();
}

void AccountsWindow::showPayees() {
    if (!payeesWindow) payeesWindow = new PayeesWindow(dataStore);
    payeesWindow->show();
}

void AccountsWindow::showCategories() {
    if (!categoriesWindow) categoriesWindow = new CategoriesWindow(dataStore);
    categoriesWindow->show();
}

void AccountsWindow::showGroups() {
    if (!groupsWindow) groupsWindow = new GroupsWindow(dataStore);
    groupsWindow->show();
}

void AccountsWindow::showSecurities() {
    if (!securitiesWindow) securitiesWindow = new SecuritiesWindow(dataStore);
    securitiesWindow->show();
}

void AccountsWindow::showTransactions() {
    if (entityView.selectedIndex().isValid()) {
        auto accountId = model()->getRow(entityView.selectedIndex())->id.toLongLong();
        if (!transactionsWindow) {
            transactionsWindow = new TransactionsWindow(dataStore, accountId);
            transactionsWindow->show();
        }
        // else transactionsWindow->l
    }
}

void AccountsWindow::selectionChanged() {
    showAccount->setEnabled(entityView.selectedIndex().isValid());
}

void AccountsWindow::addCompany(const QString &name) {
    entityView.disableUi(tr(SAVING_COMPANY));
    dataStore->accountStore->companyStore.addCompany(this, name, "newCompany");
}

void AccountsWindow::newCompany(const Company *company) {
    if (company) {
        auto index = entityView.selectedIndex();
        model()->setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
    }
    entityView.removeMessage(tr(SAVING_COMPANY));
    // entityView.itemView->setEnabled(true);
    entityView.itemView->setFocus(Qt::ActiveWindowFocusReason);
}
