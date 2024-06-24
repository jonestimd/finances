#ifndef FILTERINPUT_H
#define FILTERINPUT_H

#include "../finances.h"
#include <QLineEdit>
#include <QSortFilterProxyModel>
#include <QToolBar>

class FilterInput : public QLineEdit
{
    Q_OBJECT
    static constexpr QChar icon = QChar{finances::FontIcon::Filter};
protected Q_SLOTS:
    void onTextChanged(const QString &text);
public:
    FilterInput(const char *placeholderText, QSortFilterProxyModel *model, QWidget *parent = nullptr);

Q_SIGNALS:
    void filterChanged(const QRegularExpression &);

    // QWidget interface
protected:
    void paintEvent(QPaintEvent *event) override;
};

#endif // FILTERINPUT_H
