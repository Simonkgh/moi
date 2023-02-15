package example;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

public class GenericsUtils {

    private GenericsUtils() {
    }

    /**
     * Walk up the ancestry tree of some concrete class, and determine which concrete type is actually used for the
     * Nth generic type parameter of a specified base class.
     */
    public static Class<?> getGenericTypeFor(Class<?> clazz, Class<?> baseClazz, int index) {

        // Given:
        //    interface Foo<A>
        //    abstract class FooBase<B> implements Foo<B> 
        //    abstract class FooSub<C> extends FooBase<C>
        //    class FooImpl extends FooSub<SomeConcreteType>
        //
        // * To obtain generics-related information about FooImpl requires the somewhat obscure expression
        //   FooImp.class.getGenericSuperclass() which returns a ParameterizedType object representing
        //   FooSub<SomeConcreteType>.
        //
        // * Unfortunately, a ParameterizedType does not have a "getParent" method; obtaining generics information
        //   about the next ancestor type is obtained via FooSub.class.getGenericSuperclass - which returns a
        //   ParameterizedType object representing FooBase<C>. We must then figure out which type C is mapped to.
        //
        // When processing the first generic ancestor type, all "type parameters" are concrete types; these types are
        // kept in an array. On each subsequent step up the type hierarchy, match the "abstract" generic type parameters
        // by position with the corresponding concrete type from the previous pass. In effect, type parameters work
        // somewhat like method parameters: if code calls someFn(1,"hello",2) and the called method is declared as
        // someFn(a,b,c) then a->1, b->"hello", c->2. In a similar manner, if a type FooSub<Integer> extends a
        // type FooBase<C> then C->Integer.

        Class<?> currClazz = clazz;
        Map<String,Class<?>> currTypesByVar = new HashMap<>();
        
        while (currClazz != null) {
            Class<?> superClazz = currClazz.getSuperclass();
            Type superType = currClazz.getGenericSuperclass();

            if (superType instanceof ParameterizedType) {
                // parent type is generic
                ParameterizedType pt = (ParameterizedType) superType;

                // Get the values passed *by* the child type *to* the parent type. These are all concrete types
                // for a non-generic type (ie a class with concrete types for all generic parameters), and is a
                // mix of concrete types and variables for abstract generic types.
                Type[] paramsFrom = pt.getActualTypeArguments();

                // Get the generic variable *names* used in the declaration
                TypeVariable<?> paramsTo[] = superClazz.getTypeParameters();

                if (paramsFrom.length != paramsTo.length) {
                    // AFAIK, should never happen - like finding a method call passing a different number
                    // of parameters than the called method declares.
                    throw new IllegalStateException("Unexpected params");
                }
                Map<String,Class<?>> superTypesByVar = new HashMap<>();

                for(int i=0; i<paramsFrom.length; ++i) {
                    String paramToName = paramsTo[i].getName();
                    Type paramFrom = paramsFrom[i];
                    if (paramFrom instanceof TypeVariable) {
                        // This is a mapping from varname to varname.
                        String paramFromName = ((TypeVariable<?>) paramFrom).getName();
                        superTypesByVar.put(paramToName, currTypesByVar.get(paramFromName));
                    } else {
                        // This is a mapping from concrete type to varname; add now.
                        superTypesByVar.put(paramToName, (Class<?>) paramFrom);
                    }
                }

                currTypesByVar = superTypesByVar;

                if (superClazz == baseClazz) {
                    if (paramsTo.length < index+1) {
                        throw new IllegalArgumentException(
                                "Invalid index into generic types on type " + baseClazz.getName());
                    }
                    String pname = paramsTo[index].getName();
                    return currTypesByVar.get(pname);
                }
            } else {
                // parent type is not generic (superType is a simple Class object)
                currTypesByVar.clear();
            }

            currClazz = superClazz;
        }

        throw new IllegalArgumentException(
                String.format(
                        "Type %s does not inherit from %s",
                        clazz.getName(), baseClazz.getName()));
    }
}
