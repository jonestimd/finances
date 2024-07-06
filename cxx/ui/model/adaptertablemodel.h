#ifndef ADAPTERTABLEMODEL_H
#define ADAPTERTABLEMODEL_H

#include <QAbstractTableModel>

class AdapterTableModel : public QAbstractTableModel
{
    Q_OBJECT
public:
    explicit AdapterTableModel(QObject *parent = nullptr);

    virtual int columnIndex(const QString name) const = 0;

    Q_SLOT virtual int queueAdd() = 0;
    Q_SLOT virtual void queueDelete(int rowIndex) = 0;
};

#endif // ADAPTERTABLEMODEL_H
