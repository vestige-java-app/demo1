package fr.gaellalire.vestige_app.demo1;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import sun.reflect.ReflectionFactory;

/**
 * This converter is able to convert safely :
 * <ul>
 * <li> any object whose hierarchy has only java.lang.Object as JVM ancestor.</li>
 * <li> any JVM object whose hierarchy has no reference to a non JVM object</li>
 * <li> JVM list, map and selector.</li>
 * </ul>
 * @author Gael Lalire
 */
@SuppressWarnings({"restriction", "unchecked"})
public class Converter {

    private static List<ClassLoader> jvmClassLoaders;

    interface SpecificConverter {

        Object specificConvert(Converter converter, Object o) throws Exception;

    }

    private static Map<Class<?>, SpecificConverter> specificConverters;

    static {
        jvmClassLoaders = new ArrayList<ClassLoader>();
        ClassLoader systemClassLoader = ClassLoader.getSystemClassLoader();
        while (systemClassLoader != null) {
            jvmClassLoaders.add(systemClassLoader);
            systemClassLoader = systemClassLoader.getParent();
        }
        jvmClassLoaders.add(null);

        specificConverters = new HashMap<Class<?>, Converter.SpecificConverter>();
        SpecificConverter listConverter = new SpecificConverter() {

            public Object specificConvert(final Converter converter, final Object o) throws Exception {
                converter.alreadyConverted.put(o, o);
                List<Object> list = (List<Object>) o;
                ListIterator<Object> listIterator = list.listIterator();
                while (listIterator.hasNext()) {
                    listIterator.set(converter.convert(listIterator.next()));
                }
                return o;
            }

        };
        SpecificConverter mapConverter = new SpecificConverter() {

            public Object specificConvert(final Converter converter, final Object o) throws Exception {
                converter.alreadyConverted.put(o, o);
                Map<Object, Object> map = (Map<Object, Object>) o;
                int size = map.size();
                List<Object> keys = new ArrayList<Object>(size);
                List<Object> values = new ArrayList<Object>(size);
                Iterator<Entry<Object, Object>> iterator = map.entrySet().iterator();
                while (iterator.hasNext()) {
                    Entry<Object, Object> next = iterator.next();
                    keys.add(converter.convert(next.getKey()));
                    values.add(converter.convert(next.getValue()));
                }
                map.clear();
                for (int i = 0; i < size; i++) {
                    map.put(keys.get(i), values.get(i));
                }
                return o;
            }

        };
        specificConverters.put(List.class, listConverter);
        specificConverters.put(Map.class, mapConverter);
        specificConverters.put(Selector.class, new SpecificConverter() {

            @Override
            public Object specificConvert(final Converter converter, final Object o) throws Exception {
                converter.alreadyConverted.put(o, o);
                Selector selector = (Selector) o;
                Set<SelectionKey> keys = selector.keys();
                for (SelectionKey selectionKey : keys) {
                    selectionKey.attach(converter.convert(selectionKey.attachment()));
                }
                return o;
            }
        });
    }

    Map<Object, Object> alreadyConverted = new IdentityHashMap<Object, Object>();

    ReflectionFactory reflectionFactory = sun.reflect.ReflectionFactory.getReflectionFactory();

    ClassLoader destClassloader;

    public Converter(final ClassLoader destClassloader) {
        this.destClassloader = destClassloader;
    }

    public Object convert(final Object o) throws Exception {
        if (o == null) {
            return null;
        }
        return convert(o, o.getClass());
    }

    public <T> Constructor<T> createConstructor(final Class<T> clazz) throws Exception {
        if (clazz == Object.class) {
            return (Constructor<T>) Object.class.getConstructor();
        }
        Class<? super T> superclass = clazz.getSuperclass();
        Constructor<? super T> parentConstructor = createConstructor(superclass);
        return reflectionFactory.newConstructorForSerialization(clazz, parentConstructor);
    }

    private void copyFields(final Object o, final Object dest, final Class<?> oclass, final Class<?> destClass) throws Exception {
        if (destClass == null) {
            return;
        }
        copyFields(o, dest, oclass.getSuperclass(), destClass.getSuperclass());
        for (Field field : destClass.getDeclaredFields()) {
            field.setAccessible(true);
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            // System.out.println("field " + field);
            try {
                Field ofield = oclass.getDeclaredField(field.getName());
                ofield.setAccessible(true);

                Class<?> type = field.getType();
                Object oele = ofield.get(o);
                if (oele != null) {
                    if (!type.isPrimitive()) {
                        type = oele.getClass();
                    }
                    field.set(dest, convert(oele, type));
                }
                // System.out.println("field " + field);
            } catch (NoSuchFieldException e) {
                e.printStackTrace();

            }
        }
    }

    private Object convert(final Object o, final Class<? extends Object> oclass) throws Exception {
        // System.out.println("convert " + o);
        if (o == null) {
            return null;
        }
        if (oclass.isPrimitive()) {
            return o;
        }
        if (oclass.equals(Thread.class)) {
            // thread object should not be converted
            return null;
        }
        Object dest = alreadyConverted.get(o);
        if (dest != null) {
            return dest;
        }
        if (oclass.isArray()) {
            Class<?> componentType = oclass.getComponentType();
            Class<?> destComponentClass;
            if (componentType.isPrimitive()) {
                destComponentClass = componentType;
            } else {
                destComponentClass = destClassloader.loadClass(componentType.getName());
            }
            int length = Array.getLength(o);
            dest = Array.newInstance(destComponentClass, length);
            alreadyConverted.put(o, dest);
            if (destComponentClass.isPrimitive()) {
                for (int i = 0; i < length; i++) {
                    Array.set(dest, i, convert(Array.get(o, i), destComponentClass));
                }
            } else {
                for (int i = 0; i < length; i++) {
                    Object oele = Array.get(o, i);
                    if (oele != null) {
                        Array.set(dest, i, convert(oele, oele.getClass()));
                    }
                }
            }
        } else {
            SpecificConverter specificConverter = null;
            for (Entry<Class<?>, SpecificConverter> entry : specificConverters.entrySet()) {
                if (entry.getKey().isInstance(o)) {
                    specificConverter = entry.getValue();
                    break;
                }
            }
            if (specificConverter != null) {
                // specificConverter helps converting JVM objects without java 9 access restriction issue
                dest = specificConverter.specificConvert(this, o);
            } else if (jvmClassLoaders.contains(oclass.getClassLoader())) {
                // cannot be converted without risking illegal access, we may have a class cast exception later
                alreadyConverted.put(o, o);
                dest = o;
            } else {
                Class<?> destClass = destClassloader.loadClass(oclass.getName());
                if (destClass.equals(Class.class)) {
                    dest = destClassloader.loadClass(((Class<?>) o).getName());
                } else {
                    dest = createConstructor(destClass).newInstance();
                    alreadyConverted.put(o, dest);
                    copyFields(o, dest, oclass, destClass);
                }
            }
        }
        return dest;
    }

}
