#ifndef UITEST_H
#define UITEST_H

#include <QApplication>
#include <QWidget>

namespace uitest {
    void setConfigHome();

    template<class T>
    T* findWindow() {
        const auto windows = QApplication::topLevelWidgets();
        for (auto win : windows) {
            if (win->isVisible()) {
                T* window = qobject_cast<T*>(win);
                if (window) return window;
            }
        }
        return nullptr;
    }
}

#endif // UITEST_H
