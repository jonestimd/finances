#ifndef POD_ITEM_MODEL_H
#define POD_ITEM_MODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "adapteritemmodel.h"
#include "columnadapter.h"
#include "service/model/basedomain.h"

#define ROOT_ROW_TYPE 0

template<Copyable Row, class Store, Copyable PendingAdd = Row>
class PodItemModel : public AdapterItemModel {
protected:
    const Store *const store;

    const QList<ColumnAdapter<Row>*> columns;
    /*!
     * \brief newRows map of parent index to added children
     */
    QHash<const QModelIndex, QList<PendingAdd*>> newRows;

    const QList<PendingAdd*> pendingAdds(const QModelIndex &parent = QModelIndex()) const {
        return newRows.value(parent, QList<PendingAdd*>());
    }

    const PendingAdd* pendingAdd(const QModelIndex& index) const {
        auto rowIndex = index.row() - childCount(index.parent());
        Q_ASSERT(rowIndex >= 0);
        return newRows[index.parent()].at(rowIndex);
    }

    PendingAdd* pendingAdd(const QModelIndex& index) {
        auto rowIndex = index.row() - childCount(index.parent());
        Q_ASSERT(rowIndex >= 0);
        return newRows[index.parent()].at(rowIndex);
    }

    AbstractColumnAdapter *adapter(const QModelIndex &index) const override {
        return columns.at(index.column());
    }

    QVariant value(const QModelIndex &index, int role = Qt::DisplayRole, QVariant current = QVariant{}) const override {
        return columns.at(index.column())->value(getRow(index), index, current, role);
    }

    /**
     * @brief childCount Returns the count of *saved* children for the parent `index`.
     * Must be overriden by tree models to return the count for a valid `parent` indexes.
     */
    virtual int childCount(const QModelIndex &parent) const {
        Q_ASSERT(!parent.isValid());
        return parent.isValid() ? 0 : this->rootIds.length();
    }

    /** @brief Set a field value on a row entity. Used for new rows and for returning unsaved changes. */
    void setValue(Row *row, int column, QVariant value) {
        columns[column]->setValue(row, value);
    }

    /** @brief Set a field value on a new row. */
    void setValue(const QModelIndex &index, const QVariant &value) override {
        setValue(pendingAdd(index), index.column(), value);
    }

    int columnIndex(const QString &title) const {
        for (int i = 0; i < columns.length(); i++) {
            if (columns[i]->title == title) return i;
        }
        return -1;
    }

    virtual PendingAdd *newRow() {
        return new PendingAdd;
    }

    /** @brief Override to update state that dependents on `rootIds`. */
    virtual void rootIdsChanged() {}

public:
    explicit PodItemModel(const Store *store, const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterItemModel(parent), columns{columns}
        , store{store}
    {
        for (auto column : columns) {
            column->initialize(this);
        }
        connect(store, SIGNAL(valuesToBeRemoved(QList<domain_id>)), this, SLOT(valuesToBeRemoved(QList<domain_id>)), Qt::DirectConnection);
        connect(store, SIGNAL(valuesAdded(QList<domain_id>)), this, SLOT(valuesAdded(QList<domain_id>)), Qt::DirectConnection);
    }

    ~PodItemModel() {
        qDeleteAll(columns);
    }

    virtual void setRows(const QList<domain_id> rowIds) {
        this->clearChanges();
        this->beginResetModel();
        this->rootIds.clear();
        this->rootIds.append(rowIds);
        this->rootIdsChanged();
        this->endResetModel();
    }

    /** Must be overriden by tree models if `index.parent()` can be valid. */
    virtual const Row *getRow(const QModelIndex &index) const {
        Q_ASSERT(!index.parent().isValid());
        auto i = this->rootIds.length();
        if (index.row() < i) {
            auto id = this->rootIds.value(index.row());
            return this->store->value(id);
        }
        return this->pendingAdds().at(index.row() - i);
    }

    bool isPendingAdd(const QModelIndex &index) const override {
        return index.row() >= childCount(index.parent());
    }

    QModelIndex queueAdd(const QModelIndex &selectedIndex) override {
        auto parent = selectedIndex.parent();
        auto rowIndex = rowCount(parent);
        beginInsertRows(parent, rowIndex, rowIndex);
        PendingAdd *row = newRow();
        if (!newRows.contains(parent)) newRows.insert(parent, QList<PendingAdd*>());
        newRows[parent].append(row);
        validateRow(rowIndex, parent);
        endInsertRows();
        return index(rowIndex, 0, parent);
    }

    bool enableDelete(const QModelIndex &index) const override {
        return getRow(index)->deletable();
    }

    void queueDelete(const QModelIndex &index) override {
        auto parent = index.parent();
        auto indexRow = index.row();
        if (isPendingAdd(index)) {
            beginRemoveRows(parent, indexRow, indexRow);
            delete newRows[parent].takeAt(indexRow - childCount(parent));
            if (newRows[parent].isEmpty()) newRows.remove(parent);
            endRemoveRows();
            adjustErrorIndexes(this->index(indexRow, 0, parent), -1);
            removeStaleErrors();
        }
        else AdapterItemModel::queueDelete(index);
    }

    bool hasUnsavedChanges() const override {
        return !newRows.isEmpty() || AdapterItemModel::hasUnsavedChanges();
    }

    /**
     * @return Returns copies of the new rows.
     */
    QList<const PendingAdd*> unsavedAdds(int rowIndex = -1) const {
        QList<const PendingAdd*> rows;
        int i = childCount(QModelIndex{});
        for (const auto row : pendingAdds()) {
            if (rowIndex < 0 || i == rowIndex) rows.append(row);
            i++;
        }
        return rows;
    }

    /**
     * @brief rowType For a table with multiple row types, `rowType` indicates which row
     * type is at `index`.
     * @return an `int` indicating the row type (defaults to 0).
     */
    virtual int rowType(const QModelIndex &index) const {
        return ROOT_ROW_TYPE;
    }

    virtual QList<Row*> unsavedChanges(int rowIndex = -1) {
        QHash<const QList<int>, Row*> changeRows;
        for (auto [index, value] : changes.asKeyValueRange()) {
            if (isPendingDelete(index) || rowType(index) != ROOT_ROW_TYPE || rowIndex >= 0 && index.row() != rowIndex) continue;
            Row *updated;
            auto indexes = rowIndexes(index);
            if (changeRows.contains(indexes)) updated = changeRows[indexes];
            else {
                updated = new Row(*getRow(index));
                changeRows[indexes] = updated;
            }
            setValue(updated, index.column(), value);
        }
        return changeRows.values();
    }

    virtual QList<const Row*> unsavedDeletes(int rowIndex = -1) const {
        QList<const Row*> deletes{};
        for (auto i : pendingDeletes) {
            if (rowType(i) == ROOT_ROW_TYPE && (rowIndex < 0 || i.row() == rowIndex)) deletes.append(getRow(i));
        }
        return deletes;
    }

    void clearChanges() override {
        AdapterItemModel::clearChanges();
        if (!newRows.isEmpty()) {
            for (auto i = newRows.begin(); i != newRows.end();) {
                auto parent = i.key();
                beginRemoveRows(parent, childCount(parent), rowCount(parent)-1);
                qDeleteAll(i.value());
                i = newRows.erase(i);
                endRemoveRows();
            }
        }
    }

    int columnCount(const QModelIndex &parent) const override {
        return columns.count();
    }

    Qt::ItemFlags flags(const QModelIndex &index) const override {
        if (!index.isValid()) return Qt::NoItemFlags;
        bool pendingDelete = pendingDeletes.contains(index.siblingAtColumn(0));
        return AdapterItemModel::flags(index) | columns[index.column()]->flags(getRow(index), !pendingDelete);
    }
};

#endif // POD_ITEM_MODEL_H
