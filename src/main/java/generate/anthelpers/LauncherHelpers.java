package generate.anthelpers;

import org.apache.tools.ant.launch.LaunchException;
import org.apache.tools.ant.launch.Launcher;
import org.apache.tools.ant.launch.Locator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Created by manuel on 17.02.17.
 */
public class LauncherHelpers {

    /**
     * The Ant Home (installation) Directory property.
     * {@value}
     */
    public static final String ANTHOME_PROPERTY = "ant.home";

    /**
     * The Ant Library Directory property.
     * {@value}
     */
    public static final String ANTLIBDIR_PROPERTY = "ant.library.dir";

    /**
     * The directory name of the per-user ant directory.
     * {@value}
     */
    public static final String ANT_PRIVATEDIR = ".ant";

    /**
     * The name of a per-user library directory.
     * {@value}
     */
    public static final String ANT_PRIVATELIB = "lib";

    /**
     * The location of a per-user library directory.
     * <p>
     * It's value is the concatenation of {@link #ANT_PRIVATEDIR}
     * with {@link #ANT_PRIVATELIB}, with an appropriate file separator
     * in between. For example, on Unix, it's <code>.ant/lib</code>.
     */
    public static final String USER_LIBDIR =
            ANT_PRIVATEDIR + File.separatorChar + ANT_PRIVATELIB;

    /**
     * The startup class that is to be run.
     * {@value}
     */
    public static final String MAIN_CLASS = "org.apache.tools.ant.Main";

    /**
     * System property with user home directory.
     * {@value}
     */
    public static final String USER_HOMEDIR = "user.home";

    /**
     * System property with application classpath.
     * {@value}
     */
    private static final String JAVA_CLASS_PATH = "java.class.path";

    final static boolean launchDiag = true;


    /**
     * Add a CLASSPATH or -lib to lib path urls.
     * Only filesystem resources are supported.
     *
     * @param path        the classpath or lib path to add to the libPathULRLs
     * @param getJars     if true and a path is a directory, add the jars in
     *                    the directory to the path urls
     * @param libPathURLs the list of paths to add to
     * @throws MalformedURLException if we can't create a URL
     */
    public static void addPath(final String path, final boolean getJars, final List<URL> libPathURLs)
            throws MalformedURLException {
        final StringTokenizer tokenizer = new StringTokenizer(path, File.pathSeparator);
        while (tokenizer.hasMoreElements()) {
            final String elementName = tokenizer.nextToken();
            final File element = new File(elementName);
            if (elementName.indexOf('%') != -1 && !element.exists()) {
                continue;
            }
            if (getJars && element.isDirectory()) {
                // add any jars in the directory
                final URL[] dirURLs = Locator.getLocationURLs(element);
                for (int j = 0; j < dirURLs.length; ++j) {
                    if (launchDiag) { System.out.println("adding library JAR: " + dirURLs[j]);}
                    libPathURLs.add(dirURLs[j]);
                }
            }

            final URL url = Locator.fileToURL(element);
            if (launchDiag) {
                System.out.println("adding library URL: " + url);
            }
            libPathURLs.add(url);
        }
    }

    /**
     * Get the list of -lib entries and -cp entry into
     * a URL array.
     * @param cpString the classpath string
     * @param libPaths the list of -lib entries.
     * @return an array of URLs.
     * @throws MalformedURLException if the URLs  cannot be created.
     */
    public static URL[] getLibPathURLs(final String cpString, final List<String> libPaths)
            throws MalformedURLException {
        final List<URL> libPathURLs = new ArrayList<URL>();

        if (cpString != null) {
            addPath(cpString, false, libPathURLs);
        }

        for (final String libPath : libPaths) {
            addPath(libPath, true, libPathURLs);
        }

        return libPathURLs.toArray(new URL[libPathURLs.size()]);
    }

    /**
     * Get the jar files in ANT_HOME/lib.
     * determine ant library directory for system jars: use property
     * or default using location of ant-launcher.jar
     * @param antLauncherDir the dir that ant-launcher ran from
     * @return the URLs
     * @throws MalformedURLException if the URLs cannot be created.
     */
    public static URL[] getSystemURLs(final File antLauncherDir) throws MalformedURLException {
        File antLibDir = null;
        final String antLibDirProperty = System.getProperty(ANTLIBDIR_PROPERTY);
        if (antLibDirProperty != null) {
            antLibDir = new File(antLibDirProperty);
        }
        if ((antLibDir == null) || !antLibDir.exists()) {
            antLibDir = antLauncherDir;
            setProperty(ANTLIBDIR_PROPERTY, antLibDir.getAbsolutePath());
        }
        return Locator.getLocationURLs(antLibDir);
    }

    /**
     * Get the jar files in user.home/.ant/lib
     * @return the URLS from the user's lib dir
     * @throws MalformedURLException if the URLs cannot be created.
     */
    private static URL[] getUserURLs() throws MalformedURLException {
        final File userLibDir
                = new File(System.getProperty(USER_HOMEDIR), USER_LIBDIR);

        return Locator.getLocationURLs(userLibDir);
    }

    /**
     * Combine the various jar sources into a single array of jars.
     * @param libJars the jars specified in -lib command line options
     * @param userJars the jars in ~/.ant/lib
     * @param systemJars the jars in $ANT_HOME/lib
     * @param toolsJar   the tools.jar file
     * @return a combined array
     * @throws MalformedURLException if there is a problem.
     */
    private static URL[] getJarArray(
            final URL[] libJars, final URL[] userJars, final URL[] systemJars, final File toolsJar)
            throws MalformedURLException {
        int numJars = libJars.length + userJars.length + systemJars.length;
        if (toolsJar != null) {
            numJars++;
        }
        final URL[] jars = new URL[numJars];
        System.arraycopy(libJars, 0, jars, 0, libJars.length);
        System.arraycopy(userJars, 0, jars, libJars.length, userJars.length);
        System.arraycopy(systemJars, 0, jars, userJars.length + libJars.length,
                systemJars.length);

        if (toolsJar != null) {
            jars[jars.length - 1] = Locator.fileToURL(toolsJar);
        }
        return jars;
    }

    /**
     * set a system property, optionally log what is going on
     * @param name property name
     * @param value value
     */
    public static void setProperty(final String name, final String value) {
        if (launchDiag) {
            System.out.println("Setting \"" + name + "\" to \"" + value + "\"");
        }
        System.setProperty(name, value);
    }

    public static void logPath(final String name,final File path) {
        if(launchDiag) {
            System.out.println(name+"= \""+path+"\"");
        }
    }


    public static void prepareClassLoader(String cpString, List<String> libPaths) throws LaunchException, MalformedURLException {
        final String antHomeProperty = System.getProperty(ANTHOME_PROPERTY);
        File antHome = null;

        final File sourceJar = Locator.getClassSource(LauncherHelpers.class);
        final File jarDir = sourceJar.getParentFile();
        String mainClassname = MAIN_CLASS;

        if (antHomeProperty != null) {
            antHome = new File(antHomeProperty);
        }

        if (antHome == null || !antHome.exists()) {
            antHome = jarDir.getParentFile();
            setProperty(ANTHOME_PROPERTY, antHome.getAbsolutePath());
        }

        if (!antHome.exists()) {
            throw new LaunchException("Ant home is set incorrectly or "
                    + "ant could not be located (estimated value="+antHome.getAbsolutePath()+")");
        }

        final List<String> argList = new ArrayList<String>();
        String[] newArgs;
        boolean  noUserLib = false;
        boolean  noClassPath = false;

        logPath("Launcher JAR",sourceJar);
        logPath("Launcher JAR directory", sourceJar.getParentFile());
        logPath("java.home", new File(System.getProperty("java.home")));

        final URL[] libURLs    = getLibPathURLs(
                noClassPath ? null : cpString, libPaths);
        final URL[] systemURLs = getSystemURLs(jarDir);
        final URL[] userURLs   = noUserLib ? new URL[0] : getUserURLs();

        final File toolsJAR = Locator.getToolsJar();
        logPath("tools.jar",toolsJAR);
        final URL[] jars = getJarArray(
                libURLs, userURLs, systemURLs, toolsJAR);

        // now update the class.path property
        final StringBuffer baseClassPath
                = new StringBuffer(System.getProperty(JAVA_CLASS_PATH));
        if (baseClassPath.charAt(baseClassPath.length() - 1)
                == File.pathSeparatorChar) {
            baseClassPath.setLength(baseClassPath.length() - 1);
        }

        for (int i = 0; i < jars.length; ++i) {
            baseClassPath.append(File.pathSeparatorChar);
            baseClassPath.append(Locator.fromURI(jars[i].toString()));
        }

        setProperty(JAVA_CLASS_PATH, baseClassPath.toString());

        final URLClassLoader loader = new URLClassLoader(jars, LauncherHelpers.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(loader);
    }

}
