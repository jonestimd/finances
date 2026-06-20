#ifndef BACKGROUND_H
#define BACKGROUND_H

#include <QWidget>

typedef std::function<void()> Runnable;

void doInBackground(QWidget *source, Runnable task, Runnable onError= nullptr);

#endif // BACKGROUND_H
