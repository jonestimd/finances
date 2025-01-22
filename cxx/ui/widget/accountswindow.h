#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../model/datastore.h"
#include "../model/accounttablemodel.h"
#include "appwindow.h"
#include "categorieswindow.h"
#include "companieswindow.h"
#include "groupswindow.h"
#include "payeeswindow.h"
#include "securitieswindow.h"
#include "transactionswindow.h"
#include <QMainWindow>
#include <QTableView>

class AccountsWindow : public AppWindow {
    Q_OBJECT
    DataStore *dataStore;
    CompaniesWindow *companiesDialog{};
    PayeesWindow *payeesWindow{};
    CategoriesWindow *categoriesWindow{};
    GroupsWindow *groupsWindow{};
    SecuritiesWindow *securitiesWindow{};
    TransactionsWindow *transactionsWindow{};
    QAction *showAccount;

public:
    AccountsWindow(DataStore *dataStore);
    ~AccountsWindow();

    AccountTableModel *model();

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setCompanies(const QList<qlonglong> companyIds);
    void setAccounts(const QList<qlonglong> accountIds);
    void showCompanies();
    void showPayees();
    void showCategories();
    void showGroups();
    void showSecurities();
    void showTransactions();
    void selectionChanged();

private:
    void addCompany(const QString &name);
    Q_INVOKABLE void newCompany(const Company *company);
};
#endif // ACCOUNTSWINDOW_H
