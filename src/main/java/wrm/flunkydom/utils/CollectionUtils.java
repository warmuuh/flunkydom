package wrm.flunkydom.utils;

import java.util.LinkedList;
import java.util.List;

public class CollectionUtils {

  public static <T> List<T> merge(List<T> ... ts) {
    List<T> result = new LinkedList<>();
    for (List<T> t : ts) {
      result.addAll(t);
    }
    return result;
  }

  public static <T> T[] subArray(T[] array, int startIdx) {
    T[] result = (T[]) new Object[array.length - startIdx];
    for (int i = 0; i < array.length; i++) {
      if (i >= startIdx) {
        result[i - startIdx] = array[i];
      }
    }
    return result;
  }
}
