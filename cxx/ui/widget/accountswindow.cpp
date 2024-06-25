#include "accountswindow.h"
#include "settings.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

AccountsWindow::AccountsWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model{dataStore, this}
    , tableSort{this, &model, "Account filter", "Name"}
    , statusBar{this}
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

    connect(dataStore, SIGNAL(companiesLoaded(QList<Company*>)), this, SLOT(setCompanies(QList<Company*>)));
    connect(dataStore, SIGNAL(accountsLoaded(QList<Account*>)), this, SLOT(setAccounts(QList<Account*>)));
    if (dataStore->loadAccounts()) model.setRows(dataStore->accounts());
    else statusBar.addMessage(tr("Loading accounts..."));
    if (!dataStore->loadCompanies()) statusBar.addMessage(tr("Loading companies..."));

    toolbar->addWidget(&tableSort.filterInput);

    tableSort.enableColumnResize();

    settings::restoreWindowState("accounts", this, QSize{800, 600}, &tableSort);
}

AccountsWindow::~AccountsWindow() {
    if (companiesDialog) {
        delete companiesDialog;
    }
}

void AccountsWindow::setCompanies(QList<Company*> companies) {
    statusBar.removeMessage(tr("Loading companies..."));
}

void AccountsWindow::setAccounts(QList<Account *> accounts) {
    model.setRows(accounts);
    statusBar.removeMessage(tr("Loading accounts..."));
}

void AccountsWindow::showCompanies() {
    if (!companiesDialog) companiesDialog = new CompaniesWindow(this, dataStore->companies());
    companiesDialog->show();
}

void AccountsWindow::closeEvent(QCloseEvent *event) {
    settings::saveWindowState("accounts", this, &tableSort);
    QMainWindow::closeEvent(event);
}

void AccountsWindow::keyPressEvent(QKeyEvent *event) {
    if (!tableSort.focusFilter(event)) QMainWindow::keyPressEvent(event);
}
