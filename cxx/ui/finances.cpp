#include "finances.h"
#include <QFile>
#include <QFontDatabase>
#include <QIcon>
#include <QIconEngine>
#include <QPainter>
#include <QPalette>
#include <QStyleHints>
#include <QThreadPool>
#include <QTranslator>
#include <QSqlDatabase>
#include <QProxyStyle>

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
        const QColor color;
    public:
        MaterialIconEngine(FontIcon icon, QColor color) : icon{icon}, color{color} {}

    public:
        void paint(QPainter *painter, const QRect &rect, QIcon::Mode mode, QIcon::State state) override {
            QFont font = iconFont->font();
            font.setPixelSize(qRound(rect.height() * 0.8));
            auto colorGroup = mode == QIcon::Mode::Disabled ? QPalette::Disabled : QPalette::Normal;
            QColor textColor = color.isValid() ? color : QApplication::palette("QWidget").color(colorGroup, QPalette::Text);

            painter->save();
            painter->setPen(textColor);
            painter->setFont(font);
            painter->drawText(rect, Qt::AlignCenter, QChar{icon});
            painter->restore();
        }

        QIconEngine *clone() const override {
            return new MaterialIconEngine(icon, color);
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

    QIcon materialIcon(FontIcon icon, QColor color) {
        return QIcon(new MaterialIconEngine(icon, color));
    }

    QLabel* iconWidget(FontIcon icon, QWidget *parent) {
        auto label = new QLabel(parent);
        label->setFont(iconFont->font(label->font().pointSize() * 2));
        label->setText(QChar{icon});
        return label;
    }

    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QString &tooltip) {
        action->setText(text);
        action->setToolTip(tooltip);
        action->setIcon(materialIcon(icon));
        return action;
    }

    QAction *initAction(QAction *action, FontIcon icon, const QString &text, const QKeySequence &shortcut) {
        QString tooltip(text);
        if (!shortcut.isEmpty()) tooltip.append(" (").append(shortcut.toString()).append(")");
        initAction(action, icon, text, tooltip);
        action->setShortcut(shortcut);
        return action;
    }

    QAction *iconAction(FontIcon icon, const QString &text, QObject *parent) {
        return initAction(new QAction(parent), icon, text, text);
    }

    QAction *iconAction(FontIcon icon, const QString &text, const QKeySequence &shortcut, QObject *parent) {
        return initAction(new QAction(parent), icon, text, shortcut);
    }

    QAction *iconAction(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot, bool enabled) {
        auto action = iconAction(icon, text, QKeySequence(shortcut), receiver);
        action->setEnabled(enabled);
        QObject::connect(action, SIGNAL(triggered(bool)), receiver, slot);
        return action;
    }

    QAction *iconAction(FontIcon icon, const QString &text, QKeySequence::StandardKey shortcut, QObject *receiver, const char *slot, bool enabled) {
        auto action = iconAction(icon, text, QKeySequence(shortcut), receiver);
        action->setEnabled(enabled);
        if (receiver && slot) QObject::connect(action, SIGNAL(triggered(bool)), receiver, slot);
        return action;
    }

    QAction *iconAction(const char *iconFile, const QString &text, QObject *parent) {
        auto action = new QAction(parent);
        action->setText(text);
        action->setToolTip(text);
        action->setIcon(QIcon(iconFile));
        return action;
    }

    QAction *iconToggle(FontIcon icon, const QString &text, const QString &shortcut, QObject *receiver, const char *slot) {
        auto action = iconAction(icon, text, QKeySequence(shortcut), receiver);
        action->setCheckable(true);
        if (receiver && slot) QObject::connect(action, SIGNAL(toggled(bool)), receiver, slot);
        return action;
    }

    class InvokableAction : public QAction {
    public:
        InvokableAction(QWidget *window, const char *invokable, finances::FontIcon icon, const QString &text,
                        const QKeySequence &shortcut, bool enabled = true)
            : QAction{window}
        {
            initAction(this, icon, text, shortcut);
            setEnabled(enabled);
            connect(this, &QAction::triggered, window, [=]() {
                QMetaObject::invokeMethod(window, invokable);
            });
        }
    };

    QAction *saveAction(QWidget *window, const char *invokable) {
        return new InvokableAction(window, invokable, Save, QObject::tr("Save"), QKeySequence::Save, false);
    }

    QAction *reloadAction(QWidget *window, const char *invokable) {
        return new InvokableAction(window, invokable, Refresh, QObject::tr("Reload"), QKeySequence::Refresh);
    }

    void setColumnResize(QHeaderView *viewHeader) {
        if (viewHeader->count() > 2) viewHeader->setStretchLastSection(true);
        else {
            viewHeader->setSectionResizeMode(QHeaderView::ResizeToContents);
            viewHeader->setSectionResizeMode(0, QHeaderView::Stretch);
        }
    }

    class AppStyle : public QProxyStyle {
    public:
        void drawControl(ControlElement element, const QStyleOption *opt, QPainter *p, const QWidget *w) const override {
            if (element == CE_HeaderSection) {
                auto headerOpt = static_cast<const QStyleOptionHeader*>(opt);
                if (headerOpt->text.contains('\n')) {
                    auto lines = headerOpt->text.split('\n');
                    QStyleOptionHeader option(*headerOpt);
                    option.state.setFlag(State_Horizontal, false);
                    auto height = opt->rect.height() / lines.length();
                    option.rect.setHeight(height);
                    p->save();
                    for (const auto &line : std::as_const(lines)) {
                        option.text = line;
                        QProxyStyle::drawControl(element, &option, p, w);
                        option.rect.adjust(0, height, 0, height);
                    }
                    p->restore();
                    return;
                }
            }
            else if (element == CE_HeaderLabel) {
                auto headerOpt = static_cast<const QStyleOptionHeader*>(opt);
                if (headerOpt->text.contains('\n')) {
                    auto lines = headerOpt->text.split('\n');
                    QStyleOptionHeader option(*headerOpt);
                    auto height = opt->rect.height()/2 + opt->rect.top();
                    option.rect.setBottom(height - option.rect.top() - 1);
                    p->save();
                    for (const auto &line : std::as_const(lines)) {
                        option.text = line;
                        QProxyStyle::drawControl(element, &option, p, w);
                        option.rect.adjust(0, height, 0, height);
                    }
                    p->restore();
                    return;
                }
            }
            QProxyStyle::drawControl(element, opt, p, w);
        }

        QSize sizeFromContents(ContentsType ct, const QStyleOption *opt, const QSize &contentsSize, const QWidget *w) const override {
            if (ct == CT_HeaderSection) {
                auto headerOpt = static_cast<const QStyleOptionHeader*>(opt);
                auto lines = headerOpt->text.split('\n');
                if (lines.length() > 1) {
                    QStyleOptionHeader option(*headerOpt);
                    option.text = lines.first();
                    auto size = QProxyStyle::sizeFromContents(ct, &option, contentsSize, w);
                    return QSize(size.width(), size.height()*lines.length());
                }
            }
            return QProxyStyle::sizeFromContents(ct, opt, contentsSize, w);
        }
    };

    App::App(int &argc, char **argv)
        : QApplication(argc, argv),
        userStyleSheet{""},
        settings{new QSettings(QSettings::IniFormat, QSettings::UserScope, "finances", "finances", this)}
    {
        setStyle(new AppStyle()); // NOLINT(clang-analyzer-cplusplus.NewDeleteLeaks)
        setWindowIcon(QIcon(":/images/finances.svg"));
        auto styleFile = styleSheet();
        if (!styleFile.isEmpty()) userStyleSheet = readStyles(styleFile.replace(0, 8, ""));
        updateStyleSheet(styleHints()->colorScheme());
        connect(styleHints(), SIGNAL(colorSchemeChanged(Qt::ColorScheme)), this, SLOT(updateStyleSheet(Qt::ColorScheme)));
        QTranslator translator;
        if (translator.load(QLocale::system(), "finances", "_", ":/i18n")) installTranslator(&translator);

        QThreadPool::globalInstance()->setMaxThreadCount(5);
    }

    App::~App() {
        QThreadPool::globalInstance()->waitForDone();
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
