#ifndef WINDOWTEST_H
#define WINDOWTEST_H

#include <QApplication>

namespace windowtest {
    template<class T>
    T* findWindow() {
        const auto windows = QApplication::topLevelWidgets();
        for (auto win : windows) {
            T* window = qobject_cast<T*>(win);
            if (window) return window;
        }
        return nullptr;
    }
}

#endif // WINDOWTEST_H