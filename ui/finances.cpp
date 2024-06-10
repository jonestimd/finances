#include "finances.h"
#include <QFile>
#include <QIcon>
#include <QPalette>
#include <QStyleHints>

namespace Finances {
    App::App(int &argc, char **argv)
        : QApplication(argc, argv),
        userStyleSheet{""},
        settings{new QSettings(QSettings::IniFormat, QSettings::UserScope, "finances", "finances", this)}
    {
        setWindowIcon(QIcon(":/images/finances.svg"));
        auto styleFile = styleSheet();
        if (!styleFile.isEmpty()) {
            QFile file(styleFile.replace(0, 8, ""));
            file.open(QFile::ReadOnly);
            userStyleSheet = QLatin1StringView(file.readAll());
        }
        updateStyleSheet(styleHints()->colorScheme());
        connect(styleHints(), SIGNAL(colorSchemeChanged(Qt::ColorScheme)), this, SLOT(updateStyleSheet(Qt::ColorScheme)));
    }

    void App::updateStyleSheet(Qt::ColorScheme scheme) {
        if (scheme == Qt::ColorScheme::Dark) {
            setStyleSheet("QTableView {accent-color: rgb(255, 160, 160)}\n" + userStyleSheet);
        }
        else if (scheme == Qt::ColorScheme::Light) {
            setStyleSheet("QTableView {accent-color: rgb(255, 0, 0)}\n" + userStyleSheet);
        }
    }
}
