package fr.gaellalire.vestige_app.demo1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import fr.gaellalire.vestige_app.demo1.action.CompositeAction;
import fr.gaellalire.vestige_app.demo1.action.CompositeActionHelper;
import fr.gaellalire.vestige_app.demo1.action.Condition;
import fr.gaellalire.vestige_app.demo1.action.Promise;
import fr.gaellalire.vestige_app.demo1.action.SimpleAction;

public class EchoServer implements CompositeAction<ClientContext> {

    ReadAction readAction;

    WriteAction writeAction;

    public EchoServer(final ReadAction readAction, final WriteAction writeAction) {
        this.readAction = readAction;
        this.writeAction = writeAction;
    }

    public void describe(final CompositeActionHelper<ClientContext> compositeActionHelper) {
        Promise<ClientContext> welcomeAction = compositeActionHelper.simpleAction(new SimpleAction<ClientContext>() {

            public void perform(final ClientContext context) {
                long date = System.currentTimeMillis();
                String string = context.welcomePrefix;
                File latestConnectionFile = context.latestConnectionFile;
                try {
                    if (latestConnectionFile.exists()) {
                        DataInputStream dataInputStream = new DataInputStream(new FileInputStream(latestConnectionFile));
                        long latestConnection = dataInputStream.readLong();
                        dataInputStream.close();
                        string += ", previous connection at " + latestConnection;
                    } else {
                        string += ", no previous connection";
                    }
                    string += "\r\n";

                    DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(latestConnectionFile));
                    dataOutputStream.writeLong(date);
                    dataOutputStream.close();

                    // following line is bad, because the write byte buffer may not have enough space
                    // but we don't care it is just an example
                    context.writeByteBuffer.clear();
                    context.writeByteBuffer.put(string.getBytes(Charset.forName("ASCII")));
                    context.writeByteBuffer.flip();
                } catch (IOException e) {

                }
            }
        });

        Promise<ClientContext> readPromise = compositeActionHelper.action(readAction);
        Promise<ClientContext> updateAfterRead = compositeActionHelper.simpleAction(new SimpleAction<ClientContext>() {

            public void perform(final ClientContext context) {
                if (context.readByteBuffer.limit() == context.readByteBuffer.capacity()) {
                    // read chunk is after write chunk

                    int firstRead = context.readByteBuffer.position() - context.readBytes;
                    int copyLimit = context.writeByteBuffer.limit();
                    context.writeByteBuffer.limit(context.writeByteBuffer.capacity());
                    // only rewrite data before a new line
                    for (int i = firstRead; i < firstRead + context.readBytes; i++) {
                        if (context.writeByteBuffer.get(i) == '\n') {
                            copyLimit = i + 1;
                        }
                    }

                    context.writeByteBuffer.limit(copyLimit);
                    if (!context.readByteBuffer.hasRemaining() && context.writeByteBuffer.position() != 0) {
                        // place read chunk before write chunk
                        context.readByteBuffer.position(0).limit(context.writeByteBuffer.position());
                    }
                }
                if (!context.readByteBuffer.hasRemaining()) {
                    context.readerWaiting = true;
                }
            }
        });
        Promise<ClientContext> writePromise = compositeActionHelper.action(writeAction);
        Promise<ClientContext> updateAfterWrite = compositeActionHelper.simpleAction(new SimpleAction<ClientContext>() {

            public void perform(final ClientContext context) {
                if (!context.writeByteBuffer.hasRemaining()) {
                    // all written
                    if (context.readByteBuffer.limit() == context.readByteBuffer.capacity()) {
                        // initial state
                        context.readByteBuffer.position(0);
                        context.writeByteBuffer.position(0).limit(0);
                    } else {
                        context.readByteBuffer.limit(context.readByteBuffer.capacity());
                        context.writeByteBuffer.position(0).limit(context.readByteBuffer.position());
                    }
                } else {

                }
            }
        });

        compositeActionHelper.getFirstPromise().then(welcomeAction);
        welcomeAction.then(writePromise);
        readPromise.conditionalThen(new Condition<ClientContext>() {

            public boolean test(final ClientContext context) {
                // if close => stop
                return context.readBytes != -1;
            }
        }, updateAfterRead);
        updateAfterRead.conditionalThen(new Condition<ClientContext>() {

            public boolean test(final ClientContext context) {
                return context.writeByteBuffer.hasRemaining();
            }
        }, writePromise);
        updateAfterRead.conditionalThen(new Condition<ClientContext>() {

            public boolean test(final ClientContext context) {
                return context.readByteBuffer.hasRemaining();
            }
        }, readPromise);
        writePromise.then(updateAfterWrite);
        updateAfterWrite.conditionalThen(new Condition<ClientContext>() {

            public boolean test(final ClientContext context) {
                return context.writeByteBuffer.hasRemaining();
            }
        }, writePromise);
        updateAfterWrite.conditionalThen(new Condition<ClientContext>() {

            public boolean test(final ClientContext context) {
                // if the buffer was full & not closed
                return context.readBytes != -1 && context.readByteBuffer.hasRemaining();
            }
        }, readPromise);

    }

}
