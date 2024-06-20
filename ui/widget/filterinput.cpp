#include "filterinput.h"

#include "../finances.h"
#include <QRegularExpressionValidator>
#include <QStyle>

FilterInput::FilterInput(
    const char *placeholderText,
    QToolBar *toolbar,
    QSortFilterProxyModel *model) : QLineEdit(toolbar)
{
    setPlaceholderText(tr(placeholderText));
    setClearButtonEnabled(true);

    toolbar->addWidget(iconWidget(Finances::FontIcon::Filter, toolbar));
    toolbar->addWidget(this);

    connect(this, SIGNAL(textChanged(QString)), this, SLOT(onTextChanged(QString)));
    connect(this, SIGNAL(filterChanged(QRegularExpression)),
            model, SLOT(setFilterRegularExpression(QRegularExpression)));
    setProperty("invalid", false);
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
