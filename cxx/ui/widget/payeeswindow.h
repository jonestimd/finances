#ifndef PAYEESWINDOW_H
#define PAYEESWINDOW_H

#include "../model/datastore.h"
#include "../model/payeetablemodel.h"
#include "appwindow.h"
#include <QMainWindow>
#include <QTableView>

class PayeesWindow : public AppWindow {
    Q_OBJECT
    PayeeStore *store;
    QAction *mergeAction;

public:
    PayeesWindow(DataStore *dataStore);

    PayeeTableModel *model() const;

    void loadData() override;
    void saveData() override;

public Q_SLOTS:
    void setPayees(const QList<qlonglong> payeeIds);
    void merge();
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);

protected:
    const char *settingsGroup() const override;
};
#endif // PAYEESWINDOW_H
