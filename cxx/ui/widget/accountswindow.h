#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../model/datastore.h"
#include "../model/accounttablemodel.h"
#include "categorieswindow.h"
#include "companieswindow.h"
#include "entityview.h"
#include "payeeswindow.h"
#include "statuswindow.h"
#include <QMainWindow>
#include <QTableView>

class AccountsWindow : public StatusWindow {
    Q_OBJECT
    DataStore *dataStore;
    AccountTableModel model;
    QTableView *itemView{new QTableView(this)};
    EntityView tableSort;
    CompaniesWindow *companiesDialog{};
    PayeesWindow *payeesWindow{};
    CategoriesWindow *categoriesWindow{};

public:
    AccountsWindow(DataStore *dataStore);
    ~AccountsWindow();

public Q_SLOTS:
    void loadAccounts();
    void saveAccounts();
    void setCompanies(const QList<qlonglong> companyIds);
    void setAccounts(const QList<qlonglong> accountIds);
    void showCompanies();
    void showPayees();
    void showCategories();

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);

private:
    void addCompany(const QString &name);
    Q_INVOKABLE void newCompany(const Company *company);
};
#endif // ACCOUNTSWINDOW_H
