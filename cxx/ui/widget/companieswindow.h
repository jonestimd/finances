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

class CompaniesWindow : public QDialog {
    Q_OBJECT
    QVBoxLayout layout;
    CompanyStore *store;
    CompanyTableModel model;
    QTableView *itemView{new QTableView(this)};
    EntityView entityView;

public:
    CompaniesWindow(QMainWindow *parent, DataStore *dataStore);

    Q_INVOKABLE void loadData();
    Q_INVOKABLE void saveData();

protected Q_SLOTS:
    void setCompanies(const QList<domain_id> companyIds);

protected:
    bool confirmDelete(const QSet<const QModelIndex> rowIndex);

    // QWidget interface
    void closeEvent(QCloseEvent *event) override;
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // COMPANIESWINDOW_H
