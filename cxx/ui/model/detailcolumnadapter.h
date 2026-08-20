#ifndef DETAILCOLUMNADAPTER_H
#define DETAILCOLUMNADAPTER_H

#include "amountcolumnadapter.h"
#include "service/model/transactiondetail.h"
#include "ui/model/columnadapter.h"
#include "ui/store/datastore.h"

class TransactionTableModel;
class TransactionDetailTableModel;

/**
 * @brief The TransactionTypeColumnAdapter class displays the detail's category
 * or the related detail's account for a transfer.
 */
class TransactionTypeColumnAdapter : public ColumnAdapter<TransactionDetail>  {
    DataStore *const dataStore;
    const optional_id accountId;

public:
    TransactionTypeColumnAdapter(const QString &title, DataStore *dataStore, optional_id accountId);

    QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override;
    QVariant rowValue(const TransactionDetail *row) const override;
    void setValue(TransactionDetail *model, QVariant value) const override;

private:
    QString optionText(const NamedEntity* option) const;

    ComboBoxModel *getOptions() const;
};

class SharesColumnAdapter : public AmountColumnAdapter<TransactionDetail, std::optional<QDecNumber>> {
    const SecurityStore *securityStore;
    const std::function<QVariant(const QModelIndex&)> getDate;
    const std::function<QVariant(const QModelIndex&)> getSecurityId;

public:
    SharesColumnAdapter(const QString &title, const TransactionTableModel *model, const SecurityStore *securityStore);
    SharesColumnAdapter(const QString &title, const TransactionDetailTableModel *model, const SecurityStore *securityStore);

    virtual QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override;
};

class DetailAmountColumnAdapter : public AmountColumnAdapter<TransactionDetail, QDecNumber> {
public:
    DetailAmountColumnAdapter(const QString &title);

    virtual QVariant value(const TransactionDetail *row, const QModelIndex &index, const QVariant current, int role) const override;
};

/**
 * @brief The EmptyColumnAdapter class displays an empty cell.
 */
class EmptyColumnAdapter : public ColumnAdapter<TransactionDetail> {
public:
    EmptyColumnAdapter();

    virtual QVariant rowValue(const TransactionDetail* row) const override;
};

#endif // DETAILCOLUMNADAPTER_H
