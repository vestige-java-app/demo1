package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.List;
import java.util.Properties;

public class Demo1Installer {

    private File base;

    public Demo1Installer(final File base) {
        this.base = base;
    }

    public void install() throws Exception {
        File file = new File(base, "conf.properties");
        Properties properties = new Properties();
        properties.put("port", "7894");
        properties.put("name", "unknow");
        properties.put("hello", "Hello");
        FileWriter writer = new FileWriter(file);
        properties.store(writer, null);
        writer.close();
    }

    // support only from 0.0.0
    public void prepareMigrateFrom(final List<Integer> fromVersion) throws Exception {
        File file = new File(base, "conf.properties");
        FileReader reader = new FileReader(file);
        Properties properties = new Properties();
        properties.load(reader);
        reader.close();
        properties.put("hello", "Hello");
        FileWriter writer = new FileWriter(new File(base, "conf.properties.new"));
        properties.store(writer, null);
        writer.close();
    }

    // support only to 0.0.0
    public void prepareMigrateTo(final List<Integer> toVersion) throws Exception {
        File file = new File(base, "conf.properties");
        FileReader reader = new FileReader(file);
        Properties properties = new Properties();
        properties.load(reader);
        reader.close();
        properties.remove("hello");
        FileWriter writer = new FileWriter(new File(base, "conf.properties.new"));
        properties.store(writer, null);
        writer.close();
    }

    public void cleanMigration() throws Exception {
        File nfile = new File(base, "conf.properties.new");
        if (nfile.exists()) {
            nfile.delete();
        }
    }

    public void commitMigration() throws Exception {
        File nfile = new File(base, "conf.properties.new");
        if (nfile.exists()) {
            File file = new File(base, "conf.properties");
            if (file.exists()) {
                file.delete();
            }
            nfile.renameTo(file);
        }
    }

}
