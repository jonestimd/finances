#ifndef PAYEESWINDOW_H
#define PAYEESWINDOW_H

#include "../model/datastore.h"
#include "../model/payeetablemodel.h"
#include "entityview.h"
#include "statuswindow.h"
#include <QMainWindow>
#include <QTableView>

class PayeesWindow : public StatusWindow {
    Q_OBJECT
    PayeeStore *store;
    PayeeTableModel model;
    QTableView *itemView{new QTableView(this)};
    EntityView tableSort;
    QAction *mergeAction;

public:
    PayeesWindow(DataStore *dataStore);

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
