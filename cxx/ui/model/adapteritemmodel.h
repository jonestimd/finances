#ifndef ADAPTER_ITEM_MODEL_H
#define ADAPTER_ITEM_MODEL_H

#include <QAbstractItemModel>

template<class Row>
concept Copyable = requires(Row &t){
    { new Row() } -> std::convertible_to<Row*>;
    { new Row(t) } -> std::convertible_to<Row*>;
};

class AdapterItemModel : public QAbstractItemModel {
    Q_OBJECT

public:
    explicit AdapterItemModel(QObject *parent = nullptr);

    virtual bool hasUnsavedChanges() const = 0;
    virtual void clearChanges() = 0;
    virtual bool isValid() const = 0;
    virtual bool enableDelete(const QModelIndex &index) const = 0;

public Q_SLOTS:
    virtual int queueAdd(const QModelIndex &parent = QModelIndex{}) = 0;
    virtual void queueDelete(const QModelIndex &index) = 0;
    virtual void undoChange(const QModelIndex &index) = 0;
};

#endif // ADAPTER_ITEM_MODEL_H
