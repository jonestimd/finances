#include "accountswindow.h"
#include "filterinput.h"
#include "styleproxy.h"
#include "tableitemdelegate.h"
#include <QtSql>
#include <QtWidgets>
#include <QtConcurrent>

AccountsWindow::AccountsWindow(Finances::App *app, DataStore *dataStore)
    : QMainWindow()
    , app{app}
    , model{new AccountTableModel(this)}
    , table{new QTableView(this)}
    , statusBar{new StatusBar(this)}
{
    setCentralWidget(table);
    setStatusBar(statusBar);
    setWindowTitle(tr("Finances (Accounts)"));
    // QMetaObject::connectSlotsByName(this);

    auto toolbar = new QToolBar(this);
    toolbar->setMovable(false);
    addToolBar(toolbar);

    connect(dataStore, SIGNAL(companiesLoaded(QList<Company*>)), this, SLOT(setCompanies(QList<Company*>)));
    connect(dataStore, SIGNAL(accountsLoaded(QList<Account*>)), this, SLOT(setAccounts(QList<Account*>)));
    if (dataStore->loadAccounts()) model->setRows(dataStore->accounts());
    else statusBar->addMessage(tr("Loading accounts..."));
    if (dataStore->loadCompanies()) model->setCompanies(dataStore->companies());
    else statusBar->addMessage(tr("Loading companies..."));

    QSortFilterProxyModel *sortModel = new QSortFilterProxyModel(this);
    sortModel->setSourceModel(model);
    sortModel->setSortRole(Finances::SortRole);
    sortModel->setFilterKeyColumn(-1);
    filterInput = new FilterInput("Account filter", toolbar, sortModel);

    table->setModel(sortModel);
    table->setStyle(new StyleProxy(table));
    table->setItemDelegate(new TableItemDelegate(table));

    if (app->settings->contains("accounts/geometry")) {
        restoreGeometry(app->settings->value("accounts/geometry").toByteArray());
    }
    else resize(800, 600);

    table->resizeColumnsToContents();
    table->setAlternatingRowColors(true);
    table->setSortingEnabled(true);

    auto header = table->horizontalHeader();
    header->setStretchLastSection(true);
    header->setSectionsMovable(true);

    header->setSortIndicatorShown(true);
    auto sortColumn = app->settings->value("accounts/sort.column", tr("Name")).toString();
    auto sortOrder = app->settings->value("accounts/sort.order", 0).toInt();
    header->setSortIndicator(model->columnIndex(sortColumn), static_cast<Qt::SortOrder>(sortOrder));

    for (int section = 0; section < header->count(); ++section) {
        bool ok;
        auto name = model->headerData(section, Qt::Horizontal, Qt::DisplayRole).toString();
        auto width = app->settings->value("accounts.columns/" + name + ".width").toInt(&ok);
        if (ok) header->resizeSection(section, width);
        auto pos = app->settings->value("accounts.columns/" + name + ".pos").toInt(&ok);
        if (ok) header->moveSection(header->visualIndex(section), pos);
    }

    QTimer::singleShot(0, this, [this] { filterInput->setFocus(); });
}

AccountsWindow::~AccountsWindow() {}

void AccountsWindow::setCompanies(QList<Company*> companies) {
    model->setCompanies(companies);
    statusBar->removeMessage(tr("Loading companies..."));
}

void AccountsWindow::setAccounts(QList<Account *> accounts) {
    model->setRows(accounts);
    statusBar->removeMessage(tr("Loading accounts..."));
}

void AccountsWindow::closeEvent(QCloseEvent *event) {
    auto model = table->model();
    auto settings = app->settings;
    settings->beginGroup("accounts");
    settings->setValue("geometry", saveGeometry());
    settings->setValue("sort.column", model->headerData(table->horizontalHeader()->sortIndicatorSection(), Qt::Horizontal));
    settings->setValue("sort.order", table->horizontalHeader()->sortIndicatorOrder());
    settings->endGroup();

    settings->beginGroup("accounts.columns");
    for (int section = 0; section < table->horizontalHeader()->count(); ++section) {
        auto name = model->headerData(section, Qt::Horizontal).toString();
        auto width = table->horizontalHeader()->sectionSize(section);
        settings->setValue(name + ".width", width);
        settings->setValue(name + ".pos", table->horizontalHeader()->visualIndex(section));
    }
    settings->endGroup();
    QMainWindow::closeEvent(event);
}

void AccountsWindow::keyPressEvent(QKeyEvent *event) {
    if (event->matches(QKeySequence::Find) && !filterInput->hasFocus()) {
        filterInput->setFocus();
        return;
    }
    QMainWindow::keyPressEvent(event);
}
