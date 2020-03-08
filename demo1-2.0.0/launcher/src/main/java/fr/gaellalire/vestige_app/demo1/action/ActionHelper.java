package fr.gaellalire.vestige_app.demo1.action;

import fr.gaellalire.vestige_app.demo1.action.unparker.NotificationHandler;


public interface ActionHelper {

    void setReadyDelay(long nanoTime);

    void setReadyTime(long nanoDeadline);

    NotificationHandler getReadyNotificationHandler();

}
