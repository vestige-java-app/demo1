package fr.gaellalire.vestige_app.demo1;

import java.io.IOException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

import fr.gaellalire.vestige_app.demo1.AcceptAction.AcceptActionContext;
import fr.gaellalire.vestige_app.demo1.action.Action;
import fr.gaellalire.vestige_app.demo1.action.ActionHelper;
import fr.gaellalire.vestige_app.demo1.action.unparker.UnparkerAction;


public class AcceptAction implements Action<AcceptActionContext> {

    public interface AcceptActionContext {

        ServerSocketChannel getServerSocketChannel();

        void setAcceptedSocketChannel(SocketChannel socketChannel);

        void setIOException(IOException exception);

        UnparkerAction getUnparkerAction();

    }

    public int perform(final ActionHelper actionHelper, final AcceptActionContext acceptActionContext) {
        try {
            ServerSocketChannel serverSocketChannel = acceptActionContext.getServerSocketChannel();
            SocketChannel accept = serverSocketChannel.accept();
            if (accept == null) {
                acceptActionContext.getUnparkerAction().perform(serverSocketChannel, UnparkerAction.OP_ACCEPT, actionHelper.getReadyNotificationHandler());
                return Action.PENDING;
            }
            accept.configureBlocking(false);
            acceptActionContext.setAcceptedSocketChannel(accept);
        } catch (IOException e) {
            acceptActionContext.setIOException(e);
        }
        return Action.DONE;
    }

}
