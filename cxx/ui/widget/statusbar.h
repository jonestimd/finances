#ifndef STATUSBAR_H
#define STATUSBAR_H

#include <QStatusBar>

class StatusBar : public QStatusBar {
    Q_OBJECT
    friend class EntityView;
    QStringList messages;

public:
    StatusBar(QWidget *parent = nullptr);

private:
    using QStatusBar::showMessage;

    void addMessage(const QString &message);
    void removeMessage(const QString &message);
    void clear();
    bool isEmpty() const;
};

#endif // STATUSBAR_H
