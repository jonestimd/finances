#include "accountswindow.h"
#include "filemenu.h"
#include "settings.h"
#include "ui/uicontext.h"

#include <QMenuBar>

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
    QMenuBar *menuBar = new QMenuBar();
    menuBar->addMenu(new FileMenu(this, context->dataStore->connectionSettings().configName()));
    QHBoxLayout *layout = new QHBoxLayout();
    layout->setContentsMargins(0, 0, 0, 0);
    layout->addWidget(menuBar, 0, Qt::AlignCenter);
    layout->addWidget(finances::separator());
    layout->addWidget(&entityView.toolbar, 1);
    QFrame *frame = new QFrame();
    frame->setFrameStyle(QFrame::Panel | QFrame::Raised);
    frame->setLineWidth(2);
    frame->setLayout(layout);
    setMenuWidget(frame);

    setWindowTitle(tr("%1 - Accounts[*]").arg(dataStore->connectionName()));

    auto companyStore = &dataStore->accountStore->companyStore;
    connect(dataStore->accountStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setAccounts(QList<domain_id>)));
    connect(companyStore, SIGNAL(valuesLoaded(QList<domain_id>)), this, SLOT(setCompanies(QList<domain_id>)));
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

void AccountsWindow::setCompanies(const QList<domain_id> companyIds) {
    model()->companiesLoaded(companyIds);
}

void AccountsWindow::setAccounts(const QList<domain_id> accountIds) {
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
        auto accountId = model()->getRow(entityView.selectedIndex())->id.value();
        context->showTransactions(accountId, frameGeometry());
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
