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
    }}
{
    setCentralWidget(&tableSort.table);
    setStatusBar(&tableSort.statusBar);
    setWindowTitle(tr("Finances - Accounts[*]"));
    // QMetaObject::connectSlotsByName(this);

    addToolBar(&tableSort.toolbar);

    connect(dataStore, SIGNAL(companiesLoaded(QHash<qlonglong,const Company*>)), this, SLOT(setCompanies(QHash<qlonglong,const Company*>)));
    connect(dataStore, SIGNAL(accountsLoaded(QHash<qlonglong,const Account*>)), this, SLOT(setAccounts(QHash<qlonglong,const Account*>)));

    if (dataStore->loadAccounts(this)) model.setRows(dataStore->accounts().values());
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

void AccountsWindow::setCompanies(const QHash<qlonglong, const Company*> companies) {
    tableSort.statusBar.removeMessage(tr(LOADING_COMPANIES));
}

void AccountsWindow::setAccounts(const QHash<qlonglong, const Account *> accounts) {
    model.setRows(accounts.values());
    tableSort.statusBar.removeMessage(tr(LOADING_ACCOUNTS));
    tableSort.statusBar.removeMessage(tr(SAVING_ACCOUNTS));
    tableSort.table.setEnabled(true);
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
    tableSort.table.setEnabled(false);
    dataStore->addCompany(this, name, [=, this](const Company* company) {
        if (company) {
            model.setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
        }
        tableSort.statusBar.removeMessage(tr(SAVING_COMPANY));
        tableSort.table.setEnabled(true);
        tableSort.table.setFocus(Qt::ActiveWindowFocusReason);
    });
}
