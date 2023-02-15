// Copyright Simon Kitching 2017. Available under the Apache License 2.0
package net.vonos.network.util;

import sun.net.spi.nameservice.NameService;

import java.lang.reflect.Field;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Test/Development utility which intercepts the JRE logic which maps domain-names to addresses, allowing programmer-provided mappings.
 * <p>
 * Mappings looked-up by the JRE before this class is initialised are already cached in the JDK, ie customised mappings made too late will have no effect.
 * </p>
 * <p>
 * Example Usage:
 * <pre>
 *   NetworkNameMapper.install(NetworkNameMapper.mappingsFromSystemProperties());
 * </pre>
 * </p>
 */
public class NetworkNameMapper {
    // Disable constructor
    private NetworkNameMapper() {
    }

    /**
     * Build a mapping suitable for passing to method NetworkNameMapper.install.
     * <p>
     * System properties of form "dns.map.somename=alias" will cause lookups of "somename" to return the alias.
     * The alias may be an IP-address or a name; names will be looked up using the mappings existing at the time this method is called.
     * </p>
     */
    public static Map<String, InetAddress> mappingsFromSystemProperties() {
        try {
            Map<String, InetAddress> mappings = new HashMap<>();
            for (Map.Entry<?, ?> e : System.getProperties().entrySet()) {
                String pname = e.getKey().toString();
                if (pname.startsWith("dns.map.") && pname.length() > 8) {
                    String name = pname.substring(8);
                    String value = e.getValue().toString();
                    InetAddress addr = InetAddress.getByName(value);
                    mappings.put(name, addr);
                }
            }
            return mappings;
        } catch(UnknownHostException e) {
            // This NetworkNameMapper class is intended primarily for development/testing purposes, so it seems better
            // to not clutter calling code with exception-handling.
            throw new IllegalArgumentException("Unable to install network mappings", e);
        }
    }

    /**
     * Implement the redirects specified in the mapping.
     * <p>
     * The specified mappings are copied, ie mutating them after this install method has been called has no effect.
     * </p>
     */
    public static void install(Map<String, InetAddress> mappings) {
        if (mappings.isEmpty()) {
            return;
        }

        Map<String, InetAddress[]> internalMappings = mappings.entrySet().stream().collect(
	    Collectors.toMap(e -> e.getKey(), e -> new InetAddress[]{e.getValue()}));

        // Avoid race-conditions with other instances of this type. Unfortunately, there is no way to guarantee no
        // race-conditions with other code that is also manipulating the internal InetAddress fields, but that is
        // rather unlikely. Class INetAddress itself initializes field nameServices _very_ early, and never modifies it,
        // so there are no races with INetAddress itself.
        synchronized(NetworkNameMapper.class) {
            try {
                installInternal(internalMappings);
            } catch(NoSuchFieldException|IllegalAccessException e) {
                // Can happen only if a later JRE implementation changes the INetAddress class, or if a
                // SecurityManager class forbids private-field-access.
                throw new RuntimeException("Internal error", e);
            }
        }
    }

    /**
     * Use reflection to add a custom NameService instance to the global list of NameServices used by INetAddress.
     */
    private static void installInternal(Map<String, InetAddress[]> mappings) throws NoSuchFieldException, IllegalAccessException {
        // Use reflection to get access to the list of NameService objects deep within the JRE implementation.
        Class<InetAddress> ic = InetAddress.class;
        Field f = ic.getDeclaredField("nameServices");
        f.setAccessible(true);
        Object currentList = f.get(null);
	@SuppressWarnings("unchecked")
        List<NameService> origNS = (List<NameService>) currentList;

        // Create a new list of NameService objects containing our custom MyNS class followed by all existing NameService objects
        NameService myNs = new MyNS(mappings);
        List<NameService> newNS = new ArrayList<>(origNS.size() + 1);
        newNS.add(myNs);
        newNS.addAll(origNS);

        // Replace the static field with our new class. Updating a reference is atomic, ie INetAddress will never be corrupted.
        // Races with other instances of this type are prevented by method initIntenal. Races with other code that is also
        // manipulating this same field is not prevented, but very unlikely.
        f.set(null, newNS);
    }

    // ====================================================================================================

    /** Simple implementation of the standard NameService interface. */
    private static class MyNS implements NameService {
        private final Map<String, InetAddress[]> mappings;

        private MyNS(Map<String, InetAddress[]> mappings) {
            this.mappings = mappings;
        }

        @Override
        public InetAddress[] lookupAllHostAddr(String s) throws UnknownHostException {
            InetAddress[] result = mappings.get(s);
            if (result != null) {
                return result;
            }

            // cause INetAddress.getAllByName to try the next NameService
            throw new UnknownHostException();
        }

        // Reverse lookups not supported for now..
        @Override
        public String getHostByAddr(byte[] bytes) throws UnknownHostException {
            // cause INetAddress.getAllByName to try the next NameService
            throw new UnknownHostException();
        }
    }
}
