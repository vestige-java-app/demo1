package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadFactory;

import fr.gaellalire.vestige_app.demo1.action.ExecuteContext;
import fr.gaellalire.vestige_app.demo1.action.impl.SelfThreadActionExecutor;
import fr.gaellalire.vestige_app.demo1.action.unparker.SelectUnparker;

public class Demo1 implements Callable<Void> {

    Properties properties = new Properties();

    File latestConnectionFile;

    File confProperties;

    long timestamp;

    public ExecuteContext<MainContext> executeContext;

    public Demo1(final File base, final File data) throws FileNotFoundException, IOException {
        confProperties = new File(base, "conf.properties");
        timestamp = confProperties.lastModified();
        FileReader reader = new FileReader(confProperties);
        properties.load(reader);
        reader.close();
        latestConnectionFile = new File(data, "latestConnection.txt");
        SelfThreadActionExecutor singleThreadActionExecutor = new SelfThreadActionExecutor();

        executeContext = singleThreadActionExecutor.createExecuteContext(new EchoServerManager(new AcceptAction(), new EchoServer(new ReadAction(), new WriteAction())));
    }

    public Void call() throws Exception {
        if (executeContext.isPaused()) {
            executeContext.resume();
        } else {
            SelectUnparker selectUnparker = new SelectUnparker();
            String welcomePrefix = properties.getProperty("hello") + " " + properties.getProperty("name");
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.configureBlocking(false);
            int port = Integer.parseInt(properties.getProperty("port"));
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
            MainContext mainContext = new MainContext(serverSocketChannel, selectUnparker, welcomePrefix, latestConnectionFile);
            selectUnparker.start(new ThreadFactory() {

                public Thread newThread(final Runnable r) {
                    return new Thread(r);
                }
            });
            long lastModified = confProperties.lastModified();
            if (lastModified != timestamp) {
                FileReader reader = new FileReader(confProperties);
                properties.load(reader);
                reader.close();
                timestamp = lastModified;
            }
            executeContext.execute(mainContext);
        }
        return null;
    }

}
