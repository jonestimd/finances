#ifndef HELP_MENU_H
#define HELP_MENU_H

#include <QMenu>

class AppWindow;

class HelpMenu : public QMenu {
    Q_OBJECT
public:
    HelpMenu(AppWindow* window);

private Q_SLOTS:
    void showAboutDialog();
};

#endif // HELP_MENU_H