package fr.gaellalire.vestige_app.demo1;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Field;
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

    public void uninstall() throws Exception {
        /*
        synchronized (this) {
            wait();
        }
        */
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

    /**
     * Migrate from an older version to this version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockThread.run() the runnable.run() will be called.
     */
    public void prepareUninterruptedMigrateFrom(final List<Integer> fromVersion, final Object fromRunnable, final Object runnable, final Runnable unlockThread) throws Exception {
        Class<? extends Object> class1 = fromRunnable.getClass();
        Class<? extends Object> class2 = runnable.getClass();
        Converter converter = new Converter(class2.getClassLoader());

        Field field, field2;

        field = class1.getField("executeContext");
        field2 = class2.getField("executeContext");
        Object executeContext = field.get(fromRunnable);
        Class<? extends Object> executeContextClass = class1.getClassLoader().loadClass("fr.gaellalire.vestige_app.demo1.action.ExecuteContext");

        executeContextClass.getMethod("pauseAndWait").invoke(executeContext);
        field2.set(runnable, converter.convert(executeContext));

        unlockThread.run();
    }

    /**
     * Migrate from this version to an older version without stopping the
     * application. When this method is called fromRunnable is still running and the run method of runnable is not yet called.
     * When you call unlockToThread.run() the toRunnable.run() will be called.
     */
    public void prepareUninterruptedMigrateTo(final Object runnable, final List<Integer> toVersion, final Object toRunnable, final Runnable unlockToThread) throws Exception {
        Class<? extends Object> class1 = runnable.getClass();
        Class<? extends Object> class2 = toRunnable.getClass();
        Converter converter = new Converter(class2.getClassLoader());

        Field field, field2;

        field = class1.getField("executeContext");
        field2 = class2.getField("executeContext");
        Object executeContext = field.get(runnable);
        Class<? extends Object> executeContextClass = class1.getClassLoader().loadClass("fr.gaellalire.vestige_app.demo1.action.ExecuteContext");

        executeContextClass.getMethod("pauseAndWait").invoke(executeContext);
        field2.set(toRunnable, converter.convert(executeContext));

        unlockToThread.run();
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
