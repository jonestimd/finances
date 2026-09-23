#include "appwindow.h"
#include "helpmenu.h"
#include "../finances.h"

#include <QMessageBox>

HelpMenu::HelpMenu(AppWindow* window) : QMenu{tr("&Help"), window}
{
    auto aboutAction = new QAction{tr("&About")};
    connect(aboutAction, SIGNAL(triggered(bool)), this, SLOT(showAboutDialog()));
    addAction(aboutAction);
}

void HelpMenu::showAboutDialog() {
    QMessageBox::about(this, tr("About Finances"), tr("Version: %1").arg(APP_VERSION));
}
