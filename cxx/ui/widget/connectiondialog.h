#ifndef CONNECTIONDIALOG_H
#define CONNECTIONDIALOG_H

#include "service/database/connectionpool.h"
#include "ui/model/datastore.h"
#include <QComboBox>
#include <QDialog>
#include <QLabel>
#include <QMenu>

#define MYSQL_OPTION "MySQL"
#define PG_OPTION "PostgreSQL"
#define SQLITE_OPTION "SQLite3"

#define TEST_BUTTON_ROLE QDialogButtonBox::ApplyRole
#define CREATE_BUTTON_ROLE QDialogButtonBox::YesRole

class ConnectionDialog : public QDialog {
    Q_OBJECT
public:
    enum Mode {
        Open = 1,
        Create = 2,
        OpenOrCreate = 3,
    };

private:
    bool create;
    QComboBox typeInput;
    QPushButton *testButton{};
    QPushButton *openButton{};
    QPushButton *createButton{};
    QLineEdit *adminUserInput{};
    QLabel status{};
    AdminConnectionSettings settings;
    bool replaceConfirmed{false};

public:
    ConnectionDialog(QWidget *parent = nullptr, Mode mode = Mode::Open);

    QString getStatus();

private slots:
    void testConnection();
    void modeChanged(bool create);
    void typeChanged(const QString& value);
    void inputChanged(QWidget* widget);
    void setStatus(const QString message);
    void createDatabase();
    void openDatabase();

private:
    Q_SIGNAL void statusChanged(const QString message);

    void handleOpenResult(DataStore* dataStore, const QString& error);

    template<class Settings, class Value>
    QLineEdit* connectInput(QLineEdit* input, Value Settings::*field, const QString& name = "");
};

#endif // CONNECTIONDIALOG_H
