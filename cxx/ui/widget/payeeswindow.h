#ifndef PAYEESWINDOW_H
#define PAYEESWINDOW_H

#include "../model/datastore.h"
#include "../model/payeetablemodel.h"
#include "entityview.h"
#include <QMainWindow>
#include <QTableView>

class PayeesWindow : public QMainWindow {
    Q_OBJECT
    DataStore *dataStore;
    PayeeTableModel model;
    EntityTable tableSort;
    QAction *mergeAction;

public:
    PayeesWindow(DataStore *dataStore);

    Q_INVOKABLE void enableUi();

public Q_SLOTS:
    void loadPayees();
    void savePayees();
    void setPayees(const QList<qlonglong> payeeIds);
    void merge();
    void selectionChanged(const QModelIndex &current, const QModelIndex &previous);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // PAYEESWINDOW_H
