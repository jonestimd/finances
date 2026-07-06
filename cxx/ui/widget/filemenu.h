#ifndef FILEMENU_H
#define FILEMENU_H

#include "ui/widget/appwindow.h"
#include <QMenu>

class FileMenu : public QMenu {
public:
    FileMenu(AppWindow* window);
};

#endif // FILEMENU_H