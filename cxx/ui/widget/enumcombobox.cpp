#include "enumcombobox.h"
#include <QAbstractItemView>

QList<const EnumValue *> EnumComboBox::sortedOptions(const QHash<QString, const EnumValue*> valueMap) {
    QList<const EnumValue*> options;
    options.append(valueMap.values());
    std::stable_sort(options.begin(), options.end(), &EnumValue::less);
    return options;
}

EnumComboBox::EnumComboBox(const QHash<QString, const EnumValue*> valueMap, QWidget *parent)
    : QComboBox{parent}, options{sortedOptions(valueMap)}, entity_{nullptr}
{
    for (auto option : options) addItem(option->name);
    connect(this, &QComboBox::currentIndexChanged, this, [this](int index) {
        auto value = index < 0 ? nullptr :  this->options[index];
        if (entity_ != value) {
            entity_ = value;
            emit entityChanged(entity_);
        }
    });
}

const EnumValue *EnumComboBox::entity() const {
    return entity_;
}

void EnumComboBox::setEntity(const EnumValue *entity) {
    if (entity) {
        auto index = options.indexOf(entity);
        setCurrentIndex(index);
    } else setCurrentIndex(-1);
}

void EnumComboBox::focusInEvent(QFocusEvent *event) {
    QComboBox::focusInEvent(event);
    if (!view()->isVisible()) showPopup();
}
