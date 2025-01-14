#include "statuswindow.h"

StatusWindow::StatusWindow() : QMainWindow{} {
    setStatusBar(&statusBar);
}

void StatusWindow::enableUi() {
    if (centralWidget()) centralWidget()->setEnabled(true);
    statusBar.clear();
}
