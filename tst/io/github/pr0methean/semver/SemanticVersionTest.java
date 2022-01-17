package io.github.pr0methean.semver;

import com.google.common.primitives.UnsignedLong;
import nl.jqno.equalsverifier.EqualsVerifier;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import static com.google.common.primitives.UnsignedLong.MAX_VALUE;
import static com.google.common.primitives.UnsignedLong.ZERO;
import static io.github.pr0methean.semver.SemanticVersion.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class SemanticVersionTest {
  private static final String[] TEST_VERSIONS_FOR_SORTING = {
      "1.0.0-0",
      "1.0.0-0.0",
      "1.0.0-0.1",
      "1.0.0-0a",
      "1.0.0-0a.0",
      "1.0.0-0a.1",
      "1.0.0-1",
      "1.0.0-1.0",
      "1.0.0-1.1",
      "1.0.0-1a",
      "1.0.0-1a.0",
      "1.0.0-1a.1",
      "1.0.0-9",
      "1.0.0-10",
      "1.0.0-" + MAX_VALUE,
      "1.0.0-alpha",
      "1.0.0-alpha.0",
      "1.0.0-alpha.1",
      "1.0.0-alpha.beta",
      "1.0.0-beta",
      "1.0.0-beta.2",
      "1.0.0-beta.9",
      "1.0.0-beta.10",
      "1.0.0-beta.11",
      "1.0.0-rc.1",
      "1.0.0",
      "1.0.1",
      "1.1.0",
      "1.1.1",
      "2.0.0"};
  private static final String[] INVALID_VERSIONS_EVEN_LENIENT = {"","-","-1","-pre1",".","..","..1","q.w.e","..q","q..","w.e","+"};
  private static final String[] LENIENT_VERSIONS = {"1",".9",".9.0","0.1.2.3","0.1.2-"};
  private static final UnsignedLong[] TEST_VERSIONS = {
      ZERO,
      UnsignedLong.ONE,
      UnsignedLong.fromLongBits(Long.MAX_VALUE),
      UnsignedLong.fromLongBits(Long.MIN_VALUE),
      MAX_VALUE.minus(UnsignedLong.ONE),
      MAX_VALUE};

  @Test
  public void testEqualsAndCompareTo() {
    TestUtil.verifySortOrder(
        true, Arrays.stream(TEST_VERSIONS_FOR_SORTING).map(SemanticVersion::valueOf).toArray(SemanticVersion[]::new));
  }
  @Test
  public void testEqualsAndHashCodeConsistent() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion v1 = SemanticVersion.valueOf(versionString);
      SemanticVersion v2 = SemanticVersion.valueOf(versionString);
      assertEquals(v1, v2, "Copies of " + versionString + " compare as unequal");
      assertEquals(v1.hashCode(), v2.hashCode(), "Two different hash codes found for " + versionString);
    }
  }
  @Test
  public void testWithEqualsVerifier() {
    EqualsVerifier.forClass(SemanticVersionImpl.class).verify();
  }
  @Test
  public void testToString() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      assertEquals(versionString, valueOf(versionString).toString());
    }
  }
  @Test
  public void testLenientRoundTripIdempotent() {
    for (String versionString : LENIENT_VERSIONS) {
      SemanticVersion version = valueOf(versionString, true);
      String correctedVersionString = version.toString();
      assertEquals(version, valueOf(correctedVersionString));
      assertEquals(version, valueOf(correctedVersionString, true));
      assertEquals(correctedVersionString, valueOf(correctedVersionString).toString());
    }
  }

  @Test
  public void testInvalidRejectedStrict() {
    assertNotValidVersions(INVALID_VERSIONS_EVEN_LENIENT, false);
    assertNotValidVersions(LENIENT_VERSIONS, false);
  }

  private void assertNotValidVersions(String[] versionStrings, boolean lenient) {
    for (String versionString : versionStrings) {
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString, lenient));
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString + '+', lenient));
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString + randomBuildMetadata(), lenient));
    }
  }

  @Test
  public void testInvalidRejectedLenient() {
    assertNotValidVersions(INVALID_VERSIONS_EVEN_LENIENT, true);
  }

  @Test
  public void testValueOfStrict() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion baseReleaseVersion = valueOf(versionString);
      assertEquals(baseReleaseVersion, baseReleaseVersion.withBuildMetadata(null));
      assertEquals(baseReleaseVersion, baseReleaseVersion.withBuildMetadata(null, true));
      assertEquals(baseReleaseVersion, baseReleaseVersion.withBuildMetadata("", true));
      assertThrows(IllegalArgumentException.class, () -> baseReleaseVersion.withBuildMetadata(""));
      assertNotEquals(baseReleaseVersion.withBuildMetadata(randomIdentifier()), baseReleaseVersion);
      assertThrows(IllegalArgumentException.class,
          () -> baseReleaseVersion.withBuildMetadata("+" + randomIdentifier()));
      assertThrows(IllegalArgumentException.class,
          () -> baseReleaseVersion.withBuildMetadata(randomIdentifier() + "+"));
    }
    for (String versionString : LENIENT_VERSIONS) {
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString));
    }
  }

  @Test
  public void testValueOfLenient() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion lenientReleaseVersion = valueOf(versionString, true);
      SemanticVersion lenientReleaseVersionPlus = valueOf(versionString + '+', true);
      assertEquals(lenientReleaseVersion, lenientReleaseVersionPlus);
      SemanticVersion withBuildMetadata = valueOf(versionString + randomBuildMetadata(), true);
      assertNotEquals(lenientReleaseVersion, withBuildMetadata);
      assertNotEquals(0, TOTAL_ORDERING.compare(lenientReleaseVersion, withBuildMetadata));
    }
    for (String versionString : LENIENT_VERSIONS) {
      valueOf(versionString, true);
    }
  }
  private static String randomBuildMetadata() {
    return "+" + randomIdentifier();
  }

  private static String randomIdentifier() {
    return Long.toString(ThreadLocalRandom.current().nextLong(), 16);
  }

  @Test
  public void testClone() {
    for (String version : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion baseVersion = valueOf(version);
      SemanticVersion baseVersionClone = baseVersion.clone();
      assertEquals(baseVersion, baseVersionClone);
      assertEquals(0, TOTAL_ORDERING.compare(baseVersion, baseVersionClone));
      SemanticVersion withMetadata = valueOf(version + randomBuildMetadata());
      SemanticVersion withMetadataClone = withMetadata.clone();
      assertEquals(withMetadata, withMetadataClone);
      assertEquals(0, TOTAL_ORDERING.compare(withMetadata, withMetadataClone));
      assertNotEquals(baseVersion, withMetadataClone);
      assertNotEquals(baseVersionClone, withMetadata);
      assertNotEquals(baseVersionClone, withMetadataClone);
    }
  }

  @Test
  public void testBuildMetadataIgnored() {
    for (String version : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion baseVersion = valueOf(version);
      SemanticVersion versionWithBuildMetadata1 = valueOf(version + randomBuildMetadata());
      SemanticVersion versionWithBuildMetadata2 = valueOf(version + randomBuildMetadata());
      assertNotEquals(baseVersion, versionWithBuildMetadata1, "equals shouldn't ignore build metadata");
      assertEquals(0, baseVersion.compareTo(versionWithBuildMetadata1),
          "compareTo should ignore build metadata");
      assertNotEquals(versionWithBuildMetadata1, versionWithBuildMetadata2,
          "equals shouldn't ignore build metadata");
      assertEquals(0, versionWithBuildMetadata1.compareTo(versionWithBuildMetadata2),
          "compareTo should ignore build metadata");
      assertNotEquals(0, TOTAL_ORDERING.compare(versionWithBuildMetadata1, versionWithBuildMetadata2),
          "TOTAL_ORDERING shouldn't ignore build metadata");
      assertNotEquals(0, TOTAL_ORDERING.compare(baseVersion, versionWithBuildMetadata1),
          "TOTAL_ORDERING shouldn't ignore build metadata");
    }
  }

  @Test
  public void testEmptyMetadata() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString + "+"));
      assertEquals(valueOf(versionString), valueOf(versionString + "+", true));
    }
  }

  @Test
  public void testNextPrerelease() {
    for (int i = 0; i < TEST_VERSIONS_FOR_SORTING.length - 1; i++) {
      for (int j = i + 1; j < TEST_VERSIONS_FOR_SORTING.length; j++) {
        String currentVersionString = TEST_VERSIONS_FOR_SORTING[i];
        SemanticVersion currentVersion = valueOf(currentVersionString);
        String nextReleaseString = TEST_VERSIONS_FOR_SORTING[j];
        SemanticVersion nextRelease = valueOf(nextReleaseString);
        if (currentVersion.isPrerelease()) {
          assertTrue(currentVersion.compareTo(currentVersion.nextPrereleaseBefore(null)) < 0);
        } else {
          assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(null));
        }
        verifyPrereleaseSortOrder(currentVersion, nextRelease);
        verifyPrereleaseSortOrder(currentVersion, nextRelease.nextPatchRelease());
        verifyPrereleaseSortOrder(currentVersion, nextRelease.nextMinorRelease());
        verifyPrereleaseSortOrder(currentVersion, nextRelease.nextMajorRelease());
      }
    }
  }

  private Optional<SemanticVersion> checkedVersionBetween(SemanticVersion currentVersion, SemanticVersion nextRelease) {
    String currentVersionString = currentVersion.toString();
    String nextPossibleVersion = currentVersionString +
        (currentVersionString.contains("-") ? ".0" : "-0");
    if (nextRelease.toString().equals(nextPossibleVersion)) {
      assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(nextRelease));
      return Optional.empty();
    }
    SemanticVersion between = currentVersion.nextPrereleaseBefore(nextRelease);
    assertIsPrerelease(between);
    assertTrue(currentVersion.compareTo(between) < 0);
    assertTrue(between.compareTo(nextRelease) < 0);
    int expectedMaxLength = Math.max(nextPossibleVersion.length(), nextRelease.toString().length());
    assertTrue(between.toString().length() <= expectedMaxLength,
        between + " (between " + currentVersion + " and " + nextRelease + ") shouldn't be longer than both "
            + nextPossibleVersion + " and " + nextRelease);
    return Optional.of(between);
  }

  private void verifyPrereleaseSortOrder(SemanticVersion currentVersion, SemanticVersion nextRelease) {
    Optional<SemanticVersion> nextPrerelease = checkedVersionBetween(currentVersion, nextRelease);
    Optional<SemanticVersion> inBetween = nextPrerelease.flatMap(v -> checkedVersionBetween(currentVersion, v));
    Optional<SemanticVersion> prereleaseAfterNext = nextPrerelease.flatMap(v -> checkedVersionBetween(v, nextRelease));
    Optional<SemanticVersion> inBetween2 = (nextPrerelease.isPresent() && prereleaseAfterNext.isPresent())
        ? checkedVersionBetween(nextPrerelease.get(), prereleaseAfterNext.get()) : Optional.empty();
    Optional<SemanticVersion> prerelease3After = prereleaseAfterNext.flatMap(v -> checkedVersionBetween(v, nextRelease));
    Optional<SemanticVersion> inBetween3 = (prereleaseAfterNext.isPresent() && prerelease3After.isPresent())
        ? checkedVersionBetween(prereleaseAfterNext.get(), prerelease3After.get()) : Optional.empty();
    verifySortOrder(
        Optional.of(currentVersion),
        inBetween,
        nextPrerelease,
        inBetween2,
        prereleaseAfterNext,
        inBetween3,
        prerelease3After,
        Optional.of(nextRelease));
  }

  @SafeVarargs
  private static void verifySortOrder(Optional<SemanticVersion>... optionals) {
    TestUtil.verifySortOrder(
        true, Arrays.stream(optionals)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .toArray(SemanticVersion[]::new));
  }

  @SuppressWarnings("AssertBetweenInconvertibleTypes")
  private static void assertIsPrerelease(SemanticVersion prerelease) {
    assertNotEquals(prerelease, null);
    assertNotEquals(prerelease, prerelease.toString());
    assertNotEquals(prerelease, prerelease.releaseVersion());
    assertTrue(prerelease.isPrerelease());
    List<String> prereleaseVersion = prerelease.prereleaseVersion();
    assertNotNull(prereleaseVersion);
    assertTrue(prereleaseVersion.size() > 0);
    for (String s : prereleaseVersion) {
      assertNotNull(s);
      assertFalse(s.isEmpty());
    }
  }

  @Test
  public void testBasicAccessors() {
    for (UnsignedLong major : TEST_VERSIONS) {
      for (UnsignedLong minor : TEST_VERSIONS) {
        for (UnsignedLong patch : TEST_VERSIONS) {
          SemanticVersion releaseVersion = SemanticVersion.valueOf(major, minor, patch, null, null);
          SemanticVersion releaseVersionCopy1 = SemanticVersion.valueOf(major, minor, patch, null, "");
          SemanticVersion releaseVersionCopy2 = SemanticVersion.valueOf(major, minor, patch, List.of(), null);
          assertEquals(releaseVersion, releaseVersionCopy1);
          assertEquals(releaseVersion, releaseVersionCopy2);
          assertEquals(major, releaseVersion.majorVersion());
          assertEquals(minor, releaseVersion.minorVersion());
          assertEquals(patch, releaseVersion.patchVersion());
          assertNull(releaseVersion.buildMetadata());
          assertNotPrerelease(releaseVersion);
          assertTrue(releaseVersion.compareTo(MIN_VALUE) > 0); // MIN_VALUE is a prerelease of 0.0.0
          assertTrue(releaseVersion.compareTo(SemanticVersion.MAX_VALUE) <= 0);
          String buildMetadata = randomIdentifier();
          SemanticVersion releaseVersionPlusMetadata = releaseVersion.withBuildMetadata(buildMetadata);
          SemanticVersion releaseVersionPlusMetadataOtherMethod = SemanticVersion.valueOf(major, minor, patch, null, buildMetadata);
          assertEquals(releaseVersionPlusMetadata, releaseVersionPlusMetadataOtherMethod);
          assertNotEquals(releaseVersion, releaseVersionPlusMetadata);
          assertEquals(releaseVersion + "+" + buildMetadata, releaseVersionPlusMetadata.toString());
          assertEquals(major, releaseVersionPlusMetadata.majorVersion());
          assertEquals(minor, releaseVersionPlusMetadata.minorVersion());
          assertEquals(patch, releaseVersionPlusMetadata.patchVersion());
          assertEquals(0, releaseVersion.compareTo(releaseVersionPlusMetadata));
          assertNotPrerelease(releaseVersionPlusMetadata);
          assertNotEquals(0, TOTAL_ORDERING.compare(releaseVersion, releaseVersionPlusMetadata));
          List<String> prereleaseIds = List.of(randomIdentifier());
          SemanticVersion prerelease = releaseVersion.prereleaseWithIdentifiers(prereleaseIds);
          SemanticVersion prereleaseOtherMethod = valueOf(major, minor, patch, prereleaseIds, null);
          assertEquals(prerelease, prereleaseOtherMethod);
          assertEquals(releaseVersion + "-" + prereleaseIds.get(0), prerelease.toString());
          assertEquals(major, prerelease.majorVersion());
          assertEquals(minor, prerelease.minorVersion());
          assertEquals(patch, prerelease.patchVersion());
          assertNotEquals(releaseVersion, prerelease);
          assertTrue(prerelease.compareTo(releaseVersion) < 0);
        }
      }
    }
  }

  @Test
  public void testNextMajorMinorPatch() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion version = valueOf(versionString);
      try {
        SemanticVersion nextPatch = version.nextPatchRelease();
        assertNull(nextPatch.buildMetadata());
        assertNotPrerelease(nextPatch);
        assertTrue(version.compareTo(nextPatch) < 0);
        assertTrue(version.compareTo(version.nextPrereleaseBefore(nextPatch)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.patchVersion(), MAX_VALUE);
      }
      try {
        SemanticVersion nextMinor = version.nextMinorRelease();
        assertNull(nextMinor.buildMetadata());
        assertNotPrerelease(nextMinor);
        assertTrue(version.compareTo(nextMinor) < 0);
        assertTrue(version.compareTo(version.nextPrereleaseBefore(nextMinor)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.minorVersion(), MAX_VALUE);
      }
      try {
        SemanticVersion nextMajor = version.nextMajorRelease();
        assertNull(nextMajor.buildMetadata());
        assertNotPrerelease(nextMajor);
        assertTrue(version.compareTo(nextMajor) < 0);
        assertTrue(version.compareTo(version.nextPrereleaseBefore(nextMajor)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.majorVersion(), MAX_VALUE);
      }
    }
  }

  @Test
  public void testPrereleaseWithIdentifiers() {
    for (String versionString : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion baseVersion = valueOf(versionString);
      SemanticVersion notPrerelease = baseVersion.prereleaseWithIdentifiers(null);
      assertNotPrerelease(notPrerelease);
      SemanticVersion prerelease = baseVersion.prereleaseWithIdentifiers(List.of("alpha","1"));
      assertTrue(prerelease.isPrerelease());
      assertTrue(prerelease.compareTo(notPrerelease) < 0);
      assertEquals(baseVersion.majorVersion(), notPrerelease.majorVersion());
      assertEquals(baseVersion.minorVersion(), notPrerelease.minorVersion());
      assertEquals(baseVersion.patchVersion(), notPrerelease.patchVersion());
      assertEquals(baseVersion.majorVersion(), prerelease.majorVersion());
      assertEquals(baseVersion.minorVersion(), prerelease.minorVersion());
      assertEquals(baseVersion.patchVersion(), prerelease.patchVersion());
    }
  }

  private static void assertNotPrerelease(SemanticVersion version) {
    assertFalse(version.isPrerelease());
    assertEquals(version, version.releaseVersion());
    assertNull(version.prereleaseVersion());
  }

  private static SemanticVersion createMock(UnsignedLong major, UnsignedLong minor, UnsignedLong patch,
      List<String> prerelease) {
    SemanticVersion mock = Mockito.mock(SemanticVersion.class);
    when (mock.majorVersion()).thenReturn(major);
    when (mock.majorVersionUnsignedUnboxed()).thenReturn(major.longValue());
    when (mock.minorVersion()).thenReturn(minor);
    when (mock.minorVersionUnsignedUnboxed()).thenReturn(minor.longValue());
    when (mock.patchVersion()).thenReturn(patch);
    when (mock.patchVersionUnsignedUnboxed()).thenReturn(patch.longValue());
    when (mock.isPrerelease()).thenReturn(prerelease != null);
    when (mock.prereleaseVersion()).thenReturn(prerelease);
    when (mock.compareTo(any(SemanticVersion.class))).thenCallRealMethod();
    when (mock.toString()).thenReturn(major + "." + minor + "." + patch +
        (prerelease == null ? "" : "-" + String.join(".", prerelease)));
    return mock;
  }

  @Test
  public void testNonStandardImplementations() {
    SemanticVersion mockMinimum = createMock(ZERO, ZERO, ZERO, List.of("0"));
    SemanticVersion mockMaximum = createMock(MAX_VALUE, MAX_VALUE, MAX_VALUE, null);
    for (UnsignedLong major : TEST_VERSIONS) {
      for (UnsignedLong minor : TEST_VERSIONS) {
        for (UnsignedLong patch : TEST_VERSIONS) {
          SemanticVersion releaseVersion = SemanticVersion.valueOf(major, minor, patch, null, null);
          boolean isMax = releaseVersion.equals(mockMaximum);
          boolean isMin = releaseVersion.equals(mockMinimum);
          assertFalse(isMax && isMin);
          TestUtil.verifySortOrder(!isMax && !isMin, mockMinimum, releaseVersion, mockMaximum);

          SemanticVersion releaseVersionMockCopy = createMock(major, minor, patch, null);
          assertEquals(0, releaseVersion.compareTo(releaseVersionMockCopy));
          TestUtil.verifySortOrder(!isMax && !isMin, mockMinimum, releaseVersionMockCopy, mockMaximum);
          SemanticVersion preReleaseVersion1 = createMock(major, minor, patch, List.of("1"));
          SemanticVersion preReleaseVersion2 = createMock(major, minor, patch, List.of("1","0"));
          SemanticVersion preReleaseVersion3 = createMock(major, minor, patch, List.of("alpha"));
          TestUtil.verifySortOrder(true, preReleaseVersion1, preReleaseVersion2, preReleaseVersion3, releaseVersionMockCopy);
          assertThrows(IllegalArgumentException.class, () -> releaseVersion.nextPrereleaseBefore(mockMinimum));
          if (releaseVersion.compareTo(mockMaximum) == 0) {
            assertThrows(IllegalArgumentException.class, () -> releaseVersion.nextPrereleaseBefore(mockMaximum));
          } else {
            SemanticVersion preReleaseAfter = releaseVersion.nextPrereleaseBefore(mockMaximum);
            assertTrue(preReleaseAfter.isPrerelease());
            TestUtil.verifySortOrder(true, releaseVersion, preReleaseAfter, mockMaximum);
            TestUtil.verifySortOrder(true, releaseVersionMockCopy, preReleaseAfter, mockMaximum);
          }
        }
      }
    }
  }

  @Test
  public void testOverflows() {
    SemanticVersion maxVersion = valueOf(MAX_VALUE, MAX_VALUE, MAX_VALUE, null, null);
    assertThrows(IllegalArgumentException.class, maxVersion::nextMajorRelease);
    assertThrows(IllegalArgumentException.class, maxVersion::nextMinorRelease);
    assertThrows(IllegalArgumentException.class, maxVersion::nextPatchRelease);
    SemanticVersion maxMinor = valueOf(ZERO, MAX_VALUE, MAX_VALUE, null, null);
    maxMinor.nextMajorRelease();
    assertThrows(IllegalArgumentException.class, maxMinor::nextMinorRelease);
    assertThrows(IllegalArgumentException.class, maxMinor::nextPatchRelease);
    SemanticVersion maxPatch = valueOf(ZERO, ZERO, MAX_VALUE, null, null);
    maxPatch.nextMajorRelease();
    maxPatch.nextMinorRelease();
    assertThrows(IllegalArgumentException.class, maxPatch::nextPatchRelease);
  }
}