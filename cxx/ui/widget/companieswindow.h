#ifndef COMPANIESWINDOW_H
#define COMPANIESWINDOW_H

#include "../model/companytablemodel.h"
#include "../model/datastore.h"
#include "tablesort.h"
#include <QBoxLayout>
#include <QDialog>
#include <QMainWindow>
#include <QMessageBox>
#include <QStatusBar>
#include <QTableView>

class CompaniesWindow : public QDialog
{
    Q_OBJECT
    QVBoxLayout layout;
    QToolBar toolbar;
    DataStore *dataStore;
    CompanyTableModel model;
    TableSort tableSort;
    QAction *saveAction;
    QStatusBar statusBar;

public:
    CompaniesWindow(QMainWindow *parent, DataStore *dataStore);

protected Q_SLOTS:
    void triggerAdd();
    void dataChanged();
    void saveCompanies();
    void setCompanies(QList<Company*> companies);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // COMPANIESWINDOW_H
