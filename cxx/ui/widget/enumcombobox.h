#ifndef ENUMCOMBOBOX_H
#define ENUMCOMBOBOX_H

#include "service/model/basedomain.h"
#include <QComboBox>

class EnumComboBox : public QComboBox
{
    Q_OBJECT
    const QList<const EnumValue*> options;
    const EnumValue *entity_;

    static QList<const EnumValue*> sortedOptions(const QHash<QString, const EnumValue*> options);

public:
    EnumComboBox(const QHash<QString, const EnumValue*> valueMap, QWidget *parent);

    const EnumValue *entity() const;
    void setEntity(const EnumValue *entity);

    Q_PROPERTY(const EnumValue *entity READ entity WRITE setEntity USER true STORED false NOTIFY entityChanged)

    Q_SIGNAL void entityChanged(const EnumValue*);

    // QWidget interface
protected:
    virtual void focusInEvent(QFocusEvent *event) override;
};

#endif // ENUMCOMBOBOX_H
