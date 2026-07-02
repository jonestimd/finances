#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../model/datastore.h"
#include "../model/accounttablemodel.h"
#include "appwindow.h"
#include "companieswindow.h"
#include "transactionswindow.h"
#include <QMainWindow>
#include <QTableView>

class UiContext;

class AccountsWindow : public AppWindow {
    Q_OBJECT
    UiContext *const context;
    DataStore *const dataStore;
    CompaniesWindow *companiesDialog{};
    QAction *showAccount;

public:
    AccountsWindow(UiContext *context);
    ~AccountsWindow();

    AccountTableModel *model();

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setCompanies(const QList<domain_id> companyIds);
    void setAccounts(const QList<domain_id> accountIds);
    void showCompanies();
    void showTransactions();
    void selectionChanged();

private:
    void addCompany(const QString &name);
    Q_INVOKABLE void newCompany(const Company *company);

protected:
    const char *settingsGroup() const override;
};
#endif // ACCOUNTSWINDOW_H
