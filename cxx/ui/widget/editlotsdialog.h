#ifndef EDIT_LOTS_DIALOG_H
#define EDIT_LOTS_DIALOG_H

#include "appwindow.h"
#include <QBoxLayout>
#include <QDialog>
#include <QTableView>

class SecurityLotTableModel;
class SortFilterProxyModel;
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

private Q_SLOTS:
    void dataChanged();

protected:
    void showEvent(QShowEvent* event) override;
};

#endif // EDIT_LOTS_DIALOG_H