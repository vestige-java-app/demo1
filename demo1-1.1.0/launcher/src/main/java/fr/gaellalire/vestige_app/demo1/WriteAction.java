package fr.gaellalire.vestige_app.demo1;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import fr.gaellalire.vestige_app.demo1.WriteAction.WriteActionContext;
import fr.gaellalire.vestige_app.demo1.action.Action;
import fr.gaellalire.vestige_app.demo1.action.ActionHelper;
import fr.gaellalire.vestige_app.demo1.action.unparker.UnparkerAction;

public class WriteAction implements Action<WriteActionContext> {

    public interface WriteActionContext {

        SocketChannel getSocketChannel();

        ByteBuffer getWriteByteBuffer();

        void setWrittenBytes(int writtenBytes);

        void setIOException(IOException exception);

        UnparkerAction getWriteUnparkerAction();

    }

    public int perform(final ActionHelper actionHelper, final WriteActionContext context) {
        try {
            SocketChannel socketChannel = context.getSocketChannel();
            int writtenBytes = socketChannel.write(context.getWriteByteBuffer());
            if (writtenBytes == 0) {
                context.getWriteUnparkerAction().perform(socketChannel, UnparkerAction.OP_WRITE, actionHelper.getReadyNotificationHandler());
                return Action.PENDING;
            }
            context.setWrittenBytes(writtenBytes);
        } catch (IOException e) {
            context.setIOException(e);
        }
        return Action.DONE;
    }

    @Override
    public String toString() {
        return "write action";
    }

}
