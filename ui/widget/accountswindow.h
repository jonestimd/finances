#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../finances.h"
#include "../model/datastore.h"
#include "../model/accounttablemodel.h"
#include "../widget/statusbar.h"
#include <QMainWindow>
#include <QTableView>

QT_BEGIN_NAMESPACE
namespace Ui {
    class AccountsWindow;
}
QT_END_NAMESPACE

class AccountsWindow : public QMainWindow {
    Q_OBJECT
    Finances::App *app;
    QTableView *table;
    QLineEdit *filterInput;
    StatusBar *statusBar;
    AccountTableModel *model;

public:
    AccountsWindow(Finances::App *app, DataStore *dataStore);
    ~AccountsWindow();

public Q_SLOTS:
    void setCompanies(QList<Company*> companies);
    void setAccounts(QList<Account*> accounts);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // ACCOUNTSWINDOW_H
