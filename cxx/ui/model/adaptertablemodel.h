#ifndef ADAPTERTABLEMODEL_H
#define ADAPTERTABLEMODEL_H

#include <QAbstractTableModel>

class AdapterTableModel : public QAbstractTableModel
{
public:
    explicit AdapterTableModel(QObject *parent = nullptr);

    virtual int columnIndex(const QString name) const = 0;
};

#endif // ADAPTERTABLEMODEL_H
