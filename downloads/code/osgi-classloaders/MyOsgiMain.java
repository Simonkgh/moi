package example;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.launch.Framework;
import org.osgi.framework.launch.FrameworkFactory;

import java.util.Map;
import java.util.HashMap;
import java.util.ServiceLoader;
import java.util.List;
import java.util.LinkedList;


// Code to initialise an OSGi environment
//
// Adapted from the article 'http://njbartlett.name/2011/03/07/embedding-osgi.html'
public class MyOsgiMain {
    public static void main(String[] args) throws Exception {
        // Use the standard Java "ServiceLoader" approach to finding FrameworkFactory implementations in
        // the classpath. The OSGi implementation (whatever it is) should have the appropriate entry
        // in its MANIFEST.MF for the ServiceLoader to find..
        FrameworkFactory frameworkFactory = ServiceLoader.load(FrameworkFactory.class).iterator().next();

        // Start up the core OSGi stuff; the result is an environment with exactly one OSGI bundle
        // in it : the osgi "system" bundle.
        Map<String, String> config = initConfig();
        Framework framework = frameworkFactory.newFramework(config);
        framework.start();

        // Tell OSGi to load a bunch of bundles (aka jarfiles)
        BundleContext context = framework.getBundleContext();
        List<Bundle> installedBundles = new LinkedList<Bundle>();

        installedBundles.add(context.installBundle("file:/some/library.jar"));
        installedBundles.add(context.installBundle("file:/some/otherlib.jar"));

        // And now "start" the loaded bundles - except for "fragment" ones
        for (Bundle bundle : installedBundles) {
            if (bundle.getHeaders().get(Constants.FRAGMENT_HOST) == null) {
                bundle.start();
            }
        }

        // and finally leave the bundles alone to deal with events..
        try {
            framework.waitForStop(0);
        } finally {
            System.exit(0);
        }
    }

    static Map<String, String> initConfig() {
        Map<String, String> config = new HashMap<String, String>();

        // Allow OSGi bundles to import this package from the standard java classpath classloader
        config.put(Constants.FRAMEWORK_SYSTEMPACKAGES_EXTRA, "example.package");

        // See the org.osgi.framework.Constants class for other useful things that can be put
        // into the OSGi configuration map.

        return config;
    }
}
