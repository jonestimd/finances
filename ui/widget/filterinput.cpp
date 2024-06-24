#include "filterinput.h"

#include "../finances.h"
#include <QPainter>
#include <QRegularExpressionValidator>
#include <QStyle>
#include <QStyleOptionFrame>

FilterInput::FilterInput(
    const char *placeholderText,
    QSortFilterProxyModel *model,
    QWidget *parent) : QLineEdit(parent)
{
    setPlaceholderText(tr(placeholderText));
    setClearButtonEnabled(true);
    auto margins = textMargins();
    QFont font = finances::iconFont->font();
    font.setPixelSize(height() * 0.8);
    QFontMetrics fm(font);
    fm.boundingRect(icon).width();
    margins.setLeft(margins.left() + fm.boundingRect(icon).width() * 1.2);
    setTextMargins(margins);

    connect(this, SIGNAL(textChanged(QString)), this, SLOT(onTextChanged(QString)));
    connect(this, SIGNAL(filterChanged(QRegularExpression)),
            model, SLOT(setFilterRegularExpression(QRegularExpression)));
    setProperty("invalid", false);
}

void FilterInput::paintEvent(QPaintEvent *event) {
    QLineEdit::paintEvent(event);
    QPainter p(this);
    QPalette pal = palette();
    QFont font = finances::iconFont->font();
    font.setPixelSize(height() * 0.8);

    QStyleOptionFrame panel;
    initStyleOption(&panel);
    QRect r = style()->subElementRect(QStyle::SE_LineEditContents, &panel, this);

    p.setBrush(pal.text());
    p.setFont(font);
    p.drawText(r, Qt::AlignVCenter, icon);
}

void FilterInput::onTextChanged(const QString &text) {
    auto re = QRegularExpression(text, QRegularExpression::CaseInsensitiveOption);
    auto wasInvalid = property("invalid");
    auto invalid = !re.isValid();
    if (wasInvalid != invalid) {
        setProperty("invalid", invalid);
        style()->unpolish(this);
    }
    if (re.isValid()) emit filterChanged(re);
}
