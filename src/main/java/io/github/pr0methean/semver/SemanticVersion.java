package io.github.pr0methean.semver;

import org.checkerframework.checker.signedness.qual.Unsigned;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import static io.github.pr0methean.semver.SemanticVersionImpl.comparingUnsignedLong;
import static io.github.pr0methean.semver.SemanticVersionImpl.parsePrereleaseVersion;
import static java.util.Comparator.naturalOrder;
import static java.util.Comparator.nullsFirst;

public interface SemanticVersion extends Comparable<SemanticVersion>, Cloneable, Serializable {

  long UNSIGNED_MAX_VALUE = -1;
  SemanticVersion MAX_VALUE = new SemanticVersionImpl(UNSIGNED_MAX_VALUE, UNSIGNED_MAX_VALUE,
      UNSIGNED_MAX_VALUE, null, null);
  SemanticVersion MIN_VALUE = new SemanticVersionImpl(0, 0, 0,
      new PrereleaseIdentifier[]{PrereleaseIdentifier.MIN_VALUE}, null);

  /**
   * Implementation of the standard natural ordering on SemanticVersion instances. The semantic version standard
   * requires that comparison of semantic versions must ignore build metadata. Thus, the natural ordering is an ordered
   * partition rather than a total ordering ({@code a.equals(b)} implies {@code a.compareTo(b) == 0} but not
   * vice-versa). See {@link #TOTAL_ORDERING} for an ordering that distinguishes versions with different build metadata.
   */
  Comparator<SemanticVersion> BUILD_METADATA_AGNOSTIC_COMPARATOR
          = comparingUnsignedLong(SemanticVersion::majorVersion)
      .thenComparing(comparingUnsignedLong(SemanticVersion::minorVersion))
      .thenComparing(comparingUnsignedLong(SemanticVersion::patchVersion))
      .thenComparing(SemanticVersion::comparePrereleaseVersions);

  /**
   * A nonstandard ordering that sorts first by the standard ordering, then by build metadata. If two SemanticVersion
   * instances are unequal, then one of them is greater according to this ordering.
   */
  Comparator<SemanticVersion> TOTAL_ORDERING
          = BUILD_METADATA_AGNOSTIC_COMPARATOR.thenComparing(SemanticVersion::buildMetadata, nullsFirst(naturalOrder()));

  /**
   * Converts the given String to a SemanticVersion.
   * @param input the string to convert
   * @return the equivalent SemanticVersion instance
   * @throws NumberFormatException if the major, minor and patch versions are not all integers between 0 and
   *     {@link Long#MAX_VALUE} inclusive
   * @throws IllegalArgumentException if {@code input} isn't a valid SemanticVersion according to the standard, for any
   *     other reason
   */
  static SemanticVersion valueOf(String input) {
    return valueOf(input, false);
  }

  /**
   * Converts the given String to a SemanticVersion, with the option to allow versions that deviate from the standard in
   * the following ways: <ul>
   *   <li>Omitting the minor or patch version sets it to zero.</li>
   *   <li>Starting the version with a dot effectively prepends zero to it as the major version.</li>
   *   <li>Additional dot-separated values after the major, minor and patch versions, but before any prerelease version
   *       or build metadata, are ignored.</li>
   *   <li>An empty prerelease version or build-metadata string is ignored.</li>
   *   <li>The build metadata can include any characters, not just {@code [0-9A-Za-z-]}.</li>
   * </ul>
   * @param input the string to convert
   * @param lenient whether to allow the nonstandard variations listed above
   * @return the equivalent SemanticVersion instance
   */
  static SemanticVersion valueOf(String input, boolean lenient) {
    String[] comparablePartAndBuildMetadata = input.split("\\+", 2);
    @Nullable String buildMetadata = (comparablePartAndBuildMetadata.length >= 2)
        ? comparablePartAndBuildMetadata[1] : null;
    String[] mainAndPrerelease = comparablePartAndBuildMetadata[0].split("-", 2);
    PrereleaseIdentifier[] prereleaseChunks = null;
    if (mainAndPrerelease.length >= 2) {
      prereleaseChunks = parsePrereleaseVersion(mainAndPrerelease[1], lenient);
    }
    String[] mainChunkStrings = mainAndPrerelease[0].split("\\.");
    if (lenient) {
      switch (mainChunkStrings.length) {
        case 0:
          throw new IllegalArgumentException("Must start with major version, or decimal point then minor version");
        case 1:
          if (mainChunkStrings[0].isEmpty()) {
            throw new IllegalArgumentException("Must start with major version, or decimal point then minor version");
          }
          break;
        default:
          if (mainChunkStrings[0].isEmpty()) {
            mainChunkStrings[0] = "0";
          }
      }
    } else {
      if (mainChunkStrings.length != 3) {
        throw new IllegalArgumentException(input + " is an invalid semantic version: must be major.minor.patch");
      }
    }
    long[] mainChunks = Arrays.stream(mainChunkStrings)
        .mapToLong(Long::parseUnsignedLong)
        .toArray();
    long unsignedMajor = getOrZero(mainChunks, 0);
    long unsignedMinor = getOrZero(mainChunks, 1);
    long unsignedPatch = getOrZero(mainChunks, 2);
    if (buildMetadata != null) {
      if (buildMetadata.isEmpty()) {
        if (lenient) {
          buildMetadata = null;
        } else {
          throw new IllegalArgumentException("Build metadata following + must be non-empty");
        }
      } else if (!lenient) {
        buildMetadata.codePoints().forEach(SemanticVersion::checkValidIdentifierCodePoint);
      }
    }
    return new SemanticVersionImpl(unsignedMajor, unsignedMinor, unsignedPatch, prereleaseChunks, buildMetadata);
  }

  static SemanticVersion valueOf(@Unsigned long major, @Unsigned long minor, @Unsigned long patch,
      List<String> prereleaseIdentifiers, @Nullable String buildMetadata) {
    PrereleaseIdentifier[] prereleaseIdentifiersArray = null;
    if (prereleaseIdentifiers != null && !prereleaseIdentifiers.isEmpty()) {
      prereleaseIdentifiersArray =
          prereleaseIdentifiers.stream().map(PrereleaseIdentifier::valueOf).toArray(PrereleaseIdentifier[]::new);
    }
    return new SemanticVersionImpl(major, minor, patch, prereleaseIdentifiersArray,
        ((buildMetadata != null && buildMetadata.isEmpty()) ? null : buildMetadata));
  }

  private static long getOrZero(long[] array, int index) {
    return (array.length > index) ? array[index] : 0;
  }

  static void checkValidIdentifierCodePoint(int input) {
    if (input >= '0' && input <= '9'
        || input >= 'A' && input <= 'Z'
        || input >= 'a' && input <= 'z'
        || input == '-') {
      return;
    }
    throw new IllegalArgumentException("Invalid identifier character " + Character.toString(input));
  }

  /**
   * @return true if this version contains a prerelease identifier, e.g. 1.0.0-alpha1
   */
  boolean isPrerelease();

  @Override
  default int compareTo(@Nonnull SemanticVersion other) {
    // Comparable's Javadoc recommends that the natural ordering be a total ordering when possible.
    // This helps e.g. ensure O(log n) behavior on a treeified HashMap<SemanticVersion,?> bucket with adversarial input.
    return TOTAL_ORDERING.compare(this, other);
  }

  SemanticVersion clone();

  /**
   * @return the major version of this semantic version (e.g. returns 1 for version 1.2.0)
   */
  @Unsigned
  long majorVersion();

  /**
   * @return the minor version of this semantic version (e.g. returns 2 for version 1.2.0)
   */
  @Unsigned
  long minorVersion();

  /**
   * @return the patch version of this semantic version (e.g. returns 0 for version 1.2.0)
   */
  @Unsigned
  long patchVersion();

  @Nullable String buildMetadata();

  @Nullable
  List<String> prereleaseVersion();

  static int comparePrereleaseVersions(SemanticVersion v1, SemanticVersion v2) {
    if (!v1.isPrerelease()) {
      return v2.isPrerelease() ? 1 : 0; // Prereleases come first
    }
    if (!v2.isPrerelease()) {
      return -1; // Prereleases come first
    }
    if (v1 instanceof SemanticVersionImpl impl1 && v2 instanceof SemanticVersionImpl impl2) {
      return Arrays.compare(impl1.prereleaseVersionArray(), impl2.prereleaseVersionArray());
    }
    final Iterator<String> iterator1 = v1.prereleaseVersion().iterator();
    final Iterator<String> iterator2 = v2.prereleaseVersion().iterator();
    while (true) {
      if (iterator1.hasNext()) {
        if (iterator2.hasNext()) {
          PrereleaseIdentifier prerel1 = PrereleaseIdentifier.valueOf(iterator1.next());
          PrereleaseIdentifier prerel2 = PrereleaseIdentifier.valueOf(iterator2.next());
          int comparison = prerel1.compareTo(prerel2);
          if (comparison != 0) {
            return comparison;
          }
        } else {
          return 1; // Sort prerelease identifier lists lexicographically
        }
      } else if (iterator2.hasNext()) {
        return -1; // Sort prerelease identifier lists lexicographically
      } else {
        return 0;
      }
    }
  }

  /**
   * @param buildMetadata the build metadata string
   * @return a copy of this SemanticVersion with the given build metadata
   */
  default SemanticVersion withBuildMetadata(@Nullable String buildMetadata) {
    return withBuildMetadata(buildMetadata, false);
  }

  /**
   * @param buildMetadata the new build-metadata value, or null to remove it
   * @param lenient whether to allow invalid characters and treat an empty string as null
   * @return a copy of this SemanticVersion with the given build metadata
   */
  SemanticVersion withBuildMetadata(@Nullable String buildMetadata, boolean lenient);

  /**
   * Returns a prerelease version for the same release version as {@code this}.
   * @param identifiers the prerelease identifiers
   * @return a prerelease version of {@code this}
   */
  SemanticVersion prereleaseWithIdentifiers(List<String> identifiers);

  /**
   * @return the release version for which this version is a prerelease (with no build metadata), or this version if
   *     it's already not a prerelease
   */
  SemanticVersion releaseVersion();

  /**
   * If this is a prerelease for X.0.0, where X is the major version, returns that version; otherwise, returns
   * (X+1).0.0.
   * @return the version that would apply to a major release immediately following this version
   */
  SemanticVersion nextMajorRelease();

  /**
   * If this is a prerelease for X.Y.0, where X is the major version and Y is the minor version, returns that version;
   * otherwise, returns X.(Y+1).0.
   * @return the version that would apply to a minor release immediately following this version
   */
  SemanticVersion nextMinorRelease();

  /**
   * If this is a prerelease, returns {@link #releaseVersion()}; otherwise, returns a version with the patch number
   * incremented by 1 compared to this one.
   * @return the version that would apply to a patch release immediately following this version
   */
  SemanticVersion nextPatchRelease();

  /**
   * Returns a prerelease version coming after this one, and before the release whose major, minor and patch version are
   * specified by {@code nextRelease}.
   * @param nextRelease the next known release version, if any; otherwise, the current version must be a prerelease, and
   *     its corresponding release version is used
   * @return a prerelease version that sorts between {@code this} and {@code nextRelease}
   * @throws IllegalArgumentException if it's impossible to create a prerelease between {@code this} and
   *     {@code nextRelease} (can happen when {@code nextRelease} and {@code this} are prereleases of the same version
   *     and one is the other with ".0" appended
   */
  SemanticVersion nextPrereleaseBefore(@Nullable SemanticVersion nextRelease);
}
