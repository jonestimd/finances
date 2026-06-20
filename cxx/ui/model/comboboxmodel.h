#ifndef COMBOBOXMODEL_H
#define COMBOBOXMODEL_H

#include "service/model/basedomain.h"
#include <QAbstractListModel>
#include <QValidator>

class ComboBoxModel : public QAbstractListModel {
public:
    typedef std::function<QString(const NamedEntity*)> GetName;
    typedef std::function<void(const QString&)> CreateValue;

    class Validator : public QValidator {
        friend ComboBoxModel;
        const ComboBoxModel *model;
        Validator(const ComboBoxModel *model);
    public:
        State validate(QString &input, int &pos) const override;
        void fixup(QString &input) const override;
    };
    const Validator validator;

private:
    QList<const NamedEntity*> options;
    GetName getName;
    CreateValue createValue;

public:
    explicit ComboBoxModel(const QList<const NamedEntity*> values, GetName getName, CreateValue newValue = nullptr);

    void addOption(const QString &name);

    // QAbstractItemModel interface
    int rowCount(const QModelIndex &parent) const override;
    QVariant data(const QModelIndex &index, int role) const override;
};

#endif // COMBOBOXMODEL_H
