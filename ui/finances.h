#ifndef FINANCES_H
#define FINANCES_H

#include <QApplication>
#include <QColor>
#include <QSettings>
#include <Qt>

namespace Finances {
    enum ItemDataRole {
        TextHighlight = Qt::UserRole,
        SortRole,
    };

    class App : public QApplication {
        Q_OBJECT
        QString userStyleSheet;
    public:
        QSettings *const settings;

        App(int &argc, char **argv);
    public slots:
        void updateStyleSheet(Qt::ColorScheme scheme);
    };
}

#endif // FINANCES_H
