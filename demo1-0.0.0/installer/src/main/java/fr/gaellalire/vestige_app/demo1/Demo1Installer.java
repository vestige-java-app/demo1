package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.FileWriter;
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
        FileWriter writer = new FileWriter(file);
        properties.store(writer, null);
        writer.close();
    }

}
