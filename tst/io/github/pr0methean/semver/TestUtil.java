package io.github.pr0methean.semver;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtil {
  @SafeVarargs
  public static <T extends Comparable<T>> void verifySortOrder(boolean strict, T... expectedOrder) {
    for (int i = 0; i < expectedOrder.length; i++) {
      for (int j = 0; j < expectedOrder.length; j++) {
        int comparison = expectedOrder[i].compareTo(expectedOrder[j]);
        if (i == j) {
          assertEquals(0, comparison, expectedOrder[i] + " should equal itself in compareTo");
          assertEquals(expectedOrder[i], expectedOrder[j], expectedOrder[i] + " should equal itself in equals");
        } else {
          if (!strict && comparison == 0) {
            return;
          }
          if (i < j) {
            assertTrue(comparison < 0,
                expectedOrder[i] + " should be less than " + expectedOrder[j]);
          } else {
            assertTrue(comparison > 0,
                expectedOrder[i] + " should be greater than " + expectedOrder[j]);
          }
          assertNotEquals(expectedOrder[i], expectedOrder[j],
              expectedOrder[i] + " should not equal " + expectedOrder[j] + " in equals");
        }
      }
    }
  }
}
