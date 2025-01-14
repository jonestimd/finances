#ifndef COMPANIESWINDOW_H
#define COMPANIESWINDOW_H

#include "../model/companytablemodel.h"
#include "../model/datastore.h"
#include "entityview.h"
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
    StatusBar statusBar{};
    DataStore *dataStore;
    CompanyTableModel model;
    EntityTable tableSort;

public:
    CompaniesWindow(QMainWindow *parent, DataStore *dataStore);

    Q_INVOKABLE void enableUi();

protected Q_SLOTS:
    void loadCompanies();
    void saveCompanies();
    void setCompanies(const QList<qlonglong> companyIds);

protected:
    bool confirmDelete(const QSet<const QModelIndex> rowIndex);

    // QWidget interface
    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // COMPANIESWINDOW_H
