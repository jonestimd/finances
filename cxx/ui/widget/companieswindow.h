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
    void dataChanged();
    void loadCompanies();
    void saveCompanies();
    void setCompanies(const QHash<qlonglong, const Company*> companies);

protected:
    bool confirmDelete(const QSet<int> rowIndex);

    // QWidget interface
    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // COMPANIESWINDOW_H
