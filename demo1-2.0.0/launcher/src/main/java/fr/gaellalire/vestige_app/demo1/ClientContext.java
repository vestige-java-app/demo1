package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import fr.gaellalire.vestige_app.demo1.ReadAction.ReadActionContext;
import fr.gaellalire.vestige_app.demo1.WriteAction.WriteActionContext;
import fr.gaellalire.vestige_app.demo1.action.ActionContext;
import fr.gaellalire.vestige_app.demo1.action.unparker.UnparkerAction;

public class ClientContext implements WriteActionContext, ReadActionContext, ActionContext {

    boolean readerWaiting;

    SocketChannel socketChannel;

    ByteBuffer readByteBuffer;

    ByteBuffer writeByteBuffer;

    int readBytes;

    UnparkerAction readUnparkerAction;

    UnparkerAction writeUnparkerAction;

    int writtenBytes;

    IOException ioException;

    String welcomePrefix;

    File latestConnectionFile;

    public SocketChannel getSocketChannel() {
        return socketChannel;
    }

    public ByteBuffer getReadByteBuffer() {
        return readByteBuffer;
    }

    public ByteBuffer getWriteByteBuffer() {
        return writeByteBuffer;
    }

    public void setWrittenBytes(final int writtenBytes) {
        this.writtenBytes = writtenBytes;
    }

    public void setIOException(final IOException exception) {
        this.ioException = exception;
    }

    public void setReadBytes(final int readBytes) {
        this.readBytes = readBytes;
    }

    public UnparkerAction getReadUnparkerAction() {
        return readUnparkerAction;
    }

    public UnparkerAction getWriteUnparkerAction() {
        return writeUnparkerAction;
    }

    public File getLatestConnectionFile() {
        return latestConnectionFile;
    }

    public void close() {
        try {
            socketChannel.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void pause() {
    }

    public void resume() {
    }

}
