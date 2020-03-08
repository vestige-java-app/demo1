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

    // support 0.0.0 and 0.1.0 and 1.0.0
    public void prepareMigrateFrom(final List<Integer> fromVersion) throws Exception {
        if (fromVersion.get(0).intValue() == 0 && fromVersion.get(1).intValue() == 0) {
            // 0.0.0
            File file = new File(base, "conf.properties");
            FileReader reader = new FileReader(file);
            Properties properties = new Properties();
            properties.load(reader);
            reader.close();
            properties.put("hello", "Hello");
            FileWriter writer = new FileWriter(new File(base, "conf.properties.new"));
            properties.store(writer, null);
            writer.close();
        } else {
            // 0.1.0 or 1.0.0
            // nothing
        }
    }

    // support 0.0.0 and 0.1.0 and 1.0.0
    public void prepareMigrateTo(final List<Integer> toVersion) throws Exception {
        if (toVersion.get(0).intValue() == 0 && toVersion.get(1).intValue() == 0) {
            // 0.0.0
            File file = new File(base, "conf.properties");
            FileReader reader = new FileReader(file);
            Properties properties = new Properties();
            properties.load(reader);
            reader.close();
            properties.remove("hello");
            FileWriter writer = new FileWriter(new File(base, "conf.properties.new"));
            properties.store(writer, null);
            writer.close();
        } else {
            // 0.1.0 or 1.0.0
            // nothing
        }
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
