#include "accountswindow.h"
#include "settings.h"

#define LOADING_ACCOUNTS "Loading accounts..."
#define SAVING_ACCOUNTS "Saving accounts..."
#define LOADING_COMPANIES "Loading companies..."
#define SAVING_COMPANY "Saving company..."
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
{
    entityView.addActions({
        iconAction(FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this, SLOT(showCompanies())),
        iconAction(FontIcon::Person, tr("Payees"), tr("alt+p", "payees"), this, SLOT(showPayees())),
        iconAction(FontIcon::Category, tr("Categories"), tr("alt+k", "categories"), this, SLOT(showCategories())),
        iconAction(FontIcon::Workspaces, tr("Groups"), tr("alt+g", "groups"), this, SLOT(showGroups())),
        iconAction(FontIcon::AreaChart, tr("Securities"), tr("alt+s", "securities"), this, SLOT(showSecurities())),
    });
    setWindowTitle(tr("%1 - Accounts[*]").arg(dataStore->connectionName()));

    auto companyStore = dataStore->accountStore->companyStore;
    connect(dataStore->accountStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setAccounts(QList<qlonglong>)));
    connect(companyStore, SIGNAL(valuesLoaded(QList<qlonglong>)), this, SLOT(setCompanies(QList<qlonglong>)));

    if (dataStore->accountStore->load(this)) model()->setRows(dataStore->accountStore->ids());
    else disableUi(tr(LOADING_ACCOUNTS));
    if (!companyStore->load(this)) disableUi(tr(LOADING_COMPANIES));

    settings::restoreWindowState(SETTINGS_GROUP, this, QSize{800, 600}, &entityView);
}

AccountsWindow::~AccountsWindow() {
    if (companiesDialog) delete companiesDialog;
    if (payeesWindow) delete payeesWindow;
    if (categoriesWindow) delete categoriesWindow;
    if (groupsWindow) delete groupsWindow;
    if (securitiesWindow) delete securitiesWindow;
}

AccountTableModel *AccountsWindow::model() {
    return static_cast<AccountTableModel*>(entityView.model);
}

void AccountsWindow::loadData() {
    if (entityView.confirmLoadData(tr(LOADING_ACCOUNTS))) dataStore->accountStore->load(this, true);
}

void AccountsWindow::saveData() {
    disableUi(tr(SAVING_ACCOUNTS));
    dataStore->accountStore->update(this, model());
}

void AccountsWindow::setCompanies(const QList<qlonglong> companyIds) {
    removeMessage(tr(LOADING_COMPANIES));
    model()->companiesLoaded(companyIds);
}

void AccountsWindow::setAccounts(const QList<qlonglong> accountIds) {
    model()->setRows(accountIds);
    removeMessage(tr(LOADING_ACCOUNTS));
    removeMessage(tr(SAVING_ACCOUNTS));
    entityView.itemView->setEnabled(true);
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

void AccountsWindow::addCompany(const QString &name) {
    disableUi(tr(SAVING_COMPANY));
    dataStore->accountStore->companyStore->addCompany(this, name, "newCompany");
}

void AccountsWindow::newCompany(const Company *company) {
    if (company) {
        auto index = entityView.selectedIndex();
        model()->setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
    }
    removeMessage(tr(SAVING_COMPANY));
    entityView.itemView->setEnabled(true);
    entityView.itemView->setFocus(Qt::ActiveWindowFocusReason);
}
