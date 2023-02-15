package example;

/**
 * Test the functionality of GenericsUtils.
 */
public class TestGenericsUtils {

    public interface Gadget {
        Long getId();
    }

    public class Widget implements Gadget {
        @Override
        public Long getId() {
            return Long.valueOf(1L);
        }
    }

    public interface Foo<K> {
        K getIt();
    }

    public static abstract class FooBase<L extends Gadget> implements Foo<L> {
        L getIt2() {
            // do nothing
            return null;
        }
    }

    public static class FooImpl extends FooBase<Widget> {
        @Override
        public Widget getIt() {
            return null;
        }
    }

    public static abstract class FooSub<M extends Gadget> extends FooBase<M> {
    }

    public static abstract class FooSub2<P, Q extends Gadget> extends FooBase<Q> {
        String paramToString(P param) {
            return param.toString();
        }
    }

    public static class FooSubImpl extends FooSub<Widget> {
        @Override
        public Widget getIt() {
            return null;
        }
    }

    public static class FooSub2Impl extends FooSub2<String, Widget> {
        @Override
        public Widget getIt() {
            return null;
        }
    }

    /** Poor-man's junit testing :-) */
    public static void main(String args[]) {
        new TestGenericsUtils().testGetGenericTypeFor();
    }

    /**
     * Verify that method GenericsUtils.getGenericTypeFor can walk up the ancestry tree of some concrete class, and
     * determine which concrete type is actually used for the Nth generic type parameter of some base class.
     */
    // @Test
    public void testGetGenericTypeFor() {
        Class<?> x = GenericsUtils.getGenericTypeFor(FooImpl.class, FooBase.class, 0);
        assertEquals(Widget.class, x);
        
        Class<?> y = GenericsUtils.getGenericTypeFor(FooSubImpl.class, FooBase.class, 0);
        assertEquals(Widget.class, y);
        
        Class<?> z = GenericsUtils.getGenericTypeFor(FooSub2Impl.class, FooBase.class, 0);
        assertEquals(Widget.class, z);
    }

    private static void assertEquals(Object o1, Object o2) {
        if (!o1.equals(o2)) {
            System.err.println(
                    String.format(
                            "Unequal objects [%s] and [%s]",
                            o1, o2));
        }
    }
}
