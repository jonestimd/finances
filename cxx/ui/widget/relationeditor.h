#ifndef RELATIONEDITOR_H
#define RELATIONEDITOR_H

#include <service/model/basedomain.h>
#include <ui/model/comboboxmodel.h>
#include <QLineEdit>

class RelationEditor : public QLineEdit
{
    Q_OBJECT
    ComboBoxModel *model;
    const NamedEntity *entity_;
public:
    RelationEditor(ComboBoxModel *model, QWidget *parent = nullptr);

    const NamedEntity *entity() const;
    void setEntity(const NamedEntity * entity);

    Q_PROPERTY(const NamedEntity *entity READ entity WRITE setEntity USER true STORED false NOTIFY entityChanged)

    Q_SIGNAL void entityChanged(const NamedEntity *);

protected Q_SLOTS:
    void inputTextChanged(const QString &text);
    void activated(const QModelIndex &index);

protected:
    void keyReleaseEvent(QKeyEvent *event) override;
};

#endif // RELATIONEDITOR_H
