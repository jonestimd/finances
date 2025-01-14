#ifndef STATUSWINDOW_H
#define STATUSWINDOW_H

#include "statusbar.h"
#include <QMainWindow>

class StatusWindow : public QMainWindow {
    Q_OBJECT

protected:
    StatusBar statusBar{};

public:
    explicit StatusWindow();

    Q_INVOKABLE void enableUi();
};

#endif // STATUSWINDOW_H
