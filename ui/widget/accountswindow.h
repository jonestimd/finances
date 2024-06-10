#ifndef ACCOUNTSWINDOW_H
#define ACCOUNTSWINDOW_H

#include "../finances.h"
#include "../../database/dbcontext.h"
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

public:
    AccountsWindow(Finances::App *app, DbContext *dbContext);
    ~AccountsWindow();

private:
    Ui::AccountsWindow *ui;
    QList<Company*> companies;
    QList<Account*> accounts;

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
};
#endif // ACCOUNTSWINDOW_H
