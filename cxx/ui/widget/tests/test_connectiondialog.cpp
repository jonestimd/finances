#include "service/tests/dbtestcase.h"
#include "tests/uitest.h"
#include "ui/widget/accountswindow.h"
#include "ui/widget/connectiondialog.h"
#include <QCheckBox>
#include <QDialogButtonBox>
#include <QFileDialog>
#include <QProcess>
#include <QPushButton>
#include <QSignalSpy>
#include <QSqlError>
#include <QTest>
#include <QTimer>

#define LOCALHOST "127.0.0.1"

#define SQLITE_DB_FILE "testsqlite.dbfin"

#define SCHEMA_INPUT "schema"
#define TEST_SCHEMA "create_test"
#define OPEN_BUTTON_ROLE QDialogButtonBox::AcceptRole

static QString randomString(int length) {
    static const char charset[] = "!@#abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ1234567890$%^&()";
    QString result;
    result.resize(length);

    srand(time(nullptr));
    for (int i = 0; i < length; i++) result[i] = charset[rand() % (sizeof(charset)-1)];

    return result;
}

static QString getMysqlPassword() {
    if (qEnvironmentVariableIsSet("MYSQL_ROOT_PASSWORD")) return qEnvironmentVariable("MYSQL_ROOT_PASSWORD");
    QProcess docker{};
    docker.start("docker", {"logs", qEnvironmentVariable("TEST_DOCKER_MYSQL", "mysql-finances-test")});
    Q_ASSERT(docker.waitForFinished(2000));

    auto output = docker.readAllStandardOutput();
    using enum QRegularExpression::PatternOption;
    auto match = QRegularExpression{"GENERATED ROOT PASSWORD: (.*)$", MultilineOption}.match(output);
    Q_ASSERT(match.hasCaptured(1));
    return match.captured(1);
}

class TestConnectionDialog : public QObject {
    Q_OBJECT
    const QString pgSocketPath = qEnvironmentVariable("TEST_PG_SOCKET_PATH", "/var/run/postgresql")
            .replace(QRegularExpression{"^~"}, qEnvironmentVariable("HOME"));
    const QString mysqlSocketPath = qEnvironmentVariable("TEST_MYSQL_SOCKET_PATH")
            .replace(QRegularExpression{"^~"}, qEnvironmentVariable("HOME"));
    const QString testPassword{randomString(20)};
    const QString mySqlPassword{getMysqlPassword()};
    int pgPort{DbTestCase::port(PG_DRIVER)};
    int mysqlPort{DbTestCase::port(MYSQL_DRIVER)};

    void selectDbType(ConnectionDialog* dialog, const QString name) {
        dialog->findChild<QComboBox*>()->setCurrentText(name);
    }

    QAbstractButton* getButton(ConnectionDialog* dialog, QDialogButtonBox::ButtonRole role) {
        auto buttonBox = dialog->findChild<QDialogButtonBox*>();
        const auto buttons = buttonBox->buttons();
        for (auto button : buttons) {
            if (buttonBox->buttonRole(button) == role) return button;
        }
        return nullptr;
    }

    QLineEdit* getInput(ConnectionDialog* dialog, const QString name) {
        return dialog->findChild<QLineEdit*>(name);
    }

    void setInputText(ConnectionDialog* dialog, const QString inputName, const QString text) {
        auto input = getInput(dialog, inputName);
        QVERIFY(input->isVisible());
        input->setText(text);
    }

    void clickButton(ConnectionDialog* dialog, QDialogButtonBox::ButtonRole role) {
        auto button = getButton(dialog, role);
        QVERIFY(button->isEnabled());
        button->click();
    }

    void selectFile(ConnectionDialog* dialog, const QString& name) {
        auto fileInput = dialog->findChild<QLineEdit*>(SCHEMA_INPUT);
        auto openAction = fileInput->actions().constFirst();
        QTimer::singleShot(0, dialog, [&]() {
            auto fileDialog = uitest::findWindow<QFileDialog>();
            QVERIFY(fileDialog);
            auto fileDialogInput = fileDialog->findChild<QLineEdit*>();
            QVERIFY(fileDialogInput);
            fileDialogInput->setText(name);
            auto buttonBox = fileDialog->findChild<QDialogButtonBox*>();
            auto saveButton = buttonBox->button(QDialogButtonBox::Save);
            saveButton->click();
        });
        openAction->trigger();
    }

    void answerConfirmReplace(ConnectionDialog* dialog, QMessageBox::ButtonRole answer) {
        QTimer::singleShot(0, dialog, [&]() {
            auto messageDialog = uitest::findWindow<QMessageBox>();
            QVERIFY(messageDialog);
            for (auto button : messageDialog->buttons()) {
                if (messageDialog->buttonRole(button) == answer) button->click();
            }
        });
        clickButton(dialog, CREATE_BUTTON_ROLE);
    }

    void createEmptySqliteFile() {
        QFile dbfile{SQLITE_DB_FILE};
        QVERIFY(dbfile.open(QFile::WriteOnly));
        dbfile.close();
    }

private slots:
    void initTestCase_data() {
        uitest::setConfigHome();
    }

    void init() {
        QFile dbfile{SQLITE_DB_FILE};
        if (dbfile.exists()) QVERIFY(dbfile.remove());
    }

    void openOrCreate_defaultsToCreate() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};

        dialog.show();

        QVERIFY(dialog.findChild<QCheckBox*>()->isChecked());
        QVERIFY(dialog.findChild<QLineEdit*>("adminUser")->isVisible());
        QVERIFY(dialog.findChild<QLineEdit*>("adminPassword")->isVisible());
        QVERIFY(dialog.findChild<QLineEdit*>("adminSocket")->isVisible());
        QVERIFY(!getButton(&dialog, OPEN_BUTTON_ROLE)->isVisible());
        QVERIFY(getButton(&dialog, CREATE_BUTTON_ROLE)->isVisible());
        dialog.close();
    }

    void openOrCreate_hidesAdminInputsForOpen() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};

        dialog.show();
        dialog.findChild<QCheckBox*>()->click();

        QVERIFY(!dialog.findChild<QLineEdit*>("adminUser")->isVisible());
        QVERIFY(!dialog.findChild<QLineEdit*>("adminPassword")->isVisible());
        QVERIFY(!dialog.findChild<QLineEdit*>("adminSocket")->isVisible());
        QVERIFY(getButton(&dialog, OPEN_BUTTON_ROLE)->isVisible());
        QVERIFY(!getButton(&dialog, CREATE_BUTTON_ROLE)->isVisible());
        dialog.close();
    }

    void selectSqliteFile_appendsExtension() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::Create};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());
        QVERIFY(!dialog.findChild<QCheckBox*>());

        dialog.show();
        selectDbType(&dialog, SQLITE_OPTION);
        selectFile(&dialog, "dbname");

        QCOMPARE(dialog.findChild<QLineEdit*>(SCHEMA_INPUT)->text(), "dbname.dbfin");
    }

    void selectSqliteFile_retainsExtension() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::Create};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());

        dialog.show();
        selectDbType(&dialog, SQLITE_OPTION);
        selectFile(&dialog, "dbname.db");

        QCOMPARE(dialog.findChild<QLineEdit*>(SCHEMA_INPUT)->text(), "dbname.db");
    }

    void confirmReplaceSqliteFile() {
        createEmptySqliteFile();
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());

        dialog.show();
        selectDbType(&dialog, SQLITE_OPTION);
        setInputText(&dialog, SCHEMA_INPUT, SQLITE_DB_FILE);
        answerConfirmReplace(&dialog, QMessageBox::YesRole);
        QVERIFY(acceptedSpy.wait());

        auto accountsWindow = uitest::findWindow<AccountsWindow>();
        QVERIFY(accountsWindow);
        QVERIFY(!dialog.isVisible());
        accountsWindow->close();
        QVERIFY(QFile{SQLITE_DB_FILE}.exists());
    }

    void cancelReplaceSqliteFile() {
        createEmptySqliteFile();
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};

        dialog.show();
        selectDbType(&dialog, SQLITE_OPTION);
        setInputText(&dialog, SCHEMA_INPUT, SQLITE_DB_FILE);
        answerConfirmReplace(&dialog, QMessageBox::NoRole);

        QVERIFY(dialog.isVisible());
        QVERIFY(!uitest::findWindow<AccountsWindow>());
        dialog.close();
        QCOMPARE(QFile{SQLITE_DB_FILE}.size(), 0);
    }

    void createSqlite() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());

        dialog.show();
        selectDbType(&dialog, SQLITE_OPTION);
        setInputText(&dialog, SCHEMA_INPUT, SQLITE_DB_FILE);
        QCOMPARE(getButton(&dialog, TEST_BUTTON_ROLE)->isEnabled(), false);
        QCOMPARE(getButton(&dialog, OPEN_BUTTON_ROLE)->isEnabled(), true);
        clickButton(&dialog, CREATE_BUTTON_ROLE);
        QVERIFY(acceptedSpy.wait());

        auto accountsWindow = uitest::findWindow<AccountsWindow>();
        QVERIFY(accountsWindow);
        QVERIFY(!dialog.isVisible());
        accountsWindow->close();
        QVERIFY(QFile{SQLITE_DB_FILE}.exists());
    }

    void init_createPostgres() {
        ConnectionSettings setting{PG_DRIVER, pgSocketPath, pgPort, PG_ROOT_SCHEMA, PG_ROOT_USER, ""};
        auto db = setting.connect();
        QSqlQuery query{db};
        query.exec("drop database " TEST_SCHEMA);
        query.exec("drop user " TEST_SCHEMA);
        query.exec("drop role " TEST_SCHEMA "_rw");
    }

    void createPostgres() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());

        dialog.show();
        selectDbType(&dialog, PG_OPTION);
        setInputText(&dialog, "host", LOCALHOST);
        setInputText(&dialog, "port", QString::number(pgPort));
        setInputText(&dialog, "user", TEST_SCHEMA);
        setInputText(&dialog, "password", testPassword);
        setInputText(&dialog, SCHEMA_INPUT, TEST_SCHEMA);
        QCOMPARE(getInput(&dialog, "adminUser")->text(), PG_ROOT_USER);
        QCOMPARE(getButton(&dialog, TEST_BUTTON_ROLE)->isEnabled(), true);
        QCOMPARE(getButton(&dialog, CREATE_BUTTON_ROLE)->isEnabled(), true);
        setInputText(&dialog, "adminSocket", pgSocketPath);
        clickButton(&dialog, CREATE_BUTTON_ROLE);
        QVERIFY(acceptedSpy.wait());

        auto accountsWindow = uitest::findWindow<AccountsWindow>();
        QVERIFY(accountsWindow);
        QVERIFY(!dialog.isVisible());
        accountsWindow->close();
    }

    void init_createMysql() {
        ConnectionSettings setting{MYSQL_DRIVER, LOCALHOST, mysqlPort, MYSQL_ROOT_SCHEMA, MYSQL_ROOT_USER, mySqlPassword};
        auto db = setting.connect();
        QSqlQuery query{db};
        query.exec("drop database " TEST_SCHEMA);
        query.exec("drop user " TEST_SCHEMA);
        query.exec("drop role " TEST_SCHEMA "_rw");
    }

    void createMysql() {
        ConnectionDialog dialog{nullptr, ConnectionDialog::OpenOrCreate};
        QSignalSpy acceptedSpy{&dialog, SIGNAL(accepted())};
        QVERIFY(acceptedSpy.isValid());

        dialog.show();
        selectDbType(&dialog, MYSQL_OPTION);
        setInputText(&dialog, "host", LOCALHOST);
        setInputText(&dialog, "port", QString::number(mysqlPort));
        setInputText(&dialog, "user", TEST_SCHEMA);
        setInputText(&dialog, "password", testPassword);
        setInputText(&dialog, SCHEMA_INPUT, TEST_SCHEMA);
        QCOMPARE(getInput(&dialog, "adminUser")->text(), MYSQL_ROOT_USER);
        QCOMPARE(getButton(&dialog, TEST_BUTTON_ROLE)->isEnabled(), true);
        QCOMPARE(getButton(&dialog, CREATE_BUTTON_ROLE)->isEnabled(), true);
        setInputText(&dialog, "adminPassword", mySqlPassword);
        setInputText(&dialog, "adminSocket", mysqlSocketPath);
        clickButton(&dialog, CREATE_BUTTON_ROLE);
        QVERIFY(acceptedSpy.wait());

        auto accountsWindow = uitest::findWindow<AccountsWindow>();
        QVERIFY(accountsWindow);
        QVERIFY(!dialog.isVisible());
        accountsWindow->close();
    }
};

QTEST_MAIN(TestConnectionDialog)
#include "test_connectiondialog.moc"