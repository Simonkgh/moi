package net.vonos.refactor; 

import java.lang.reflect.Method;

/**
 * A helper class for invoking a method on a class without having compile-time access to its type.
 * <p>
 * The intended use for this class is to refactor an application, dividing it into isolated modules.
 * This helper can be used as an intermediate step between calling the class (using compile-time dependency)
 * and invoking it via a REST call.
 * </p>
 * <p>
 * This class uses reflection to invoke a service which is in another domain, but still in the original process.
 * This supports a refactoring step between moving code to a new maven module, and calling it over REST; because
 * the service is called via reflection it is far faster than REST, but the calling module does not need a
 * compile-time dependency on the domain it is invoking.
 * </p>
 * <p>
 * Any code using this helper must eventually be migrated to REST calls, but using this approach as an intermediate
 * phase guarantees that implementing full REST will be an easy step, as all dependencies have definitely been cleanly
 * separated (due to there being no compile-time deps between modules). The new domain can be deployed to production
 * (while not being invoked), then the BtmServiceInvoker calls can be replaced with full REST, and the job is done.
 * </p>
 */
public class BtmServiceInvoker {
    private final String domain;
    private final Object service;
    private final Method method;

    private static Method getMethod(Object service, String methodName, Object... paramTypes) {
        try {
            Class clazz = service.getClass();
            Class<?>[] paramClasses = new Class<?>[paramTypes.length];
            for(int i=0; i<paramTypes.length; ++i) {
                Object pt = paramTypes[i];
                if (pt instanceof Class<?>) {
                    // type accessible from caller, eg Long or List
                    paramClasses[i] = (Class<?>) pt;
                } else if (pt instanceof String) {
                    // presumably type inaccessible to caller at compile-time, eg "net.vonos.domain.xyz.model.SomeType"
                } else {
                    throw new IllegalArgumentException("Paramtype must be Class or String");
                }
            }
            return clazz.getMethod(methodName, paramClasses);
        } catch(NoSuchMethodException|ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Create a helper which can invoke the specified method on the specified service.
     * <p>
     * The paramTypes must match the declared parameters of the desired method, and may be Class objects or string
     * names of classes. JDK native primitive types can be passed using "Integer.TYPE", etc. Due to "type erasure",
     * generic types such as "List<SomeDomainType>" simply require specifying "List.class".
     * </p>
     * <p>
     * The service instance is typically obtained via spring injection using an expression like:
     * <pre>
     *     @Qualifier("someServiceName") Object service
     * </pre>
     * </p>
     */
    public BtmServiceInvoker(String domain, Object service, String methodName, Object... paramTypes) {
        this.domain = domain;
        this.service = service;
        this.method = getMethod(service, methodName, paramTypes);
    }

    /**
     * Invoke the method specified by the constructor parameters.
     * <p>
     * When invoking the method, it is the caller's responsibility to pass types required by the service, which
     * may be domain-types not visible to the caller; the recommended solution is to load the target type
     * with ClassLoader.loadClass(name) then serialize the caller-side parameter to JSON and deserialize it
     * into the desired type. This is pretty much what a REST call would do.
     * </p>
     * <p>
     * If the service return-type is a domain-specific type, then JSON-serialize the returned object and deserialize
     * into the appropriate caller-side type.
     * </p>
     */
    public Object invoke(Object... params) throws DomainServiceInvocationException {
        try {
            return method.invoke(service, params);
        } catch(ReflectiveOperationException e) {
            throw new DomainServiceInvocationException(
                    domain,
                    service.getClass().getName(),
                    method.getName(),
                    e);
        }
    }
}

