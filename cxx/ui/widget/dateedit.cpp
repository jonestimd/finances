#include "dateedit.h"
#include <QCalendarWidget>
#include <QKeyEvent>
#include <QStyle>
#include <QStyleOptionSpinBox>

DateEdit::DateEdit(QWidget *parent)
    : QDateEdit(parent)
{
    setMinimumDate(QDate(100, 1, 1));
    setDisplayFormat(tr("MM/dd/yyyy"));
    setCalendarPopup(true);
}

void DateEdit::keyPressEvent(QKeyEvent *event) {
    if (event->key() == Qt::Key_Down && event->modifiers() == Qt::CTRL) {
        QStyleOptionSpinBox opt;
        initStyleOption(&opt);
        opt.subControls = QStyle::SC_All;
        auto point = style()->subControlRect(QStyle::CC_SpinBox, &opt, QStyle::SC_SpinBoxDown, this).center();
        QMouseEvent event(QEvent::MouseButtonPress, point, point, Qt::LeftButton, Qt::LeftButton, Qt::NoModifier);
        mousePressEvent(&event);
    }
    else QDateEdit::keyPressEvent(event);
}
