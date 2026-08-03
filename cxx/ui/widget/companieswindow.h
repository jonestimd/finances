#ifndef COMPANIESWINDOW_H
#define COMPANIESWINDOW_H

#include "ui/model/companytablemodel.h"
#include "ui/store/datastore.h"
#include "appwindow.h"
#include <QBoxLayout>
#include <QDialog>
#include <QMainWindow>
#include <QMessageBox>
#include <QStatusBar>
#include <QTableView>

class CompaniesWindow : public EntityDialog {
    Q_OBJECT
    CompanyStore *store;

public:
    CompaniesWindow(QMainWindow *parent, DataStore *dataStore);

    void loadData() override;
    void saveData() override;

protected Q_SLOTS:
    void setCompanies(const QList<domain_id> companyIds);

protected:
    inline CompanyTableModel* model() {
        return entityView.model<CompanyTableModel>();
    }

    bool confirmDelete(const QSet<const QModelIndex> rowIndex);
};

#endif // COMPANIESWINDOW_H
