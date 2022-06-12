package io.github.pr0methean.semver;

import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

public class TestUtil {
  @SuppressWarnings("unchecked")
  @SafeVarargs
  public static <T extends Comparable<T>> void verifySortOrder(boolean strict, T... expectedOrder) {
    for (int i = 0; i < expectedOrder.length; i++) {
      for (int j = 0; j < expectedOrder.length; j++) {
        int comparison = expectedOrder[i].compareTo(expectedOrder[j]);
        if (i == j) {
          assertEquals(0, comparison, expectedOrder[i] + " should equal itself in compareTo");
          assertEquals(expectedOrder[i], expectedOrder[i], expectedOrder[i] + " should equal itself in equals");
          if (expectedOrder[i] instanceof SemanticVersion
              && !Mockito.mockingDetails(expectedOrder[i]).isMock()) {
            SemanticVersion clone = ((SemanticVersion) expectedOrder[i]).clone();
            assertEquals(expectedOrder[i], clone, expectedOrder[i] + " should equal its clone in equals");
            assertEquals(0, expectedOrder[i].compareTo((T) clone),
                expectedOrder[i] + " should equal its clone in compareTo");
          }
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
