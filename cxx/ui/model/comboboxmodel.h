#ifndef COMBOBOXMODEL_H
#define COMBOBOXMODEL_H

#include "service/model/basedomain.h"
#include <QAbstractListModel>
#include <QValidator>

class ComboBoxModel : public QAbstractListModel {
    QList<const NamedEntity*> options;
public:
    typedef std::function<void(const QString &)> CreateValue;

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
    CreateValue createValue;

public:
    explicit ComboBoxModel(const QList<const NamedEntity*> values, CreateValue newValue = nullptr);

    const NamedEntity *valueOf(const QString &name) const;

    void addOption(const QString &name);

    // QAbstractItemModel interface
    int rowCount(const QModelIndex &parent) const override;
    int columnCount(const QModelIndex &parent) const override;
    QVariant data(const QModelIndex &index, int role) const override;
};

#endif // COMBOBOXMODEL_H
