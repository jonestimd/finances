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
    // , entity_{nullptr}
{
    model->setParent(this);
    auto completer = new QCompleter(model, this);
    setCompleter(completer);
    completer->setCompletionRole(Qt::DisplayRole);
    completer->setCaseSensitivity(Qt::CaseInsensitive);
    completer->setFilterMode(Qt::MatchContains);
    setValidator(&model->validator);
    connect(completer, SIGNAL(activated(QModelIndex)), this, SLOT(activated(QModelIndex)));
    connect(completer, SIGNAL(highlighted(QModelIndex)), this, SLOT(activated(QModelIndex)));
    connect(this, SIGNAL(textChanged(QString)), this, SLOT(inputTextChanged(QString)));
}

const NamedEntity *RelationEditor::entity() const {
    return selectedIndex.isValid() ? selectedIndex.data(finances::EntityPtrRole).value<const NamedEntity*>() : nullptr;
}

void RelationEditor::setEntity(const NamedEntity *entity) {
    if (entity) {
        selectedIndex = model->indexOf(entity);
        setText(selectedIndex.data().toString());
    } else {
        selectedIndex = {};
        setText("");
    }
}

void RelationEditor::inputTextChanged(const QString &text) {
    if (selectedIndex.isValid()) {
        if (text != selectedIndex.data()) {
            selectedIndex = {};
            emit entityChanged(entity());
        }
    } else if (completer()->completionCount() == 1) {
        auto index = completer()->completionModel()->index(0, 0);
        if (text == index.data()) {
            selectedIndex = index;
            activated(index);
        }
    }
}

void RelationEditor::activated(const QModelIndex &index) {
    selectedIndex = index;
    emit entityChanged(entity());
}

void RelationEditor::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Return || event->key() == Qt::Key_Enter) {
        auto text = this->text().toLower();
        auto model = completer()->completionModel();
        for (int i = 0; i < model->rowCount(); i++) {
            auto index = model->index(i, 0);
            if (text == index.data().toString().toLower()) {
                completer()->popup()->selectionModel()->select(index, QItemSelectionModel::SelectionFlag::SelectCurrent);
                activated(index);
                break;
            }
        }
    }
    QLineEdit::keyPressEvent(event);
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
