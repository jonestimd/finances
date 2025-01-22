#include "relationeditor.h"
#include "ui/finances.h"

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
    completer->setCompletionRole(Qt::DisplayRole);
    completer->setCaseSensitivity(Qt::CaseInsensitive);
    completer->setFilterMode(Qt::MatchContains);
    setValidator(&model->validator);
    connect(completer, SIGNAL(activated(QModelIndex)), this, SLOT(activated(QModelIndex)));
    connect(this, SIGNAL(textChanged(QString)), this, SLOT(inputTextChanged(QString)));
}

const NamedEntity *RelationEditor::entity() const {
    return entity_;
}

void RelationEditor::setEntity(const NamedEntity *entity) {
    entity_ = entity;
    if (entity) setText(entity->name.toString());
    else setText("");
}

void RelationEditor::inputTextChanged(const QString &text) {
    if (entity_) {
        if (text != entity_->name) {
            entity_ = nullptr;
            emit entityChanged(entity_);
        }
    }
    else if (completer()->completionCount() == 1) {
        auto index = completer()->completionModel()->index(0, 0);
        if (text == index.data()) activated(index);
    }
}

void RelationEditor::activated(const QModelIndex &index) {
    entity_ = index.data(finances::EntityPtrRole).value<const NamedEntity*>();
    emit entityChanged(entity_);
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
