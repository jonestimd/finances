#include "comboboxmodel.h"
#include "ui/finances.h"

#include <QMessageBox>

ComboBoxModel::ComboBoxModel(const QList<const NamedEntity*> values, GetName getName, CreateValue createValue)
    : validator(this), getName{getName}, createValue{createValue}
{
    options.append(values);
    auto less = [this](const NamedEntity *v1, const NamedEntity *v2) {
        auto name1 = this->getName(v1), name2 = this->getName(v2);
        auto lname1 = name1.toLower(), lname2 = name2.toLower();
        return lname1 == lname2 ? name1 < name2 : lname1 < lname2;
    };
    std::stable_sort(options.begin(), options.end(), less);
}

void ComboBoxModel::addOption(const QString &name) {
    if (createValue) createValue(name);
}

int ComboBoxModel::rowCount(const QModelIndex &parent) const {
    return parent.isValid() ? 0 : options.length();
}

QVariant ComboBoxModel::data(const QModelIndex &index, int role) const {
    if (role == Qt::DisplayRole) return getName(options.at(index.row()));
    if (role == finances::EntityIdRole) return options.at(index.row())->id.value();
    if (role == finances::EntityPtrRole) return QVariant::fromValue(options.at(index.row()));
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
        if (name.contains(input, Qt::CaseInsensitive)) {
            input = name;
            return;
        }
    }
}
