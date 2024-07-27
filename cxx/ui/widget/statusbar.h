#ifndef STATUSBAR_H
#define STATUSBAR_H

#include <QStatusBar>

class StatusBar : public QStatusBar
{
    Q_OBJECT
    QStringList messages;
public:
    StatusBar(QWidget *parent = nullptr);

    void addMessage(QString message);
    void removeMessage(QString message);
};

#endif // STATUSBAR_H
