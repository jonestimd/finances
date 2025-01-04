#include "comboboxmodel.h"

#include <QMessageBox>

ComboBoxModel::ComboBoxModel(const QList<const NamedEntity*> values, GetName getName, CreateValue createValue)
    : validator(this), getName{getName}, createValue{createValue}
{
    options.append(values);
    auto less = [getName](const NamedEntity *v1, const NamedEntity *v2) {
        auto name1 = getName(v1), name2 = getName(v2);
        auto lname1 = name1.toLower(), lname2 = name2.toLower();
        return lname1 == lname2 ? name1 < name2 : lname1 < lname2;
    };
    std::sort(options.begin(), options.end(), less);
}

const NamedEntity *ComboBoxModel::valueOf(const QString &name) const {
    for (auto option : options) {
        if (getName(option) == name) return option;
    }
    return nullptr;
}

void ComboBoxModel::addOption(const QString &name) {
    if (createValue) createValue(name);
}

int ComboBoxModel::rowCount(const QModelIndex &parent) const {
    if (parent.isValid()) return 0;
    return options.length();
}

int ComboBoxModel::columnCount(const QModelIndex &parent) const {
    return 2;
}

QVariant ComboBoxModel::data(const QModelIndex &index, int role) const {
    if (role == Qt::DisplayRole) {
        if (index.column() == 0) return options.at(index.row())->id;
        if (index.column() == 1) return getName(options.at(index.row()));
    }
    return QVariant{};
}

ComboBoxModel::Validator::Validator(const ComboBoxModel *model) : model{model} {}

QValidator::State ComboBoxModel::Validator::validate(QString &input, int &pos) const {
    if (input.isEmpty()) return QValidator::Acceptable;
    for (auto option : model->options) {
        if (model->getName(option) == input) return QValidator::Acceptable;
    }
    return QValidator::Intermediate;
}

void ComboBoxModel::Validator::fixup(QString &input) const {
    for (auto option : model->options) {
        auto name = model->getName(option);
        if (name.toLower().contains(input.toLower())) {
            input = name;
            return;
        }
    }
}
