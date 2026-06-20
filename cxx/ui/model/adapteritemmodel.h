#ifndef ADAPTER_ITEM_MODEL_H
#define ADAPTER_ITEM_MODEL_H

#include "columnadapter.h"
#include <QAbstractItemModel>

template<class Row>
concept Copyable = requires(Row &t){
    { new Row() } -> std::convertible_to<Row*>;
    { new Row(t) } -> std::convertible_to<Row*>;
};

class AdapterItemModel : public QAbstractItemModel {
    Q_OBJECT
protected:
    QHash<const QModelIndex, QVariant> changes; // TODO const values
    QList<QModelIndex> pendingDeletes;
    QHash<const QModelIndex, QString> errors; // TODO const values

public:
    explicit AdapterItemModel(QObject *parent = nullptr);

    virtual bool hasUnsavedChanges() const;
    virtual void clearChanges();
    virtual bool isValid() const;
    virtual bool enableDelete(const QModelIndex &index) const = 0;

    QVariant data(const QModelIndex &index, int role) const override;
    bool setData(const QModelIndex &index, const QVariant &value, int role) override;
    QVariant headerData(int section, Qt::Orientation orientation, int role) const override;

public Q_SLOTS:
    virtual QModelIndex queueAdd(const QModelIndex &selectedIndex = QModelIndex{}) = 0;
    virtual void queueDelete(const QModelIndex &index);
    virtual void undoChange(const QModelIndex &index);

protected:
    virtual AbstractColumnAdapter *adapter(const QModelIndex &index) const = 0;

public:
    virtual bool isPendingAdd(const QModelIndex &index) const = 0;
    virtual bool isPendingDelete(const QModelIndex &index) const;

protected:
    virtual QVariant value(const QModelIndex &index, int role = Qt::DisplayRole, QVariant current = QVariant{}) const = 0;
    /**
     * @brief setValue Sets a column value on a pending add (unsaved row).
     */
    virtual void setValue(const QModelIndex &index, const QVariant &value) = 0;

    void rowsChanged(int from, int to, const QModelIndex &parent);
    void rowChanged(const QModelIndex &index);
    void adjustErrorIndexes(int rowIndex, const QModelIndex &parent, int delta);

    const QString validate(const QModelIndex &index);
    void validateRow(int rowIndex, const QModelIndex &parent);
    void removeStaleErrors();
    void revalidateColumn(int column, const QModelIndex &parent);
    void revalidateRow(const QModelIndex &index);

    /** @brief rowIndexes returns indexes of row and its ancestors. */
    const QList<int> rowIndexes(const QModelIndex &index) const;
};

#endif // ADAPTER_ITEM_MODEL_H
