#ifndef FILTERINPUT_H
#define FILTERINPUT_H

#include <QLineEdit>
#include <QSortFilterProxyModel>
#include <QToolBar>

class FilterInput : public QLineEdit
{
    Q_OBJECT
protected Q_SLOTS:
    void onTextChanged(const QString &text);
public:
    FilterInput(const char *placeholderText, QToolBar *toolbar, QSortFilterProxyModel *model);

Q_SIGNALS:
    void filterChanged(const QRegularExpression &);
};

#endif // FILTERINPUT_H
