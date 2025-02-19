
package org.wargamer2010.signshop.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import org.wargamer2010.signshop.SignShop;

public class JarUtil {
    private static ReentrantLock loadLocker = new ReentrantLock();

    private JarUtil() {

    }

    public static boolean loadClass(String filename, String className) {
        try {
            loadLocker.lock(); //tryLock() doesn't guarantee the lock is aquired. the result wasn't used either.
            File libLocation = new File(SignShop.getInstance().getDataFolder(), "lib" + File.separator + filename);
            if (!libLocation.exists())
                getDriver(libLocation);
            JarUtil.addClassPath(new URL("jar:file:" + libLocation.getPath() + "!/"));
            return true;
        } catch (MalformedURLException ex) {
        } catch (IOException ex) {
        } finally {
            loadLocker.unlock();
        }
        return false;
    }

    static void getDriver(File destination) {
        try {
            if (destination.exists()) {
                destination.delete();
            }
            if (!destination.getParentFile().exists()) {
                destination.getParentFile().mkdirs();
            }
            destination.createNewFile();
            SignShop.log("Copying " + destination.getName() + " from JAR to Lib folder", Level.INFO);

            FileOutputStream out = new FileOutputStream(destination);
            InputStream in = SignShop.class.getResourceAsStream("/" + destination.getName());
            byte[] buffer = new byte[1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }

            in.close();
            out.close();
        } catch (IOException e) {
        }
    }

    /**
     * @deprecated bundling and shading is preferred
     * @param url where the class(es) live
     * @throws IOException if anything goes wrong
     */
    @Deprecated
    private static void addClassPath(final URL url) throws IOException {
        //TODO might throw ClassCastExeption in the future, especially when bukkit gets support for libraries in plugin.yml
        URLClassLoader pluginClassLoader = (URLClassLoader) SignShop.class.getClassLoader();

        try {
            final Method method = URLClassLoader.class.getDeclaredMethod("addURL", URL.class);
            method.setAccessible(true);
            method.invoke(pluginClassLoader, url);
        } catch (final Throwable t) {
            throw new IOException("Error adding " + url + " to plugin classloader", t);
        }
    }
}
