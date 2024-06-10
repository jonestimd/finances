#include "styleproxy.h"

#include <QStyleOptionViewItem>

StyleProxy::StyleProxy(QObject *parent) : QProxyStyle() {
    setParent(parent);
}

QRect StyleProxy::subElementRect(QStyle::SubElement element, const QStyleOption *option, const QWidget *widget) const {
        const QRect baseRes = QProxyStyle::subElementRect(element, option, widget);
        if (element == SE_ItemViewItemCheckIndicator) {
            const QStyleOptionViewItem* const itemOpt = qstyleoption_cast<const QStyleOptionViewItem*>(option) ;
            if (itemOpt->index.data(Qt::CheckStateRole).isValid() && !itemOpt->index.data(Qt::DisplayRole).isValid()) {
                const QRect itemRect = option->rect;
                int x = itemRect.x() + itemRect.width()/2 - baseRes.width()/2;
                return QRect(QPoint(x, baseRes.y()), baseRes.size());
            }
        }
        if (element == SE_ItemViewItemFocusRect) {
            const QStyleOptionViewItem* const itemOpt = qstyleoption_cast<const QStyleOptionViewItem*>(option) ;
            if (itemOpt->index.data(Qt::CheckStateRole).isValid() && !itemOpt->index.data(Qt::DisplayRole).isValid()) {
                return option->rect;
            }
        }
        return baseRes;
};
