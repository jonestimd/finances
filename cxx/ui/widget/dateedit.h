#ifndef DATEEDIT_H
#define DATEEDIT_H

#include <QDateEdit>

class DateEdit : public QDateEdit {
public:
    DateEdit(QWidget *parent);

protected:
    void keyPressEvent(QKeyEvent *event) override;
};

#endif // DATEEDIT_H
