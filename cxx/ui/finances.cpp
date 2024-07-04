#include "finances.h"
#include <QFile>
#include <QFontDatabase>
#include <QIcon>
#include <QIconEngine>
#include <QPainter>
#include <QPalette>
#include <QStyleHints>
#include <QTranslator>

QString readStyles(const QString &fileName) {
    QFile file(fileName);
    file.open(QFile::ReadOnly);
    return QString(file.readAll());
}

namespace finances {
    FontResource::FontResource(const char *fileName, const char *style) : style{style} {
        fontId = QFontDatabase::addApplicationFont(fileName);
        auto fontFamilies = QFontDatabase::applicationFontFamilies(fontId);
        family = fontFamilies.first();
        // qDebug() << "Finances:" << fontFamilies;
        // qDebug() << "Finances:" << QFontDatabase::styles(family);
    }

    FontResource::~FontResource() {
        QFontDatabase::removeApplicationFont(fontId);
    }

    QFont FontResource::font() {
        return QFont(family);
    }

    QFont FontResource::font(int pointSize) {
        return QFontDatabase::font(family, style, pointSize);
    }

    class MaterialIconEngine : public QIconEngine {
        const FontIcon icon;
    public:
        MaterialIconEngine(FontIcon icon) : icon{icon} {}

    public:
        void paint(QPainter *painter, const QRect &rect, QIcon::Mode mode, QIcon::State state) override {
            QFont font = iconFont->font();
            font.setPixelSize(qRound(rect.height() * 0.8));
            auto colorGroup = mode == QIcon::Mode::Disabled ? QPalette::Disabled : QPalette::Normal;
            QColor textColor = QApplication::palette("QWidget").color(colorGroup, QPalette::ButtonText);;

            painter->save();
            painter->setPen(textColor);
            painter->setFont(font);
            painter->drawText(rect, Qt::AlignCenter, QChar{icon});
            painter->restore();
        }

        QIconEngine *clone() const override {
            return new MaterialIconEngine(icon);
        }

        QPixmap pixmap(const QSize &size, QIcon::Mode mode, QIcon::State state) override {
            QImage image(size, QImage::Format_ARGB32);
            image.fill(qRgba(0, 0, 0, 0));
            QPixmap pix = QPixmap::fromImage(image, Qt::NoFormatConversion);
            QPainter painter(&pix);
            paint(&painter, QRect(QPoint(), size), mode, state);
            return pix;
        }
    };

    QIcon qIcon(FontIcon icon) {
        return QIcon(new MaterialIconEngine(icon)); // TODO memory leak?
    }

    QLabel* iconWidget(FontIcon icon, QWidget *parent) {
        auto label = new QLabel(parent);
        label->setFont(iconFont->font(label->font().pointSize() * 2));
        label->setText(QChar{icon});
        return label;
    }

    QAction *iconAction(FontIcon icon, QString text, QObject *parent) {
        auto action = new QAction(parent);
        action->setText(text);
        action->setToolTip(text);
        action->setIcon(qIcon(icon));
        return action;
    }

    QAction *iconAction(FontIcon icon, QString text, QKeySequence shortcut, QObject *parent) {
        auto action = iconAction(icon, text, parent);
        action->setShortcut(shortcut);
        return action;
    }

    QAction *iconAction(FontIcon icon, QString text, QKeySequence::StandardKey shortcut, QObject *parent) {
        return iconAction(icon, text, QKeySequence(shortcut), parent);
    }

    QAction *iconAction(FontIcon icon, QString text, QKeySequence::StandardKey shortcut, QObject *receiver, const char *member, bool enabled) {
        auto action = iconAction(icon, text, shortcut, receiver);
        action->setEnabled(enabled);
        QObject::connect(action, SIGNAL(triggered(bool)), receiver, member);
        return action;
    }

    QAction *iconAction(FontIcon icon, QString text, QString shortcut, QObject *parent) {
        return iconAction(icon, text, QKeySequence(shortcut), parent);
    }

    QAction *iconAction(const char *iconFile, QString text, QObject *parent) {
        auto action = new QAction(parent);
        action->setText(text);
        action->setToolTip(text);
        action->setIcon(QIcon(iconFile));
        return action;
    }

    QAction *iconAction(const char *iconFile, QString text, QString shortcut, QObject *parent) {
        auto action = iconAction(iconFile, text, parent);
        action->setShortcut(QKeySequence(shortcut));
        return action;
    }

    App::App(int &argc, char **argv)
        : QApplication(argc, argv),
        userStyleSheet{""},
        settings{new QSettings(QSettings::IniFormat, QSettings::UserScope, "finances", "finances", this)}
    {
        setWindowIcon(QIcon(":/images/finances.svg"));
        auto styleFile = styleSheet();
        if (!styleFile.isEmpty()) userStyleSheet = readStyles(styleFile.replace(0, 8, ""));
        updateStyleSheet(styleHints()->colorScheme());
        connect(styleHints(), SIGNAL(colorSchemeChanged(Qt::ColorScheme)), this, SLOT(updateStyleSheet(Qt::ColorScheme)));
        QTranslator translator;
        if (translator.load(QLocale::system(), "finances", "_", ":/i18n")) installTranslator(&translator);
    }

    void App::updateStyleSheet(Qt::ColorScheme scheme) {
        if (scheme == Qt::ColorScheme::Dark) {
            auto styles = readStyles(":/styles/finances.qss");
            setStyleSheet(styles + "\n" + userStyleSheet);
        }
        else if (scheme == Qt::ColorScheme::Light) {
            auto styles = readStyles(":/styles/minimal-light.qss");
            setStyleSheet(styles + "\n" + userStyleSheet);
        }
    }
}
