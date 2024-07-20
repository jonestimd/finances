#include "comboboxmodel.h"

#include <QMessageBox>

bool ComboBoxModel::compare(const NamedEntity *v1, const NamedEntity *v2) {
    auto name1 = v1->displayName(), name2 = v2->displayName();
    auto lname1 = name1.toLower(), lname2 = name2.toLower();
    return lname1 == lname2 ? name1 < name2 : lname1 < lname2;
}

ComboBoxModel::ComboBoxModel(const QList<const NamedEntity*> values, CreateValue newValue)
    : validator(this), createValue{newValue}
{
    options.append(values);
    std::sort(options.begin(), options.end(), ComboBoxModel::compare);
}

const NamedEntity *ComboBoxModel::valueOf(const QString &name) const {
    for (auto option : options) {
        if (option->displayName() == name) return option;
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
        if (index.column() == 1) return options.at(index.row())->displayName();
    }
    return QVariant{};
}

ComboBoxModel::Validator::Validator(const ComboBoxModel *model) : model{model} {}

QValidator::State ComboBoxModel::Validator::validate(QString &input, int &pos) const {
    if (input.isEmpty()) return QValidator::Acceptable;
    for (auto option : model->options) {
        if (option->displayName() == input) return QValidator::Acceptable;
    }
    return QValidator::Intermediate;
}

void ComboBoxModel::Validator::fixup(QString &input) const {
    for (auto option : model->options) {
        if (option->displayName().toLower().contains(input.toLower())) {
            input = option->displayName();
            return;
        }
    }
}
