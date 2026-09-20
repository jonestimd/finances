#ifndef EDIT_LOTS_DIALOG_H
#define EDIT_LOTS_DIALOG_H

#include "appwindow.h"
#include <QBoxLayout>
#include <QDialog>
#include <QTableView>

class SecurityLot;
class SecurityLotTableModel;
class SortFilterProxyModel;
class SecurityPurchase;
class TransactionDetail;
class UiContext;

class EditLotsDialog : public EntityDialog {
    Q_OBJECT
    UiContext* const context;
    QLineEdit* purchaseSharesField;
    QLabel allocationStatus;

public:
    EditLotsDialog(QMainWindow* parent, UiContext* context, const TransactionDetail* sale);

    SecurityLotTableModel* model() const;

    void loadData() override;
    void saveData() override;

    void setRows(const QList<const SecurityPurchase*> rows, const QList<const SecurityLot*> lots);

private Q_SLOTS:
    void dataChanged();

protected:
    void showEvent(QShowEvent* event) override;
};

#endif // EDIT_LOTS_DIALOG_H