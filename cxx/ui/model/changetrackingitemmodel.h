#ifndef CHANGE_TRACKING_ITEM_MODEL_H
#define CHANGE_TRACKING_ITEM_MODEL_H

#include <QAbstractItemModel>

class SortFilterProxyModel;

class ChangeTrackingItemModel : public QAbstractItemModel {
    Q_OBJECT
public:
    ChangeTrackingItemModel(QObject* parent = nullptr);

    virtual bool hasUnsavedChanges() const = 0;
    virtual void clearChanges() = 0;
    virtual bool isValid() const = 0;

    virtual void undoChange(const QModelIndex &index) = 0;
};

class ChangeHandler : public QObject {
    Q_OBJECT
    QList<QMetaObject::Connection> connections;
    SortFilterProxyModel* const sortModel;
    const std::function<void(const ChangeTrackingItemModel*)> handleChange;

public:
    ChangeHandler(QObject* parent, SortFilterProxyModel* sortModel, std::function<void(const ChangeTrackingItemModel*)> handleChange);

private Q_SLOTS:
    void modelChanged();
    void dataChanged();
};

#endif // CHANGE_TRACKING_ITEM_MODEL_H
