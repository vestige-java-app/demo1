package fr.gaellalire.vestige_app.demo1;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Properties;
import java.util.concurrent.Callable;

public class Demo1 implements Callable<Void> {

    Properties properties = new Properties();

    File latestConnectionFile;

    public ServerSocketChannel serverSocketChannel;

    File confProperties;

    long timestamp;

    public BufferedReader bufferedReader;

    public boolean stopWithoutClean;

    public OutputStream outputStream;

    public SocketChannel accept;

    public Thread mainThread;

    public Demo1(final File base, final File data) throws FileNotFoundException, IOException {
        confProperties = new File(base, "conf.properties");
        timestamp = confProperties.lastModified();
        FileReader reader = new FileReader(confProperties);
        properties.load(reader);
        reader.close();
        latestConnectionFile = new File(data, "latestConnection.txt");
        mainThread = Thread.currentThread();
    }

    public Void call() throws Exception {
        long lastModified = confProperties.lastModified();
        if (lastModified != timestamp) {
            FileReader reader = new FileReader(confProperties);
            properties.load(reader);
            reader.close();
            timestamp = lastModified;
        }

        if (serverSocketChannel == null) {
            int port = Integer.parseInt(properties.getProperty("port"));
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));
        }

        try {
            if (accept != null) {
                continueProcess();
                if (stopWithoutClean) {
                    return null;
                }
            }

            accept = serverSocketChannel.accept();
            while (accept != null) {
                process();
                if (stopWithoutClean) {
                    return null;
                }
                accept = serverSocketChannel.accept();
            }
            serverSocketChannel.close();
        } catch (ClosedByInterruptException e) {
            if (!stopWithoutClean) {
                outputStream.close();
                accept.close();
                bufferedReader.close();
                bufferedReader = null;
                outputStream = null;
                serverSocketChannel.close();
            }
        }
        return null;
    }

    private void process() throws FileNotFoundException, IOException {
        long date = System.currentTimeMillis();

        String string = properties.getProperty("hello") + " " + properties.getProperty("name");

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

        bufferedReader = new BufferedReader(new InputStreamReader(accept.socket().getInputStream()));
        outputStream = accept.socket().getOutputStream();
        outputStream.write(string.getBytes(Charset.forName("ASCII")));
        outputStream.flush();

        continueProcess();
    }

    private void continueProcess() throws IOException {
        while (bufferedReader.readLine() != null) {
            outputStream.write("echo\r\n".getBytes(Charset.forName("ASCII")));
            outputStream.flush();
            if (stopWithoutClean) {
                return;
            }
        }
        outputStream.close();
        accept.close();
        bufferedReader.close();
        bufferedReader = null;
        outputStream = null;
    }

    public static void main(final String[] args) throws Exception {
        new Demo1(new File("."), new File(".")).call();
    }
}
