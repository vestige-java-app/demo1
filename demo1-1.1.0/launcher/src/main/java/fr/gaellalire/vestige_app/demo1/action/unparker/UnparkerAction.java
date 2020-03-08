package fr.gaellalire.vestige_app.demo1.action.unparker;

import java.nio.channels.SelectableChannel;

/**
 * @author gaellalire
 */
public interface UnparkerAction {

    int OP_READ = 1 << 0;

    int OP_WRITE = 1 << 2;

    int OP_CONNECT = 1 << 3;

    int OP_ACCEPT = 1 << 4;

    int OP_CANCEL = 1 << 5;

    void perform(SelectableChannel selectableChannel, int ops, NotificationHandler notificationHandler);

    void cancel();

}
