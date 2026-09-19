#include "editlotsdialog.h"
#include "ui/model/formats.h"
#include "ui/model/securitylottablemodel.h"
#include "ui/uicontext.h"
#include <QFormLayout>
#include <QPushButton>

#define SETTINGS_GROUP "editLotsDialog"

QLineEdit* readOnlyLineEdit(const QString& contents) {
    auto field = new QLineEdit{contents};
    field->setReadOnly(true);
    field->setFocusPolicy(Qt::NoFocus);
    return field;
}

QFormLayout* formLayout() {
    auto layout = new QFormLayout;
    layout->setLabelAlignment(Qt::AlignTrailing);
    layout->setContentsMargins(10, 0, 10, 0);
    return layout;
}

EditLotsDialog::EditLotsDialog(QMainWindow* parent, UiContext* context, const TransactionDetail* sale)
    : EntityDialog{parent, tr("Security Lot"), SETTINGS_GROUP, new SecurityLotTableModel{context->dataStore, sale},
        new QTableView, &context->dataStore->messageStore}
    , context{context}
    , purchaseSharesField{readOnlyLineEdit("")}
{
    setWindowTitle(tr("Allocate Lots[*]"));

    auto transaction = context->dataStore->transactionStore->value(sale->transactionId);
    auto security = context->dataStore->securityStore->value(transaction->securityId.value());
    auto headerLayout = formLayout();
    headerLayout->addRow(tr("Sale Date:"), readOnlyLineEdit(transaction->date.toString(*dateDisplayFormat)));
    headerLayout->addRow(tr("Security:"), readOnlyLineEdit(security->name));
    layout.insertLayout(0, headerLayout);

    using enum finances::FontIcon;
    entityView.insertAction(0, finances::iconAction(PlaylistRemove, tr("Discard Lots"), tr("ctrl+delete"), model(), SLOT(discardLots())));
    entityView.insertAction(0, finances::iconAction(Sort, tr("Highest Price"), tr("ctrl+h"), model(), SLOT(allocateHighestPrice())));
    entityView.insertAction(0, finances::iconAction({Sort, true}, tr("Lowest Price"), tr("ctrl+l"), model(), SLOT(allocateLowestPrice())));
    entityView.insertAction(0, finances::iconAction(ClockArrowUp, tr("Last In"), tr("ctrl+t"), model(), SLOT(allocateLastIn())));
    entityView.insertAction(0, finances::iconAction(ClockArrowDown, tr("First In"), tr("ctrl+f"), model(), SLOT(allocateFirstIn())));
    entityView.statusBar.addWidget(&allocationStatus);

    auto summaryLayout = formLayout();
    summaryLayout->addRow(tr("Sale Shares:"), readOnlyLineEdit(sale->assetQuantity.value().abs().toString()));
    summaryLayout->addRow(tr("Total Lot Shares:"), purchaseSharesField);
    layout.insertLayout(3, summaryLayout);

    connect(model(), SIGNAL(modelReset()), this, SLOT(dataChanged()));
    connect(model(), SIGNAL(dataChanged(QModelIndex,QModelIndex,QList<int>)), this, SLOT(dataChanged()));
    // TODO select editable column (shares) after loading
}

SecurityLotTableModel* EditLotsDialog::model() const {
    return entityView.model<SecurityLotTableModel>();
}

void EditLotsDialog::loadData() {
    context->dataStore->transactionStore->findPurchases(this, model()->sale);
}

void EditLotsDialog::saveData() {
    auto model = this->model();
    context->dataStore->securityStore->updateLots(this, model->unsavedChanges(), model->unsavedAdds(), model->unsavedDeletes());
}

void EditLotsDialog::dataChanged() {
    purchaseSharesField->setText(model()->totalAllocatedShares().toString());
    if (!model()->isValid()) allocationStatus.setText(tr("Total lot shares exceeds sale shares"));
    else allocationStatus.setText("");
}

void EditLotsDialog::showEvent(QShowEvent* event) {
    loadData();
    QDialog::showEvent(event);
}
