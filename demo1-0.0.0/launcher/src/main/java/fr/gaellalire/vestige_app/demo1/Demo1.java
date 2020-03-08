package fr.gaellalire.vestige_app.demo1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Properties;

public class Demo1 implements Runnable {

    Properties properties = new Properties();

    File latestConnectionFile;

    ServerSocketChannel serverSocketChannel;

    public Demo1(final File base, final File data) throws FileNotFoundException, IOException {
        FileReader reader = new FileReader(new File(base, "conf.properties"));
        properties.load(reader);
        reader.close();
        latestConnectionFile = new File(data, "latestConnection.txt");
    }

    public void run() {
        try {
            int port = Integer.parseInt(properties.getProperty("port"));
            serverSocketChannel = ServerSocketChannel.open();
            serverSocketChannel.socket().bind(new InetSocketAddress(port));

            SocketChannel accept = serverSocketChannel.accept();
            while (accept != null) {
                long date = System.currentTimeMillis();

                String string = "Hello " + properties.getProperty("name");

                if (latestConnectionFile.exists()) {
                    DataInputStream dataInputStream = new DataInputStream(new FileInputStream(latestConnectionFile));
                    long latestConnection = dataInputStream.readLong();
                    dataInputStream.close();
                    string += ", previous connection at " + latestConnection;
                } else {
                    string += ", no previous connection";
                }
                string += "\r\n";

                OutputStream outputStream = accept.socket().getOutputStream();
                outputStream.write(string.getBytes(Charset.forName("ASCII")));
                outputStream.close();
                accept.close();

                DataOutputStream dataOutputStream = new DataOutputStream(new FileOutputStream(latestConnectionFile));
                dataOutputStream.writeLong(date);
                dataOutputStream.close();

                accept = serverSocketChannel.accept();
            }
            serverSocketChannel.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(final String[] args) throws FileNotFoundException, IOException {
        new Demo1(new File("."), new File(".")).run();
    }
}
