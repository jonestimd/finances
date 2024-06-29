#ifndef STYLEPROXY_H
#define STYLEPROXY_H

#include <QProxyStyle>

class StyleProxy : public QProxyStyle
{
public:
    StyleProxy();

    // QStyle interface
public:
    QRect subElementRect(SubElement subElement, const QStyleOption *option, const QWidget *widget) const override;
};

#endif // STYLEPROXY_H
