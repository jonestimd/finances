#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../model/datastore.h"
#include "../model/accounttablemodel.h"
#include "../widget/statusbar.h"
#include "companieswindow.h"
#include "tablesort.h"
#include <QMainWindow>
#include <QTableView>

QT_BEGIN_NAMESPACE
namespace Ui {
    class AccountsWindow;
}
QT_END_NAMESPACE

class AccountsWindow : public QMainWindow {
    Q_OBJECT
    DataStore *dataStore;
    QAction *saveAction;
    StatusBar statusBar;
    AccountTableModel model;
    TableSort tableSort;
    CompaniesWindow *companiesDialog;

public:
    AccountsWindow(DataStore *dataStore);
    ~AccountsWindow();

public Q_SLOTS:
    void dataChanged();
    void loadAccounts();
    void saveAccounts();
    void setCompanies(const QHash<qlonglong, const Company*> companies);
    void setAccounts(const QHash<qlonglong, const Account*> accounts);
    void showCompanies();

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);

private:
    void addCompany(const QString &name);
};
#endif // ACCOUNTSWINDOW_H
