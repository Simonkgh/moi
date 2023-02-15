package example;

import com.google.inject.TypeLiteral;
import com.google.inject.matcher.AbstractMatcher;

/**
 * Compares TypeLiteral<X> against TypeLiteral<Y> and returns true if X is a subclass of Y.
 * 
 * @param <T> is the type of the parameter to the matches method.
 */
public class GuiceSubclassMatcher<T extends TypeLiteral<?>>
    extends AbstractMatcher<T> {

  private TypeLiteral<?> baseType;

  /**
   * Constructor for instances that return true for TypeLiteral<X> where X is a subclass of base.
   */
  public GuiceSubclassMatcher(Class<?> base) {
    baseType = TypeLiteral.get(base);
  }

  /** Compare type T against the type of the class passed to the constructor. */
  public boolean matches(T type) {
    return typeIsSubtypeOf(type, baseType);
  }

  /**
   * Utility method to implement subtype comparisons on TypeLiteral objects; unfortunately they don't provide this
   * built-in.
   */
  private static boolean typeIsSubtypeOf(TypeLiteral<?> subtype, TypeLiteral<?> supertype) {
    if (subtype.equals(supertype)) {
      return true;
    }

    Class<?> superRawType = supertype.getRawType();
    Class<?> subRawType = subtype.getRawType();

    // Test non-generics compatibility
    if (!superRawType.isAssignableFrom(subRawType)) {
      return false;
    }

    // Now find the generic ancestor (if any) which is based on non-generic type superRawType.
    if (!supertype.equals(subtype.getSupertype(superRawType))) {
      return false;
    }

    return true;
  }
}
