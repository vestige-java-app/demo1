package fr.gaellalire.vestige_app.demo1;

import java.nio.ByteBuffer;

import fr.gaellalire.vestige_app.demo1.action.CompositeAction;
import fr.gaellalire.vestige_app.demo1.action.CompositeActionHelper;
import fr.gaellalire.vestige_app.demo1.action.Condition;
import fr.gaellalire.vestige_app.demo1.action.ContextAllocator;
import fr.gaellalire.vestige_app.demo1.action.Promise;

public class EchoServerManager implements CompositeAction<MainContext> {

    AcceptAction acceptAction;

    EchoServer echoServer;

    public EchoServerManager(final AcceptAction acceptAction, final EchoServer echoServer) {
        this.acceptAction = acceptAction;
        this.echoServer = echoServer;
    }

    public void describe(final CompositeActionHelper<MainContext> actionHelper) {
        Promise<MainContext> acceptPromise = actionHelper.action(acceptAction);
        actionHelper.getFirstPromise().then(acceptPromise);
        Promise<MainContext> allocateContextPromise = actionHelper.fork(new ContextAllocator<ClientContext, MainContext>() {

            public ClientContext allocate(final MainContext context) {
                ClientContext clientContext = new ClientContext();
                ByteBuffer allocate = ByteBuffer.allocate(1024 * 1024);
                clientContext.readByteBuffer = allocate;
                ByteBuffer duplicate = allocate.duplicate();
                duplicate.flip();
                clientContext.writeByteBuffer = duplicate;
                clientContext.readUnparkerAction = context.selectUnparker.prepareUnparkerAction();
                clientContext.writeUnparkerAction = context.selectUnparker.prepareUnparkerAction();
                clientContext.socketChannel = context.acceptedSocketChannel;
                clientContext.welcomePrefix = context.welcomePrefix;
                clientContext.latestConnectionFile = context.latestConnectionFile;
                return clientContext;
            }

        }, echoServer);
        acceptPromise.conditionalThen(new Condition<MainContext>() {

            public boolean test(final MainContext context) {
                return context.getIOException() == null;
            }
        }, allocateContextPromise);
        allocateContextPromise.then(acceptPromise);
    }


}
