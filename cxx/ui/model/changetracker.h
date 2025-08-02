#ifndef CHANGE_TRACKER_H
#define CHANGE_TRACKER_H

#include "columnadapter.h"

#include <QAbstractItemModel>
#include <QObject>

class AbstractChangeTracker : public QObject {
    Q_OBJECT
public:
    AbstractChangeTracker(QObject *parent);

    virtual bool hasUnsavedChanges() const = 0;
    virtual void clearChanges() = 0;

    virtual int childCount(const QModelIndex &index) const = 0;
    virtual bool isPendingAdd(const QModelIndex &index) const = 0;
    virtual bool isPendingDelete(const QModelIndex &index) const = 0;

    virtual QVariant value(const QModelIndex &index, int role = Qt::DisplayRole, QVariant current = QVariant{}) const = 0;
    virtual void setValue(const QModelIndex &index, const QVariant &value) = 0;

public Q_SLOTS:
    virtual QModelIndex queueAdd(const QModelIndex &selectedIndex = QModelIndex{}) = 0;
    virtual void queueDelete(const QModelIndex &index) = 0;
    virtual void undoChange(const QModelIndex &index) = 0;
};

template<class Row>
class ChangeTracker : AbstractChangeTracker {
protected:
    const QList<ColumnAdapter<Row>*> columns;

    QHash<const QModelIndex, QVariant> changes{}; // TODO const values
    QList<QModelIndex> pendingDeletes{}; // TODO const values?
    QHash<QModelIndex, QString> errors{}; // TODO const keys/values
    /*!
     * \brief newRows map of parent index to added children
     */
    QHash<const QModelIndex, QList<Row*>> newRows{};

public:
    ChangeTracker(QObject *parent, const QList<ColumnAdapter<Row>*> columns)
        : AbstractChangeTracker(parent)
        , columns{columns} {}

protected:
    AbstractColumnAdapter *adapter(const QModelIndex &index) const {
        return columns.at(index.column());
    }

public:
    const QList<Row*> pendingAdds(const QModelIndex &parent = QModelIndex()) const {
        return newRows.value(parent, QList<Row*>());
    }

    virtual bool hasUnsavedChanges() const override {
        return !changes.isEmpty() || !pendingDeletes.isEmpty() || !newRows.isEmpty();
    }

    virtual void clearChanges() override {
        changes.clear(); // TODO emit changes
        pendingDeletes.clear(); // TODO emit changes
        errors.clear();
        if (!newRows.isEmpty()) {
            for (auto [key, rows] : newRows.asKeyValueRange()) {
                auto count = childCount(key);
                // beginRemoveRows(key, count, rowCount(key.parent())-1);
                while (!rows.isEmpty()) delete rows.takeFirst();
                // endRemoveRows();
            }
            newRows.clear();
        }
    }

    virtual const Row *getRow(const QModelIndex &index) const = 0;

    virtual bool isPendingAdd(const QModelIndex &index) const override {
        return index.row() >= childCount(index.parent());
    }

    virtual bool isPendingDelete(const QModelIndex &index) const override {
        return pendingDeletes.contains(index.siblingAtColumn(0));
    }

    virtual QVariant value(const QModelIndex &index, int role, QVariant current) const override {
        return columns.at(index.column())->value(getRow(index), index, current, role);
    }

    virtual void setValue(const QModelIndex &index, const QVariant &value) override;

public Q_SLOTS:
    virtual QModelIndex queueAdd(const QModelIndex &selectedIndex) override;
    virtual void queueDelete(const QModelIndex &index) override;
    virtual void undoChange(const QModelIndex &index) override;
};

#endif // CHANGE_TRACKER_H
