#ifndef DETAILCOLUMNADAPTER_H
#define DETAILCOLUMNADAPTER_H

#include "amountcolumnadapter.h"
#include "datastore.h"
#include "service/model/transactiondetail.h"
#include "ui/model/columnadapter.h"

class TransactionTableModel;

/**
 * @brief The TransactionTypeColumnAdapter class displays the detail's category
 * or the related detail's account for a transfer.
 */
class TransactionTypeColumnAdapter : public ColumnAdapter<TransactionDetail>  {
    DataStore *const dataStore;

public:
    TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore);

    QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override;
    QVariant fieldValue(const TransactionDetail *row) const override;

    void setValue(TransactionDetail *row, QVariant value) const override;
    void setValue(TransactionDetailUpdate *model, QVariant value) const;

private:
    QVariant getId(const QVariant &value) const;

    QString optionText(const NamedEntity* option) const;

    ComboBoxModel *getOptions() const;
};

class SharesColumnAdapter : public AmountColumnAdapter<TransactionDetail> {
public:
    SharesColumnAdapter(const QString &title, const TransactionTableModel *model);
};

class DetailAmountColumnAdapter : public AmountColumnAdapter<TransactionDetail> {
public:
    DetailAmountColumnAdapter(const QString &title);
};

/**
 * @brief The EmptyColumnAdapter class displays an empty cell.
 */
class EmptyColumnAdapter : public ColumnAdapter<TransactionDetail> {
public:
    EmptyColumnAdapter();
    QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override;;
};

#endif // DETAILCOLUMNADAPTER_H
