package io.github.pr0methean.semver;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.OptionalLong;

import static io.github.pr0methean.semver.PrereleaseIdentifier.valueOf;
import static org.junit.jupiter.api.Assertions.*;

public class PrereleaseIdentifierTest {
  private static final String[] CHUNK_STRINGS_TO_TEST_IN_SORT_ORDER = {"1","2","2a","10","alpha","beta"};
  @Test
  public void testEqualsAndCompareTo() {
    TestUtil.verifySortOrder(true, Arrays.stream(CHUNK_STRINGS_TO_TEST_IN_SORT_ORDER).map(PrereleaseIdentifier::valueOf)
        .toArray(PrereleaseIdentifier[]::new));
  }

  @Test
  public void testWithEqualsVerifier() {
    EqualsVerifier.forRelaxedEqualExamples(
            new PrereleaseIdentifier(false, 1, "foo"),
            new PrereleaseIdentifier(false, 2, "foo"))
        .andUnequalExamples(
            new PrereleaseIdentifier(true, 1, "foo"),
            new PrereleaseIdentifier(true, 2, "foo"),
            new PrereleaseIdentifier(false, 1, "foo-dog"),
            new PrereleaseIdentifier(false, 1, "bar"))
        .verify();
  }

  @Test
  public void testToString() {
    for (String chunkString : CHUNK_STRINGS_TO_TEST_IN_SORT_ORDER) {
      assertEquals(chunkString, PrereleaseIdentifier.valueOf(chunkString).toString());
    }
  }

  @Test
  public void testExtractPartsNumericAndSuffix() {
    PrereleaseIdentifier chunk = PrereleaseIdentifier.valueOf("123a");
    assertTrue(chunk.hasNumericPart());
    assertEquals(123, chunk.numericPart());
    assertEquals(OptionalLong.of(123), chunk.numericPartIfPresent());
    assertEquals("a", chunk.suffix());
  }

  @Test
  public void testExtractPartsNumericOnly() {
    PrereleaseIdentifier chunk = PrereleaseIdentifier.valueOf("123");
    assertTrue(chunk.hasNumericPart());
    assertEquals(123, chunk.numericPart());
    assertEquals(OptionalLong.of(123), chunk.numericPartIfPresent());
    assertEquals("", chunk.suffix());
  }

  @Test
  public void testExtractPartsSuffixOnly() {
    PrereleaseIdentifier chunk = PrereleaseIdentifier.valueOf("a");
    assertFalse(chunk.hasNumericPart());
    assertEquals(OptionalLong.empty(), chunk.numericPartIfPresent());
    assertEquals("a", chunk.suffix());
  }

  @Test
  public void testInvalidCharsRejected() {
    assertThrows(IllegalArgumentException.class, () -> valueOf("+"));
    assertThrows(IllegalArgumentException.class, () -> valueOf("."));
    assertThrows(IllegalArgumentException.class, () -> valueOf(" "));
    assertThrows(IllegalArgumentException.class, () -> valueOf("\uD83D\uDCA9"));
  }

  @Test
  public void testEmptyRejected() {
    assertThrows(IllegalArgumentException.class, () -> valueOf(""));
  }
}