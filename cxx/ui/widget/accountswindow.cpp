#include "accountswindow.h"
#include "settings.h"
#include "ui/widget/dialog.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

#define LOADING_ACCOUNTS "Loading accounts..."
#define SAVING_ACCOUNTS "Saving accounts..."
#define LOADING_COMPANIES "Loading companies..."
#define SAVING_COMPANY "Saving company..."

using namespace std::placeholders;
using namespace finances;

AccountsWindow::AccountsWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model(dataStore, this, std::bind(&AccountsWindow::addCompany, this, _1))
    , tableSort{this, &model, tr("Account"), tr("Name"), SLOT(saveAccounts()), SLOT(loadAccounts()), QList{
        iconAction(FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this, SLOT(showCompanies())),
        iconAction(FontIcon::Person, tr("Payees"), tr("alt+p", "payees"), this, SLOT(showPayees())),
        iconAction(FontIcon::Category, tr("Categories"), tr("alt+k", "categories"), this, SLOT(showCategories())),
    }}
{
    setCentralWidget(tableSort.itemView);
    setStatusBar(&tableSort.statusBar);
    setWindowTitle(tr("Finances - Accounts[*]"));
    // QMetaObject::connectSlotsByName(this);

    addToolBar(&tableSort.toolbar);

    connect(dataStore, SIGNAL(companiesLoaded(QList<qlonglong>)), this, SLOT(setCompanies(QList<qlonglong>)));
    connect(dataStore, SIGNAL(accountsLoaded(QList<qlonglong>)), this, SLOT(setAccounts(QList<qlonglong>)));

    if (dataStore->loadAccounts(this)) model.setRows(dataStore->accounts()->ids());
    else tableSort.statusBar.addMessage(tr(LOADING_ACCOUNTS));
    if (!dataStore->loadCompanies(this)) tableSort.statusBar.addMessage(tr(LOADING_COMPANIES));

    tableSort.enableColumnResize();

    settings::restoreWindowState("accounts", this, QSize{800, 600}, &tableSort);
}

AccountsWindow::~AccountsWindow() {
    if (companiesDialog) delete companiesDialog;
    if (payeesWindow) delete payeesWindow;
}

void AccountsWindow::loadAccounts() {
    tableSort.loadData(tr(LOADING_ACCOUNTS), [this]() { dataStore->loadAccounts(this, true); });
}

void AccountsWindow::saveAccounts() {
    tableSort.saveData(tr(SAVING_ACCOUNTS), [this]() {
        dataStore->updateAccounts(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
    });
}

void AccountsWindow::setCompanies(const QList<qlonglong> companyIds) {
    tableSort.statusBar.removeMessage(tr(LOADING_COMPANIES));
}

void AccountsWindow::setAccounts(const QList<qlonglong> accountIds) {
    model.setRows(accountIds);
    tableSort.statusBar.removeMessage(tr(LOADING_ACCOUNTS));
    tableSort.statusBar.removeMessage(tr(SAVING_ACCOUNTS));
    tableSort.itemView->setEnabled(true);
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

void AccountsWindow::closeEvent(QCloseEvent *event) {
    if (!dialog::confirmDiscardChanges(this, &model)) event->ignore();
    else settings::saveWindowState("accounts", this, &tableSort);
}

void AccountsWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}

void AccountsWindow::addCompany(const QString &name) {
    auto index = tableSort.selectedIndex();
    tableSort.statusBar.addMessage(tr(SAVING_COMPANY));
    tableSort.itemView->setEnabled(false);
    dataStore->addCompany(this, name, "newCompany");
}

void AccountsWindow::newCompany(const Company *company) {
    if (company) {
        auto index = tableSort.selectedIndex();
        model.setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
    }
    tableSort.statusBar.removeMessage(tr(SAVING_COMPANY));
    tableSort.itemView->setEnabled(true);
    tableSort.itemView->setFocus(Qt::ActiveWindowFocusReason);
}
