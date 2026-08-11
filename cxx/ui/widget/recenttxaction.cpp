#include "recenttxaction.h"

#include "ui/model/transactiontablemodel.h"
#include <QMenu>
#include <QPointerEvent>

#define SUMMARY_DETAILS 5
#define SUMMARY_SEPARATOR '|'

RecentTxAction::RecentTxAction(QMenu* parent, TransactionTableModel *model, PendingTransaction *transaction, const DataStore *dataStore)
    : QWidgetAction{parent}, model{model}, transaction{transaction}
{
    auto widget = new QLabel{label(transaction, dataStore)};
    widget->setAutoFillBackground(true);
    widget->installEventFilter(this);
    parent->installEventFilter(this);
    setDefaultWidget(widget);
    connect(this, SIGNAL(triggered(bool)), this, SLOT(selected()));
}

RecentTxAction::~RecentTxAction() {
    if (transaction) delete transaction;
}

bool RecentTxAction::eventFilter(QObject *watched, QEvent *event) {
    auto menu = qobject_cast<QMenu*>(watched);
    if (menu) {
        if (event->type() == QEvent::StatusTip) {
            bool selected = menu->activeAction() == this;
            if (selected) defaultWidget()->setBackgroundRole(QPalette::Highlight);
            else defaultWidget()->setBackgroundRole(QPalette::NoRole);
        }
    } else {
        if (event->type() == QEvent::Enter) defaultWidget()->setBackgroundRole(QPalette::Highlight);
        else if (event->type() == QEvent::Leave) defaultWidget()->setBackgroundRole(QPalette::NoRole);
    }
    return QWidgetAction::eventFilter(watched, event);
}

void RecentTxAction::selected() {
    model->replacePendingAdd(transaction);
    transaction = nullptr;
}

QString RecentTxAction::label(PendingTransaction *transaction, const DataStore *dataStore) {
    auto label = QString{"<html><i>%1</i>"}.arg(transaction->memo);
    const auto& details = transaction->details;
    for (auto detail : details.first(std::min<qsizetype>(details.size(), SUMMARY_DETAILS))) {
        if (detail->categoryId.has_value()) label += " <b>" + dataStore->categoryStore->displayName(detail->categoryId.value()) + "</b>";
        else if (detail->transferAccountId.has_value()) label += " &#xf81c;<b>" + dataStore->accountStore->qualifiedName(detail->transferAccountId.value()) + "</b>";
        label += QString{" $%1"}.arg(detail->amount.toString()); // TODO other currencies
        if (detail->assetQuantity.has_value()) label += ' ' + detail->assetQuantity.value().toString();
        label += SUMMARY_SEPARATOR;
    }
    label.removeLast();
    if (details.size() > SUMMARY_DETAILS) label += QString{" ... (%1)"}.arg(details.size() - SUMMARY_DETAILS);
    label += "</html>";
    return label;
}
