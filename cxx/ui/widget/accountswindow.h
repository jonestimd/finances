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
    StatusBar statusBar;
    AccountTableModel model;
    TableSort tableSort;
    CompaniesWindow *companiesDialog;

public:
    AccountsWindow(DataStore *dataStore);
    ~AccountsWindow();

public Q_SLOTS:
    void setCompanies(QList<Company*> companies);
    void setAccounts(QList<Account*> accounts);
    void showCompanies();
    // void hideCompanies();

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // ACCOUNTSWINDOW_H
