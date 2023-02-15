import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Calendar;
import java.util.List;
import java.text.SimpleDateFormat;

// Define a "function object" that takes one param and returns one value.
// Alas, a different interface type is needed for different #s of params.
interface ClosureWith1Param<OUT, IN> {
  OUT invoke(IN param1);
}

class StringTransformerClosure implements ClosureWith1Param<String, String> {
  private String prefix;
  private StringBuilder postfix;

  StringTransformerClosure(String prefix, StringBuilder postfix) {
    this.prefix = prefix;
    this.postfix = postfix;
  }

  @Override
  public String invoke(String in) {
    return prefix + in + postfix;
  }
};

public class ClosureExample1 {
  private static List<String> baseStrings = Arrays.asList("Day", "Month",
      "Year");
  private static StringBuilder postfix = new StringBuilder("."); // mutable
                                  // string

  private static ClosureWith1Param<String, String> makeStringTransformer(
      String prefix) {
    // return a "closure" that "captures" the prefix and postfix values
    return new StringTransformerClosure(prefix, postfix);
  }

  public static List<String> map(List<String> src,
      ClosureWith1Param<String, String> closure) {
    ArrayList<String> out = new ArrayList<String>(src.size());
    for (String str : src) {
      out.add(closure.invoke(str));
    }
    return out;
  }

  private static void showList(List<?> in) {
    for (Object o : in) {
      System.out.print(o);
      System.out.print(" ");
    }
    System.out.println();
  }

  public static void main(String[] args) throws Exception {
    ClosureWith1Param<String, String> closure1 = makeStringTransformer("Previous ");
    ClosureWith1Param<String, String> closure2 = makeStringTransformer("Next ");

    List<String> mapped1 = map(baseStrings, closure1);
    showList(mapped1);

    List<String> mapped2 = map(baseStrings, closure2);
    showList(mapped2);

    postfix.setLength(0);
    postfix.append("!");
    List<String> mapped3 = map(baseStrings, closure2);
    showList(mapped3);
  }
}

