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

public:
    PayeesWindow(DataStore *dataStore);

public Q_SLOTS:
    void loadPayees();
    void savePayees();
    void setPayees(const QList<qlonglong> payeeIds);

    // QWidget interface
protected:
    void closeEvent(QCloseEvent *event);
    void keyPressEvent(QKeyEvent *event);
};
#endif // PAYEESWINDOW_H
