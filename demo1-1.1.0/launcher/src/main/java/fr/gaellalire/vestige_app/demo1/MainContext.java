package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.ThreadFactory;

import fr.gaellalire.vestige_app.demo1.AcceptAction.AcceptActionContext;
import fr.gaellalire.vestige_app.demo1.action.ActionContext;
import fr.gaellalire.vestige_app.demo1.action.unparker.SelectUnparker;
import fr.gaellalire.vestige_app.demo1.action.unparker.UnparkerAction;

public class MainContext implements AcceptActionContext, ActionContext {

    SelectUnparker selectUnparker;

    ServerSocketChannel serverSocketChannel;

    IOException exception;

    SocketChannel acceptedSocketChannel;

    UnparkerAction unparkerAction;

    String welcomePrefix;

    File latestConnectionFile;

    public MainContext(final ServerSocketChannel serverSocketChannel, final SelectUnparker selectUnparker, final String welcomePrefix, final File latestConnectionFile) {
        this.serverSocketChannel = serverSocketChannel;
        this.selectUnparker = selectUnparker;
        unparkerAction = selectUnparker.prepareUnparkerAction();
        this.welcomePrefix = welcomePrefix;
        this.latestConnectionFile = latestConnectionFile;
    }

    public ServerSocketChannel getServerSocketChannel() {
        return serverSocketChannel;
    }

    public void setIOException(final IOException exception) {
        this.exception = exception;
    }

    public IOException getIOException() {
        return exception;
    }

    public void setAcceptedSocketChannel(final SocketChannel socketChannel) {
        this.acceptedSocketChannel = socketChannel;
    }

    public UnparkerAction getUnparkerAction() {
        return unparkerAction;
    }

    public SelectUnparker getSelectUnparker() {
        return selectUnparker;
    }

    public String getWelcomePrefix() {
        return welcomePrefix;
    }

    public int getByteBufferSize() {
        return 1024;
    }

    public void close() {
        try {
            selectUnparker.stop();
            serverSocketChannel.close();
            selectUnparker.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void pause() {
        try {
            selectUnparker.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void resume() {
        try {
            selectUnparker.start(new ThreadFactory() {

                public Thread newThread(final Runnable r) {
                    return new Thread(r);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
