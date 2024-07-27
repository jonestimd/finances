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

AccountsWindow::AccountsWindow(DataStore *dataStore)
    : QMainWindow()
    , dataStore{dataStore}
    , model(dataStore, this, std::bind(&AccountsWindow::addCompany, this, _1))
    , statusBar{this}
    , tableSort{this, &model, tr("Account filter"), tr("Name"), &statusBar}
{
    setCentralWidget(&tableSort.table);
    setStatusBar(&statusBar);
    setWindowTitle(tr("Finances - Accounts[*]"));
    // QMetaObject::connectSlotsByName(this);

    auto toolbar = new QToolBar(this);
    toolbar->setMovable(false);
    addToolBar(toolbar);
    toolbar->addAction(tableSort.addAction(tr("Add account")));
    toolbar->addAction(tableSort.deleteAction(tr("Delete account"), [&](int rowIndex) {
        return model.row(rowIndex)->transactions.toInt() == 0;
    }));
    toolbar->addAction(tableSort.undoAction());

    saveAction = finances::iconAction(finances::Save, tr("Save"), QKeySequence::Save, this, SLOT(saveAccounts()), false);
    toolbar->addAction(saveAction);

    auto reloadAction = finances::iconAction(finances::Refresh, tr("Reload"), QKeySequence::Refresh, this, SLOT(loadAccounts()));
    toolbar->addAction(reloadAction);

    toolbar->addSeparator();
    auto companiesAction = finances::iconAction(finances::FontIcon::AccountBalance, tr("Companies"), tr("alt+c", "companies"), this);
    toolbar->addAction(companiesAction);
    connect(companiesAction, SIGNAL(triggered(bool)), this, SLOT(showCompanies()));

    connect(dataStore, SIGNAL(companiesLoaded(QHash<qlonglong,const Company*>)), this, SLOT(setCompanies(QHash<qlonglong,const Company*>)));
    connect(dataStore, SIGNAL(accountsLoaded(QHash<qlonglong,const Account*>)), this, SLOT(setAccounts(QHash<qlonglong,const Account*>)));
    connect(&model, SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));

    if (dataStore->loadAccounts(this)) model.setRows(dataStore->accounts().values());
    else statusBar.addMessage(tr(LOADING_ACCOUNTS));
    if (!dataStore->loadCompanies(this)) statusBar.addMessage(tr(LOADING_COMPANIES));

    toolbar->addWidget(&tableSort.filterInput);

    tableSort.enableColumnResize();

    settings::restoreWindowState("accounts", this, QSize{800, 600}, &tableSort);
}

AccountsWindow::~AccountsWindow() {
    if (companiesDialog) {
        delete companiesDialog;
    }
}

void AccountsWindow::dataChanged() {
    saveAction->setEnabled(model.hasUnsavedChanges() && model.isValid());
    setWindowModified(model.hasUnsavedChanges());
}

void AccountsWindow::loadAccounts() {
    if (!dialog::confirmDiscardChanges(this, &model)) return;
    tableSort.table.setEnabled(false); // TODO save/restore selection
    statusBar.addMessage(tr(LOADING_ACCOUNTS));
    dataStore->loadAccounts(this, true);
}

void AccountsWindow::saveAccounts() {
    statusBar.addMessage(tr(SAVING_ACCOUNTS));
    tableSort.table.setEnabled(false);
    dataStore->updateAccounts(this, model.unsavedChanges(), model.unsavedAdds(), model.unsavedDeletes());
}

void AccountsWindow::setCompanies(const QHash<qlonglong, const Company*> companies) {
    statusBar.removeMessage(tr(LOADING_COMPANIES));
}

void AccountsWindow::setAccounts(const QHash<qlonglong, const Account *> accounts) {
    model.setRows(accounts.values());
    statusBar.removeMessage(tr(LOADING_ACCOUNTS));
    statusBar.removeMessage(tr(SAVING_ACCOUNTS));
    tableSort.table.setEnabled(true);
}

void AccountsWindow::showCompanies() {
    if (!companiesDialog) {
        companiesDialog = new CompaniesWindow(this, dataStore);
        companiesDialog->setWindowModality(Qt::WindowModal);
    }
    companiesDialog->show();
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
    statusBar.addMessage(tr(SAVING_COMPANY));
    tableSort.table.setEnabled(false);
    dataStore->addCompany(this, name, [=, this](const Company* company) {
        if (company) {
            model.setData(index, QVariant::fromValue(static_cast<const NamedEntity*>(company)), Qt::EditRole);
        }
        statusBar.removeMessage(tr(SAVING_COMPANY));
        tableSort.table.setEnabled(true);
        tableSort.table.setFocus(Qt::ActiveWindowFocusReason);
    });
}
