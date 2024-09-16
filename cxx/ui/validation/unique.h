#ifndef UNIQUE_H
#define UNIQUE_H

#include "validatorfactory.h"
#include <QModelIndex>
#include <QValidator>

class UniqueValidatorFactory : public ValidatorFactory {
    Q_OBJECT
    const int columnIndex;
    QMultiHash<QStringList, int> values;
    QAbstractItemModel *model;

public:
    UniqueValidatorFactory(int columnIndex);

    const QString isValid(const QModelIndex &index, QString &value) const override;

    virtual void initialize(QAbstractItemModel *model) override;

public Q_SLOTS:
    void modelReset();
    void dataChanged(const QModelIndex &topLeft, const QModelIndex &bottomRight, const QList<int> &roles);
    void rowsInserted(const QModelIndex &parent, int first, int last);
    void rowsRemoved(const QModelIndex &parent, int first, int last);

protected:
    QStringList rowValues(const QModelIndex &index) const;
    virtual QStringList rowValues(const QString value, const QModelIndex &index) const;
    virtual inline bool isValidated(int columnIndex) const {
        return this->columnIndex == columnIndex;
    }
};

#endif // UNIQUE_H
