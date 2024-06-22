#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../finances.h"
#include "../../service/servicecontext.h"
#include "../model/accounttablemodel.h"
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
    QStatusBar *statusBar;

public:
    AccountsWindow(Finances::App *app, ServiceContext *serviceContext);
    ~AccountsWindow();

private:
    QList<Company*> companies;
    QList<Account*> accounts;
    AccountTableModel *model;

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // ACCOUNTSWINDOW_H
