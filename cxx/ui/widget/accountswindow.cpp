#include "accountswindow.h"
#include "settings.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

using namespace std::placeholders;

AccountsWindow::AccountsWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model(dataStore, this, std::bind(&AccountsWindow::addCompany, this, _1))
    , statusBar{this}
    , tableSort{this, &model, "Account filter", "Name", &statusBar}
{
    setCentralWidget(&tableSort.table);
    setStatusBar(&statusBar);
    setWindowTitle(tr("Finances (Accounts)"));
    // QMetaObject::connectSlotsByName(this);

    auto toolbar = new QToolBar(this);
    toolbar->setMovable(false);
    addToolBar(toolbar);
    auto companiesAction = finances::iconAction(finances::FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this);
    toolbar->addAction(companiesAction);
    connect(companiesAction, SIGNAL(triggered(bool)), this, SLOT(showCompanies()));

    connect(dataStore, SIGNAL(companiesLoaded(QHash<qlonglong,const Company*>)), this, SLOT(setCompanies(QHash<qlonglong,const Company*>)));
    connect(dataStore, SIGNAL(accountsLoaded(QHash<qlonglong,const Account*>)), this, SLOT(setAccounts(QHash<qlonglong,const Account*>)));
    if (dataStore->loadAccounts(this)) model.setRows(dataStore->accounts().values());
    else statusBar.addMessage(tr("Loading accounts..."));
    if (!dataStore->loadCompanies(this)) statusBar.addMessage(tr("Loading companies..."));

    toolbar->addWidget(&tableSort.filterInput);

    tableSort.enableColumnResize();

    settings::restoreWindowState("accounts", this, QSize{800, 600}, &tableSort);
}

AccountsWindow::~AccountsWindow() {
    if (companiesDialog) {
        delete companiesDialog;
    }
}

void AccountsWindow::setCompanies(const QHash<qlonglong, const Company*> companies) {
    statusBar.removeMessage(tr("Loading companies..."));
}

void AccountsWindow::setAccounts(const QHash<qlonglong, const Account *> accounts) {
    model.setRows(accounts.values());
    statusBar.removeMessage(tr("Loading accounts..."));
}

void AccountsWindow::showCompanies() {
    if (!companiesDialog) {
        companiesDialog = new CompaniesWindow(this, dataStore);
        companiesDialog->setWindowModality(Qt::WindowModal);
    }
    companiesDialog->show();
}

void AccountsWindow::closeEvent(QCloseEvent *event) {
    settings::saveWindowState("accounts", this, &tableSort);
    QMainWindow::closeEvent(event);
}

void AccountsWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}

void AccountsWindow::addCompany(const QString &name) {
    auto index = tableSort.selectedIndex();
    statusBar.addMessage(tr("Saving company..."));
    tableSort.table.setEnabled(false);
    dataStore->addCompany(this, name, [=, this](const Company* company) {
        if (company) {
            model.setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
        }
        statusBar.removeMessage(tr("Saving company..."));
        tableSort.table.setEnabled(true);
        tableSort.table.setFocus(Qt::ActiveWindowFocusReason);
    });
}
