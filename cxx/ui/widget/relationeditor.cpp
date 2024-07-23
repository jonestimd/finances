#include "relationeditor.h"

#include <QCompleter>
#include <QKeyEvent>
#include <QMessageBox>
#include <QPainter>
#include <QRegularExpressionValidator>
#include <QStyle>
#include <QStyleOptionFrame>

RelationEditor::RelationEditor(ComboBoxModel *model, QWidget *parent)
    : QLineEdit(parent)
    , model{model}
    , entity_{nullptr}
{
    model->setParent(this);
    auto completer = new QCompleter(model, this);
    setCompleter(completer);
    completer->setCompletionColumn(1);
    completer->setCompletionRole(Qt::DisplayRole);
    completer->setCaseSensitivity(Qt::CaseInsensitive);
    completer->setFilterMode(Qt::MatchContains);
    setValidator(&model->validator);
    connect(this, &QLineEdit::textChanged, this, [this](const QString &text) {
        auto value = this->model->valueOf(text);
        if (value != entity_) {
            entity_ = value;
            emit entityChanged(entity_);
        }
    });
}

const NamedEntity *RelationEditor::entity() const {
    return entity_;
}

void RelationEditor::setEntity(const NamedEntity *entity) {
    entity_ = entity;
    if (entity) setText(entity->displayName());
    else setText("");
}

void RelationEditor::keyReleaseEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Return || event->key() == Qt::Key_Enter) {
        auto text = this->text();
        auto pos = this->cursorPosition();
        if (validator()->validate(text, pos) == QValidator::State::Intermediate) {
            this->model->addOption(text);
        }
    }
    QLineEdit::keyReleaseEvent(event);
}
