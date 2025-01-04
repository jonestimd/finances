#ifndef BACKGROUND_H
#define BACKGROUND_H

#include <QtConcurrent>

typedef std::function<void()> Runnable;
typedef std::function<void(bool)> OnComplete;

void doInBackground(QWidget *source, Runnable task, OnComplete onComplete = nullptr);

#endif // BACKGROUND_H
