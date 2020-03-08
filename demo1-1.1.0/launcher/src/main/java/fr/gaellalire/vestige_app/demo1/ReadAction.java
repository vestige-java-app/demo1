package fr.gaellalire.vestige_app.demo1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import fr.gaellalire.vestige_app.demo1.ReadAction.ReadActionContext;
import fr.gaellalire.vestige_app.demo1.action.Action;
import fr.gaellalire.vestige_app.demo1.action.ActionHelper;
import fr.gaellalire.vestige_app.demo1.action.unparker.UnparkerAction;

public class ReadAction implements Action<ReadActionContext> {

    public interface ReadActionContext {

        SocketChannel getSocketChannel();

        ByteBuffer getReadByteBuffer();

        void setReadBytes(int readBytes);

        void setIOException(IOException exception);

        UnparkerAction getReadUnparkerAction();

    }

    public int perform(final ActionHelper actionHelper, final ReadActionContext context) {
        try {
            SocketChannel socketChannel = context.getSocketChannel();
            int read = socketChannel.read(context.getReadByteBuffer());
            context.setReadBytes(read);
            if (read == 0) {
                context.getReadUnparkerAction().perform(socketChannel, UnparkerAction.OP_READ, actionHelper.getReadyNotificationHandler());
                return Action.PENDING;
            }
        } catch (IOException e) {
            context.setIOException(e);
        }
        return Action.DONE;
    }

    @Override
    public String toString() {
        return "read action";
    }

}
