#ifndef POD_ITEM_MODEL_H
#define POD_ITEM_MODEL_H

#include <QBrush>
#include <QDecNumber.hh>
#include "adapteritemmodel.h"
#include "columnadapter.h"

template<Copyable Row>
class PodItemModel : public AdapterItemModel
{
protected:
    const QList<ColumnAdapter<Row>*> columns;
    QHash<const QModelIndex, QVariant> changes; // TODO const values
    QHash<QModelIndex, QString> errors; // TODO const keys/values
    /*!
     * \brief newRows map of parent index to added children
     */
    QHash<const QModelIndex, QList<Row*>> newRows;
    QList<const Row*> pendingDeletes;

    QList<Row*> pendingAdds(const QModelIndex &parent = QModelIndex()) const {
        return newRows.value(parent, QList<Row*>());
    }

    QVariant value_(const QModelIndex index, int role = Qt::DisplayRole, QVariant current = QVariant{}) const {
        return columns[index.column()]->value(getRow(index), index, current, role);
    }

    virtual int childCount(const QModelIndex &index) const = 0;

    void rowsChanged(int from, int to, const QModelIndex &parent) {
        emit dataChanged(index(from, 0, parent), index(to, columns.length()-1, parent), QList<int>{Qt::DisplayRole, finances::UnsavedRole});
    }

    void rowChanged(const QModelIndex &index) {
        rowsChanged(index.row(), index.row(), index.parent());
    }

    void setValue_(Row *row, int column, QVariant value) {
        columns[column]->setValue(row, value);
    }

    void removeStaleErrors() {
        QList<QModelIndex> fixes;
        for (auto [index, message] : errors.asKeyValueRange()) {
            auto newMessage = columns[index.column()]->isValid(index);
            if (newMessage.isNull()) fixes.append(index);
        }
        for (auto index : fixes) {
            errors.remove(index);
            emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
        }
    }

    void revalidateColumn(int column) {
        auto changes = columns[column]->revalidate(errors, index(0, column));
        for (auto index : changes) {
            emit dataChanged(index, index, QList<int>{finances::ValidationMessageRole});
        }
    }

    void revalidateRow(const QModelIndex &index) {
        for (int c = 0; c < columns.length(); ++c) {
            auto i = index.siblingAtColumn(c);
            auto message = columns[c]->isValid(i);
            if (message.isEmpty()) {
                if (errors.remove(i)) {
                    emit dataChanged(i, i, QList<int>{finances::ValidationMessageRole});
                    removeStaleErrors();
                }
            } else {
                if (!errors.contains(i) || message != errors.value(i)) {
                    errors.insert(i, message);
                    emit dataChanged(i, i, QList<int>{finances::ValidationMessageRole});
                }
                // uniqueness conflict may have moved to another row
                revalidateColumn(c);
            }
        }
    }

    const QList<int> rowIndexes(const QModelIndex &index) const {
        QList<int> indexes{index.row()};
        auto parent = index.parent();
        while (parent.isValid()) {
            indexes.append(parent.row());
            parent = parent.parent();
        }
        return indexes;
    }

public:
    explicit PodItemModel(const QList<ColumnAdapter<Row>*> columns, QObject *parent = nullptr)
        : AdapterItemModel(parent), columns{columns}
    {
        for (auto column : columns) {
            column->initialize(this);
        }
    }

    ~PodItemModel() {
        for (auto column : columns) delete column;
    }

    virtual const Row *getRow(const QModelIndex &index) const = 0;

    int queueAdd(const QModelIndex &parent) override {
        auto rowIndex = rowCount(parent);
        beginInsertRows(parent, rowIndex, rowIndex);
        Row *row = new Row;
        if (!newRows.contains(parent)) newRows.insert(parent, QList<Row*>());
        newRows[parent].append(row);
        for (int colIndex = 0; colIndex < columns.length(); ++colIndex) {
            auto i = index(rowIndex, colIndex, parent);
            auto message = columns[colIndex]->isValid(i);
            if (!message.isNull()) errors.insert(i, message);
        }
        endInsertRows();
        return rowIndex;
    }

    bool enableDelete(const QModelIndex &index) const override {
        return getRow(index)->deletable();
    }

    void queueDelete(const QModelIndex &index) override {
        auto savedChildCount = childCount(index.parent());
        if (newRows.contains(index.parent()) && index.row() >= savedChildCount) {
            beginRemoveRows(index.parent(), index.row(), index.row());
            delete newRows[index.parent()].takeAt(index.row() - savedChildCount);
            if (newRows[index.parent()].isEmpty()) newRows.remove(index.parent());
            QHash<QModelIndex, QString> updateErrors;
            for (auto [key, value] : errors.asKeyValueRange()) {
                if (key.parent() != index.parent() || key.row() < index.row()) updateErrors.insert(key, value);
                else if (key.row() > index.row()) updateErrors.insert(key.siblingAtRow(key.row()-1), value);
            }
            errors.clear();
            errors.insert(updateErrors);
            endRemoveRows();
            removeStaleErrors();
        }
        else if (!pendingDeletes.contains(getRow(index))) {
            pendingDeletes.append(getRow(index));
            rowChanged(index);
        }
    }

    void undoChange(const QModelIndex &index) override {
        if (pendingDeletes.removeAll(getRow(index)) > 0) rowChanged(index);
        else if (changes.contains(index)) {
            changes.remove(index);
            emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
            revalidateRow(index);
        }
    }

    int columnIndex(const QString name) const override {
        for (int col = 0; col < columns.length(); ++col) {
            if (columns[col]->title == name) return col;
        }
        return -1;
    }

    virtual bool hasUnsavedChanges() const override {
        return !changes.isEmpty() || !newRows.isEmpty() || !pendingDeletes.isEmpty();
    }

    virtual bool isValid() const override {
        return errors.isEmpty();
    }

    const QList<Row*> unsavedAdds() const {
        QList<Row*> rows;
        for (const auto &children : newRows) {
            for (auto row : children) {
                rows.append(new Row(*row));
            }
        }
        return rows;
    }

    const QList<Row*> unsavedChanges() { // TODO override in category model to handle parent change
        QHash<const QList<int>, Row*> changeRows;
        for (auto i = changes.cbegin(), end = changes.cend(); i != end; ++i) {
            if (pendingDeletes.contains(getRow(i.key()))) continue;
            Row *updated;
            auto indexes = rowIndexes(i.key());
            if (changeRows.contains(indexes)) updated = changeRows[indexes];
            else {
                updated = new Row(*getRow(i.key()));
                changeRows[indexes] = updated;
            }
            setValue_(updated, i.key().column(), i.value());
        }
        return changeRows.values();
    }

    const QList<const Row*> unsavedDeletes() const {
        return pendingDeletes;
    }

    virtual void clearChanges() override {
        changes.clear(); // TODO emit changes
        pendingDeletes.clear(); // TODO emit changes
        errors.clear();
        if (!newRows.isEmpty()) {
            for (auto [key, rows] : newRows.asKeyValueRange()) {
                auto count = childCount(key);
                beginRemoveRows(key, count, rowCount(key.parent())-1);
                while (!rows.isEmpty()) delete rows.takeFirst();
                endRemoveRows();
            }
            newRows.clear();
        }
    }

    int columnCount(const QModelIndex &parent) const override {
        return columns.count();
    }

    QVariant data(const QModelIndex &index, int role) const override {
        switch (role) {
        case Qt::DisplayRole:
        case Qt::EditRole:
            if (changes.contains(index)) return value_(index, role, changes.value(index));
            break;
        case finances::ValidationMessageRole:
            if (errors.contains(index)) return errors[index];
            break;
        case finances::UnsavedRole:
            if (pendingDeletes.contains(getRow(index))) return finances::Delete;
            if (index.row() >= childCount(index.parent()) || changes.contains(index)) return finances::AddUpdate;
        }
        return value_(index, role);
    }

    Qt::ItemFlags flags(const QModelIndex &index) const override {
        if (!index.isValid()) return Qt::NoItemFlags;
        bool pendingDelete = pendingDeletes.contains(getRow(index));
        return AdapterItemModel::flags(index) | columns[index.column()]->flags(getRow(index), !pendingDelete);
    }

    bool setData(const QModelIndex &index, const QVariant &value, int role) override {
        if (role == Qt::EditRole) {
            errors.remove(index); // editor must have accepted the value
            auto savedRows = childCount(index.parent());
            if (index.row() >= savedRows) {
                setValue_(newRows[index.parent()][index.row()-savedRows], index.column(), value);
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole));
                revalidateRow(index);
                return true;
            }
            auto column = columns[index.column()];
            auto original = value_(index, Qt::EditRole);
            if (column->isEqual(original, value)) {
                if (changes.contains(index)) {
                    changes.remove(index);
                    emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                    revalidateRow(index);
                    return true;
                }
            } else if (!changes.contains(index) || !column->isEqual(value, changes[index])) {
                changes.insert(index, value);
                emit dataChanged(index, index, QList<int>(Qt::DisplayRole, finances::UnsavedRole));
                revalidateRow(index);
                return true;
            }
        }
        return false;
    }

    QVariant headerData(int section, Qt::Orientation orientation, int role) const override {
        if (role == Qt::DisplayRole && orientation == Qt::Horizontal && section >= 0 && section < columns.count()) {
            return columns[section]->title;
        }
        return QVariant{};
    }
};

#endif // POD_ITEM_MODEL_H
