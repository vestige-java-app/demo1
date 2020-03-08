package fr.gaellalire.vestige_app.demo1.action.unparker;

import java.io.IOException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 *
 * @author Gael Lalire
 */
public class SelectUnparker implements Runnable {

    private static AtomicInteger selectUnparkerInitNumber = new AtomicInteger(1);

    private Selector selector;

    private boolean stopped;

    private Thread thread;

    /**
     *
     * @author Gael Lalire
     */
    private static class Key {

        private Action firstReadAction;

        private Action firstWriteAction;

        private Action firstAcceptAction;

        private Action firstConnectAction;

    }

    /**
     *
     * @author Gael Lalire
     */
    private class Action implements UnparkerAction {

        // 0 normal, 1 pending, 2 cancelled+pending, 3 queued, 4 cancelled+queued
        private int state;

        private SelectableChannel selectableChannel;

        private int ops;

        private NotificationHandler notificationHandler;

        private Action next;

        public void perform(final SelectableChannel selectableChannel, final int ops, final NotificationHandler notificationHandler) {
            this.selectableChannel = selectableChannel;
            this.notificationHandler = notificationHandler;
            this.ops = ops;
            synchronized (actionMutex) {
                if (state == 0) {
                    if (lastAction == null) {
                        lastAction = this;
                        firstAction = this;
                    } else {
                        lastAction.next = this;
                        lastAction = this;
                    }
                    this.next = null;
                    state = 1;
                } else if (state == 2) {
                    state = 1;
                }
            }
            selector.wakeup();
        }

        public void cancel() {
            synchronized (actionMutex) {
                if (state == 1) {
                    state = 2;
                } else if (state == 3) {
                    state = 4;
                }
            }
        }

    }

    private CancelSelectionKeyAction firstCancelSelectionKey;

    /**
     *
     * @author Gael Lalire
     */
    private class CancelSelectionKeyAction implements UnparkerAction {

        private boolean queued;

        private boolean cancelled;

        private CancelSelectionKeyAction next;

        private SelectableChannel selectableChannel;

        public void perform(final SelectableChannel selectableChannel, final int ops, final NotificationHandler notificationHandler) {
            this.selectableChannel = selectableChannel;
            synchronized (actionMutex) {
                if (!queued) {
                    this.next = firstCancelSelectionKey;
                    firstCancelSelectionKey = this;
                    queued = true;
                }
                cancelled = false;
            }
        }

        public void cancel() {
            synchronized (actionMutex) {
                cancelled = true;
            }
        }

    }

    private Object actionMutex = new Object();

    private Action firstAction;

    private Action lastAction;

    public SelectUnparker() {
    }

    public void start(final ThreadFactory threadFactory) throws IOException {
        if (selector == null) {
            selector = Selector.open();
        }
        thread = threadFactory.newThread(this);
        thread.setName("SelectUnparker-" + selectUnparkerInitNumber.getAndIncrement());
        stopped = false;
        thread.start();
    }

    public void stop() throws InterruptedException {
        stopped = true;
        thread.interrupt();
        thread.join();
    }

    public void close() throws IOException {
        selector.close();
    }

    public UnparkerAction prepareUnparkerAction() {
        return new Action();
    }

    public void run() {
        try {
            Set<SelectionKey> selectedKeys = Collections.emptySet();
            while (!stopped) {
                synchronized (actionMutex) {
                    Iterator<SelectionKey> iterator = selectedKeys.iterator();
                    selectionKeyLoop: while (iterator.hasNext()) {
                        SelectionKey selectionKey = iterator.next();
                        CancelSelectionKeyAction cancelSelectionKey = firstCancelSelectionKey;
                        while (cancelSelectionKey != null) {
                            if (!cancelSelectionKey.cancelled) {
                                if (cancelSelectionKey.selectableChannel == selectionKey.channel()) {
                                    selectionKey.cancel();
                                    continue selectionKeyLoop;
                                }
                            }
                            cancelSelectionKey = cancelSelectionKey.next;
                        }

                        int readyOps = selectionKey.readyOps();
                        Key key = (Key) selectionKey.attachment();
                        if (selectionKey.isReadable()) {
                            Action action = key.firstReadAction;
                            while (action != null) {
                                if (action.state == 3) {
                                    action.notificationHandler.handle();
                                }
                                action.state = 0;
                                action = action.next;
                            }
                            key.firstReadAction = null;
                        }
                        if (selectionKey.isWritable()) {
                            Action action = key.firstWriteAction;
                            while (action != null) {
                                if (action.state == 3) {
                                    action.notificationHandler.handle();
                                }
                                action.state = 0;
                                action = action.next;
                            }
                            key.firstWriteAction = null;
                        }
                        if (selectionKey.isConnectable()) {
                            Action action = key.firstConnectAction;
                            while (action != null) {
                                if (action.state == 3) {
                                    action.notificationHandler.handle();
                                }
                                action.state = 0;
                                action = action.next;
                            }
                            key.firstConnectAction = null;
                        }
                        if (selectionKey.isAcceptable()) {
                            Action action = key.firstAcceptAction;
                            while (action != null) {
                                if (action.state == 3) {
                                    action.notificationHandler.handle();
                                }
                                action.state = 0;
                                action = action.next;
                            }
                            key.firstAcceptAction = null;
                        }
                        int interestOps = selectionKey.interestOps();
                        int ops = interestOps - (readyOps & interestOps);
                        selectionKey.interestOps(ops);
                        // clear ready set
                        iterator.remove();
                    }
                    CancelSelectionKeyAction cancelSelectionKey = firstCancelSelectionKey;
                    while (cancelSelectionKey != null) {
                        cancelSelectionKey.queued = false;
                        cancelSelectionKey = cancelSelectionKey.next;
                    }
                    firstCancelSelectionKey = null;
                    if (lastAction != null) {
                        Action currentAction = firstAction;
                        while (currentAction != null) {
                            Action nextAction = currentAction.next;
                            if (currentAction.state == 1) {
                                SelectionKey selectionKey = currentAction.selectableChannel.keyFor(selector);
                                Key key;
                                if (selectionKey == null) {
                                    key = new Key();
                                    currentAction.selectableChannel.register(selector, currentAction.ops, key);
                                } else {
                                    key = (Key) selectionKey.attachment();
                                    selectionKey.interestOps(selectionKey.interestOps() | currentAction.ops);
                                }
                                Action previousAction;
                                switch (currentAction.ops) {
                                case SelectionKey.OP_READ:
                                    previousAction = key.firstReadAction;
                                    key.firstReadAction = currentAction;
                                    break;
                                case SelectionKey.OP_WRITE:
                                    previousAction = key.firstWriteAction;
                                    key.firstWriteAction = currentAction;
                                    break;
                                case SelectionKey.OP_CONNECT:
                                    previousAction = key.firstConnectAction;
                                    key.firstConnectAction = currentAction;
                                    break;
                                case SelectionKey.OP_ACCEPT:
                                    previousAction = key.firstAcceptAction;
                                    key.firstAcceptAction = currentAction;
                                    break;
                                default:
                                    throw new Error();
                                }
                                currentAction.state = 3;
                                currentAction.next = previousAction;
                            } else if (currentAction.state == 2) {
                                // cancel succeed
                                currentAction.state = 0;
                            }
                            currentAction = nextAction;
                        }
                        firstAction = null;
                        lastAction = null;
                    }
                }
                selector.select();
                selectedKeys = selector.selectedKeys();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
