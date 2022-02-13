package io.github.pr0methean.semver;

import nl.jqno.equalsverifier.EqualsVerifier;
import org.checkerframework.checker.signedness.qual.Unsigned;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Stream;

import static io.github.pr0methean.semver.SemanticVersion.*;
import static java.lang.Long.toUnsignedString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@Execution(ExecutionMode.CONCURRENT)
public class SemanticVersionTest {
  @Unsigned
  private static final long MAX_UNSIGNED_LONG = -1; // bits are all 1s
  private static final String MAX_UNSIGNED_LONG_STRING = toUnsignedString(MAX_UNSIGNED_LONG);
  private static final String MAX_UNSIGNED_LONG_MINUS_ONE_STRING = toUnsignedString(MAX_UNSIGNED_LONG - 1);
  private static final String[] TEST_VERSION_STRINGS_FOR_SORTING = {
      "1.0.0-0",
      "1.0.0-0.0",
      "1.0.0-0.1",
      "1.0.0-0a",
      "1.0.0-0a.0",
      "1.0.0-0a.1",
      "1.0.0-0beta",
      "1.0.0-1",
      "1.0.0-1.0",
      "1.0.0-1.0.0",
      "1.0.0-1.1",
      "1.0.0-1.1.0",
      "1.0.0-1.1.0a",
      "1.0.0-1.1.2",
      "1.0.0-1a",
      "1.0.0-1a.0",
      "1.0.0-1a.1",
      "1.0.0-1b",
      "1.0.0-3",
      "1.0.0-3a",
      "1.0.0-3b",
      "1.0.0-9",
      "1.0.0-10",
      "1.0.0-" + MAX_UNSIGNED_LONG_STRING,
      "1.0.0-alpha",
      "1.0.0-alpha.0",
      "1.0.0-alpha.1",
      "1.0.0-alpha.1a",
      "1.0.0-alpha.1a.bravo",
      "1.0.0-alpha.5",
      "1.0.0-alpha.5a",
      "1.0.0-alpha.5a.alpha",
      "1.0.0-alpha.5a.bravo",
      "1.0.0-alpha." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING,
      "1.0.0-alpha." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + "a",
      "1.0.0-alpha." + MAX_UNSIGNED_LONG_STRING,
      "1.0.0-alpha." + MAX_UNSIGNED_LONG_STRING + "a",
      "1.0.0-alpha.beta",
      "1.0.0-beta",
      "1.0.0-beta.0",
      "1.0.0-beta.2",
      "1.0.0-beta.9",
      "1.0.0-beta.10",
      "1.0.0-beta.11",
      "1.0.0-beta." + MAX_UNSIGNED_LONG_STRING,
      "1.0.0-beta." + MAX_UNSIGNED_LONG_STRING + ".1",
      "1.0.0-beta." + MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_MINUS_ONE_STRING,
      "1.0.0-beta." + MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING,
      "1.0.0-beta." + MAX_UNSIGNED_LONG_STRING + "a",
      "1.0.0-beta0",
      "1.0.0-beta1",
      "1.0.0-rc.1",
      "1.0.0",
      "1.0.1",
      "1.0." + Long.MAX_VALUE,
      "1.0." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING,
      "1.0." + MAX_UNSIGNED_LONG_STRING,
      "1.1.0",
      "1.1.1",
      "1." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + ".0",
      "1." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + '.' + MAX_UNSIGNED_LONG_STRING + "-0",
      "1." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + '.' + MAX_UNSIGNED_LONG_STRING + "-0.1",
      "1." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + '.' + MAX_UNSIGNED_LONG_STRING + "-0a",
      "1." + MAX_UNSIGNED_LONG_MINUS_ONE_STRING + '.' + MAX_UNSIGNED_LONG_STRING,
      "1." + MAX_UNSIGNED_LONG_STRING + ".0-" + MAX_UNSIGNED_LONG_STRING,
      "1." + MAX_UNSIGNED_LONG_STRING + ".0",
      "1." + MAX_UNSIGNED_LONG_STRING + ".1",
      "1." + MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING + "-a",
      "1." + MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING,
      "2.0.0",
      Long.MAX_VALUE + ".0.0",
      MAX_UNSIGNED_LONG_MINUS_ONE_STRING + ".0.0",
      MAX_UNSIGNED_LONG_STRING + ".0.0",
      MAX_UNSIGNED_LONG_STRING + ".0." + MAX_UNSIGNED_LONG_STRING,
      MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING + ".0",
      MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING + '.' + MAX_UNSIGNED_LONG_STRING,
      };
  private static final SemanticVersion[] TEST_VERSIONS_FOR_SORTING = Arrays.stream(TEST_VERSION_STRINGS_FOR_SORTING)
      .map(SemanticVersion::valueOf)
      .toArray(SemanticVersion[]::new);
  private static final String[] INVALID_VERSIONS_EVEN_LENIENT = {"","-","-1","-pre1",".","..","..1","q.w.e","..q","q..","w.e","+"};
  private static final String[] LENIENT_VERSIONS = {"1",".9",".9.0","0.1.2.3","0.1.2-"};
  private static final long[] TEST_VERSIONS = {
      0,
      1,
      Long.MAX_VALUE - 1,
      Long.MAX_VALUE,
      Long.MIN_VALUE,
      Long.MIN_VALUE + 1,
      MAX_UNSIGNED_LONG - 1,
      MAX_UNSIGNED_LONG};
  private static final SemanticVersion MOCK_MINIMUM = createMock(0, 0, 0, List.of("0"));
  private static final SemanticVersion MOCK_MAXIMUM = createMock(MAX_UNSIGNED_LONG, MAX_UNSIGNED_LONG, MAX_UNSIGNED_LONG, null);

  @Test
  public void testEqualsAndCompareTo() {
    TestUtil.verifySortOrder(
        true, Arrays.stream(TEST_VERSION_STRINGS_FOR_SORTING).map(SemanticVersion::valueOf).toArray(SemanticVersion[]::new));
  }
  @Test
  public void testEqualsAndHashCodeConsistent() {
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
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
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
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
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
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
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
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
    for (SemanticVersion baseVersion : TEST_VERSIONS_FOR_SORTING) {
      SemanticVersion baseVersionClone = baseVersion.clone();
      assertEquals(baseVersion, baseVersionClone);
      assertEquals(0, TOTAL_ORDERING.compare(baseVersion, baseVersionClone));
      SemanticVersion withMetadata = valueOf(baseVersion + randomBuildMetadata());
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
    for (SemanticVersion baseVersion : TEST_VERSIONS_FOR_SORTING) {
      String baseVersionString = baseVersion.toString();
      SemanticVersion versionWithBuildMetadata1 = valueOf(baseVersionString + randomBuildMetadata());
      SemanticVersion versionWithBuildMetadata2 = valueOf(baseVersionString + randomBuildMetadata());
      assertNotEquals(baseVersion, versionWithBuildMetadata1, "equals shouldn't ignore build metadata");
      assertEquals(0, BUILD_METADATA_AGNOSTIC_COMPARATOR.compare(baseVersion, versionWithBuildMetadata1),
          "BUILD_METADATA_AGNOSTIC_COMPARATOR should treat these as equal");
      assertEquals(0,
              BUILD_METADATA_AGNOSTIC_COMPARATOR.compare(versionWithBuildMetadata1, versionWithBuildMetadata2),
              "BUILD_METADATA_AGNOSTIC_COMPARATOR should treat these as equal");
      assertNotEquals(0, baseVersion.compareTo(versionWithBuildMetadata1),
          "compareTo shouldn't ignore build metadata");
      assertNotEquals(0, versionWithBuildMetadata1.compareTo(versionWithBuildMetadata2),
              "compareTo shouldn't ignore build metadata");
      assertTrue(TOTAL_ORDERING.compare(baseVersion, versionWithBuildMetadata1) < 0,
          "compareTo should put version without build metadata first");
    }
  }

  @Test
  public void testEmptyMetadata() {
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
      assertThrows(IllegalArgumentException.class, () -> valueOf(versionString + "+"));
      assertEquals(valueOf(versionString), valueOf(versionString + "+", true));
    }
  }

  @ParameterizedTest
  @ArgumentsSource(VersionPairProvider.class)
  public void testNextPrerelease(int i, int j) {
    SemanticVersion currentVersion = TEST_VERSIONS_FOR_SORTING[i];
    if (j <= i) {
      SemanticVersion previousOrSameVersion = TEST_VERSIONS_FOR_SORTING[j];
      assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(currentVersion),
          "checkedVersionBetween " + currentVersion + " and " + previousOrSameVersion + " should have thrown exception");
    } else {
      SemanticVersion nextRelease = TEST_VERSIONS_FOR_SORTING[j];
      if (currentVersion.isPrerelease()) {
        final SemanticVersion nextPrerelease = debuggableBetween(currentVersion, null);
        assertTrue(currentVersion.compareTo(nextPrerelease) < 0);
        assertTrue(nextPrerelease.compareTo(currentVersion.releaseVersion()) < 0);
      } else {
        assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(null));
      }
      verifyPrereleaseSortOrder(currentVersion, nextRelease);
    }
  }

  private Optional<SemanticVersion> checkedVersionBetween(SemanticVersion currentVersion, SemanticVersion nextVersion) {
    if (currentVersion.compareTo(nextVersion) >= 0) {
      assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(nextVersion));
      return Optional.empty();
    }
    String nextPossibleVersion =
        (currentVersion.releaseVersion().equals(nextVersion.releaseVersion()))
            ? (currentVersion + (currentVersion.isPrerelease() ? ".0" : "-0"))
            : (nextVersion.releaseVersion() + "-0");
    if (nextVersion.toString().equals(nextPossibleVersion)) {
      assertThrows(IllegalArgumentException.class, () -> currentVersion.nextPrereleaseBefore(nextVersion));
      return Optional.empty();
    }
    SemanticVersion between = debuggableBetween(currentVersion, nextVersion);
    assertIsPrerelease(between);
    assertEquals(between.releaseVersion(), nextVersion.releaseVersion());
    assertTrue(currentVersion.compareTo(between) < 0, "Expected " + currentVersion + " < " + between);
    assertTrue(between.compareTo(nextVersion) < 0, "Expected " + between + " < " + nextVersion);
    return Optional.of(between);
  }

  private SemanticVersion debuggableBetween(SemanticVersion currentVersion, SemanticVersion nextVersion) {
    try {
      return currentVersion.nextPrereleaseBefore(nextVersion);
    } catch (IllegalArgumentException e) {
      // Set breakpoint here
      throw new AssertionError(e);
    }
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

  @ParameterizedTest
  @ArgumentsSource(ReleaseVersionProvider.class)
  public void testBasicAccessors(long major, long minor, long patch) {
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
    assertTrue(releaseVersion.compareTo(releaseVersionPlusMetadata) < 0,
            "Version without build metadata should come first");
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

  @Test
  public void testNextMajorMinorPatch() {
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
      SemanticVersion version = valueOf(versionString);
      try {
        SemanticVersion nextPatch = version.nextPatchRelease();
        assertNull(nextPatch.buildMetadata());
        assertNotPrerelease(nextPatch);
        assertTrue(version.compareTo(nextPatch) < 0);
        assertTrue(version.compareTo(debuggableBetween(version, nextPatch)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.patchVersion(), MAX_UNSIGNED_LONG);
      }
      try {
        SemanticVersion nextMinor = version.nextMinorRelease();
        assertNull(nextMinor.buildMetadata());
        assertNotPrerelease(nextMinor);
        assertTrue(version.compareTo(nextMinor) < 0);
        assertTrue(version.compareTo(debuggableBetween(version, nextMinor)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.minorVersion(), MAX_UNSIGNED_LONG);
      }
      try {
        SemanticVersion nextMajor = version.nextMajorRelease();
        assertNull(nextMajor.buildMetadata());
        assertNotPrerelease(nextMajor);
        assertTrue(version.compareTo(nextMajor) < 0);
        assertTrue(version.compareTo(debuggableBetween(version, nextMajor)) < 0);
      } catch (IllegalArgumentException e) {
        assertEquals(version.majorVersion(), MAX_UNSIGNED_LONG);
      }
    }
  }

  @Test
  public void testPrereleaseWithIdentifiers() {
    for (String versionString : TEST_VERSION_STRINGS_FOR_SORTING) {
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

  private static SemanticVersion createMock(long major, long minor, long patch,
      List<String> prerelease) {
    SemanticVersion mock = Mockito.mock(SemanticVersion.class);
    when (mock.majorVersion()).thenReturn(major);
    when (mock.minorVersion()).thenReturn(minor);
    when (mock.patchVersion()).thenReturn(patch);
    when (mock.isPrerelease()).thenReturn(prerelease != null);
    when (mock.prereleaseVersion()).thenReturn(prerelease);
    when (mock.compareTo(any(SemanticVersion.class))).thenCallRealMethod();
    when (mock.toString()).thenReturn(toUnsignedString(major) + '.' + toUnsignedString(minor) + '.' + toUnsignedString(patch) +
        (prerelease == null ? "" : "-" + String.join(".", prerelease)));
    when (mock.clone()).thenAnswer(invocation -> createMock(major, minor, patch, prerelease));
    return mock;
  }

  @ParameterizedTest
  @ArgumentsSource(ReleaseVersionProvider.class)
  public void testNonStandardImplementations(long major, long minor, long patch) {
          SemanticVersion releaseVersion = SemanticVersion.valueOf(major, minor, patch, null, null);
          boolean isMax = releaseVersion.equals(MOCK_MAXIMUM);
          boolean isMin = releaseVersion.equals(MOCK_MINIMUM);
          assertFalse(isMax && isMin);
          TestUtil.verifySortOrder(!isMax && !isMin, MOCK_MINIMUM, releaseVersion, MOCK_MAXIMUM);

          SemanticVersion releaseVersionMockCopy = createMock(major, minor, patch, null);
          assertEquals(0, releaseVersion.compareTo(releaseVersionMockCopy));
          TestUtil.verifySortOrder(!isMax && !isMin, MOCK_MINIMUM, releaseVersionMockCopy, MOCK_MAXIMUM);
          SemanticVersion preReleaseVersion1 = createMock(major, minor, patch, List.of("1"));
          SemanticVersion preReleaseVersion2 = createMock(major, minor, patch, List.of("1","0"));
          SemanticVersion preReleaseVersion3 = createMock(major, minor, patch, List.of("alpha"));
          TestUtil.verifySortOrder(true, preReleaseVersion1, preReleaseVersion2, preReleaseVersion3, releaseVersionMockCopy);
          assertThrows(IllegalArgumentException.class, () -> releaseVersion.nextPrereleaseBefore(MOCK_MINIMUM));
          if (releaseVersion.compareTo(MOCK_MAXIMUM) == 0) {
            assertThrows(IllegalArgumentException.class, () -> releaseVersion.nextPrereleaseBefore(MOCK_MAXIMUM));
          } else {
            SemanticVersion preReleaseAfter = debuggableBetween(releaseVersion, MOCK_MAXIMUM);
            assertTrue(preReleaseAfter.isPrerelease());
            TestUtil.verifySortOrder(true, releaseVersion, preReleaseAfter, MOCK_MAXIMUM);
            TestUtil.verifySortOrder(true, releaseVersionMockCopy, preReleaseAfter, MOCK_MAXIMUM);
          }
  }

  @Test
  public void testOverflows() {
    SemanticVersion maxVersion = valueOf(MAX_UNSIGNED_LONG, MAX_UNSIGNED_LONG, MAX_UNSIGNED_LONG, null, null);
    assertThrows(IllegalArgumentException.class, maxVersion::nextMajorRelease);
    assertThrows(IllegalArgumentException.class, maxVersion::nextMinorRelease);
    assertThrows(IllegalArgumentException.class, maxVersion::nextPatchRelease);
    SemanticVersion maxMinor = valueOf(0, MAX_UNSIGNED_LONG, MAX_UNSIGNED_LONG, null, null);
    maxMinor.nextMajorRelease();
    assertThrows(IllegalArgumentException.class, maxMinor::nextMinorRelease);
    assertThrows(IllegalArgumentException.class, maxMinor::nextPatchRelease);
    SemanticVersion maxPatch = valueOf(0, 0, MAX_UNSIGNED_LONG, null, null);
    maxPatch.nextMajorRelease();
    maxPatch.nextMinorRelease();
    assertThrows(IllegalArgumentException.class, maxPatch::nextPatchRelease);
  }

  private static class VersionPairProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      ArrayList<Arguments> args = new ArrayList<>(TEST_VERSIONS_FOR_SORTING.length * TEST_VERSIONS_FOR_SORTING.length);
      for (int i = 0; i < TEST_VERSIONS_FOR_SORTING.length; i++) {
        for (int j = 0; j < TEST_VERSIONS_FOR_SORTING.length; j++) {
          args.add(Arguments.of(i, j));
        }
      }
      return args.parallelStream();
    }
  }

  private static class ReleaseVersionProvider implements ArgumentsProvider {
    @Override
    public Stream<? extends Arguments> provideArguments(ExtensionContext extensionContext) {
      ArrayList<Arguments> args = new ArrayList<>(
          TEST_VERSIONS.length * TEST_VERSIONS.length * TEST_VERSIONS.length);
      for (long major : TEST_VERSIONS) {
        for (long minor : TEST_VERSIONS) {
          for (long patch : TEST_VERSIONS) {
            args.add(Arguments.of(major, minor, patch));
          }
        }
      }
      return args.parallelStream();
    }
  }
}