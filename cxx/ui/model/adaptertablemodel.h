#ifndef ADAPTERTABLEMODEL_H
#define ADAPTERTABLEMODEL_H

#include <QAbstractTableModel>

class AdapterTableModel : public QAbstractTableModel
{
    Q_OBJECT
public:
    explicit AdapterTableModel(QObject *parent = nullptr);

    virtual int columnIndex(const QString name) const = 0;

    virtual bool hasUnsavedChanges() const = 0;
    virtual void clearChanges() = 0;
    virtual bool isValid() const = 0;
    virtual bool enableDelete(int rowIndex) const = 0;

public Q_SLOTS:
    virtual int queueAdd() = 0;
    virtual void queueDelete(int rowIndex) = 0;
    virtual void undoChange(const QModelIndex &index) = 0;
};

#endif // ADAPTERTABLEMODEL_H
