// Author: Simon Kitching
// This code is in the public domain
package net.vonos.testsupport;

import org.junit.Assert;
import java.util.function.Consumer;

/**
 * Some static methods useful for unit-testing.
 * <p>
 * The "assertThrows" methods can be used in a unit-test to assert that a specific method being tested will
 * throw an exception of a specific type, with a specific message. This is an alternative to the junit
 * "@Rule ExpectedException" feature.
 * </p>
 */
public final class TestUtils {
    /**
     * Helper interface needed by the assertThrows methods.
     * <p>
     * Any closure which takes no parameters can be cast to this type.
     * </p>
     */
    @FunctionalInterface
    public interface ThrowableRunnable {
        void run() throws Throwable;
    }

    /**
     * Verify that a method throws an exception with specific values.
     * <p>
     * Example:
     * <code>
     *  TestUtils.assertThrows(SomeException.class, () -> someMethodThatThrows(...))
     * </code>
     * </p>
     *
     * @param throwable is the exception that must be thrown
     * @param runnable is a closure that invokes the method to be tested
     */
    public static <T extends Throwable> void assertThrows(Class<T> throwable, ThrowableRunnable runnable) {
        assertThrows(throwable, runnable, t -> {});
    }

    /**
     * Verify that a method throws an exception with specific values.
     * <p>
     * Example:
     * <code>
     *  TestUtils.assertThrows(SomeException.class,
     *  () -> someMethodThatThrows(...),
     *  (e) -> Assert.assertEquals("some message", e.getMessage()));
     * </code>
     * </p>
     *
     * @param throwable is the exception that must be thrown
     * @param runnable is a closure that invokes the method to be tested
     * @param postCheck is a closure that is passed the thrown exception
     */
    public static <T extends Throwable> void assertThrows(
        Class<T> throwable,
        ThrowableRunnable runnable,
        Consumer<T> postCheck) {

        try {
            runnable.run();
            Assert.fail("No exception was thrown");
        } catch (Throwable t) {
            if (!throwable.isInstance(t)) {
                Assert.fail("Unexpected exception type:" + t.getClass());
            }
            @SuppressWarnings("unchecked")
            T ex = (T) t;
            postCheck.accept(ex);
        }
    }

    /** private constructor - this is a utils class. */
    private TestUtils() {
    }
}
