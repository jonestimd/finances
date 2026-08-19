#ifndef TRANSACTION_DETAIL_MODEL_H
#define TRANSACTION_DETAIL_MODEL_H

#include "service/model/transactiondetail.h"
#include "ui/model//columnadapter.h"

class DataStore;

class TransactionDetailTableModel : public QAbstractTableModel {
    Q_OBJECT
    const QList<ColumnAdapter<SearchTransactionDetail>*> txColumns;
    const QList<ColumnAdapter<TransactionDetail>*> detailColumns;
    DataStore* const dataStore;
    QList<const SearchTransactionDetail*> details{};

public:
    static const int dateColumn;
    static const int securityColumn;
    static const int sharesColumn;

    explicit TransactionDetailTableModel(DataStore *dataStore);
    ~TransactionDetailTableModel();

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;
    int columnCount(const QModelIndex &parent) const override;
    int rowCount(const QModelIndex &parent) const override;
    QVariant data(const QModelIndex &index, int role) const override;

    const SearchTransactionDetail* getRow(const QModelIndex& index) const;

    static QVariant detailDate(const QModelIndex& index);
    static QVariant detailSecurityId(const QModelIndex& index);

public slots:
    void setRows(QList<const SearchTransactionDetail*> rows); // clazy:exclude=fully-qualified-moc-types
    void splitsLoaded();

private:
    void reset();
};

#endif // TRANSACTION_DETAIL_MODEL_H