#ifndef COMPANIESWINDOW_H
#define COMPANIESWINDOW_H

#include "../model/companytablemodel.h"
#include "tablesort.h"
#include <QBoxLayout>
#include <QDialog>
#include <QMainWindow>
#include <QTableView>

class CompaniesWindow : public QDialog
{
    QVBoxLayout layout;
    QWidget toolbar;
    QHBoxLayout toolbarLayout;
    CompanyTableModel model;
    QSortFilterProxyModel sortModel;
    TableSort tableSort;
public:
    CompaniesWindow(QMainWindow *parent, QList<Company*> companies);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // COMPANIESWINDOW_H
