#ifndef PAYEESWINDOW_H
#define PAYEESWINDOW_H

#include "ui/model/payeetablemodel.h"
#include "ui/store/datastore.h"
#include "appwindow.h"
#include <QMainWindow>
#include <QTableView>

class PayeesWindow : public EntityWindow<> {
    Q_OBJECT
    PayeeStore *store;
    QAction *mergeAction;

public:
    PayeesWindow(DataStore *dataStore);
    ~PayeesWindow();

    PayeeTableModel *model() const;

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setPayees(const QList<domain_id> payeeIds);
    void merge();
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);
};
#endif // PAYEESWINDOW_H
