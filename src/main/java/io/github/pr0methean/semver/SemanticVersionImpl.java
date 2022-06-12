package io.github.pr0methean.semver;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.checkerframework.checker.signedness.qual.Unsigned;

record SemanticVersionImpl(
    @Unsigned long majorVersion,
    @Unsigned long minorVersion,
    @Unsigned long patchVersion,
    @Nullable PrereleaseIdentifier[] prereleaseVersionArray,
    @Nullable String buildMetadata)
    implements SemanticVersion {
  public static final PrereleaseIdentifier FIRST_PRERELEASE = new PrereleaseIdentifier(true, 1, "");
  private static final PrereleaseIdentifier[] DEFAULT_FIRST_PRERELEASE_ARRAY
      = {FIRST_PRERELEASE};
  private static final PrereleaseIdentifier[] MIN_VALUE_PRERELEASE_ARRAY = {PrereleaseIdentifier.MIN_VALUE};

  /**
   * Equivalent to {@code comparing(keyExtractor, UnsignedLongs::compare)} without the autoboxing.
   */
  static <T> Comparator<T> comparingUnsignedLong(ToLongFunction<? super T> keyExtractor) {
    return (x, y) -> Long.compareUnsigned(keyExtractor.applyAsLong(x), keyExtractor.applyAsLong(y));
  }

  @Nullable
  public static PrereleaseIdentifier[] parsePrereleaseVersion(String versionString, boolean lenient) {
    Stream<String> prereleaseIdStrings = Arrays.stream(versionString.split("\\."));
    if (lenient) {
      prereleaseIdStrings = prereleaseIdStrings.filter(s -> !s.isEmpty());
    }
    PrereleaseIdentifier[] result = prereleaseIdStrings
        .map(PrereleaseIdentifier::valueOf)
        .toArray(PrereleaseIdentifier[]::new);
    return (result.length == 0) ? null : result;
  }

  @SuppressWarnings("ConstantConditions")
  private SemanticVersion withLastPrereleaseId(int newLength, PrereleaseIdentifier finalPrereleaseId) {
    PrereleaseIdentifier[] prereleaseIdentifiers = Arrays.copyOf(prereleaseVersionArray,
        newLength);
    prereleaseIdentifiers[newLength - 1] = finalPrereleaseId;
    return prereleaseWithIdentifiers(prereleaseIdentifiers);
  }

  @Override
  public boolean isPrerelease() {
    return prereleaseVersionArray != null;
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder(Long.toUnsignedString(majorVersion)).append('.')
        .append(Long.toUnsignedString(minorVersion)).append('.')
        .append(Long.toUnsignedString(patchVersion));
    if (prereleaseVersionArray != null) {
      out.append('-');
      boolean first = true;
      for (PrereleaseIdentifier chunk : prereleaseVersionArray) {
        if (first) {
          first = false;
        } else {
          out.append('.');
        }
        out.append(chunk);
      }
    }
    if (buildMetadata != null) {
      out.append('+').append(buildMetadata);
    }
    return out.toString();
  }

  @Override
  public SemanticVersion clone() {
    try {
      return (SemanticVersion) super.clone(); // No mutable state, so shallow copy is fine
    } catch (CloneNotSupportedException e) {
      throw new AssertionError(e);
    }
  }

  @Nullable
  @Override
  public List<String> prereleaseVersion() {
    return prereleaseVersionArray == null ? null :
        Arrays.stream(prereleaseVersionArray.clone()).map(PrereleaseIdentifier::toString).collect(Collectors.toList());
  }

  @Override
  public SemanticVersion withBuildMetadata(@Nullable String buildMetadata, boolean lenient) {
    if (buildMetadata != null) {
      if (!lenient) {
        if (buildMetadata.isEmpty()) {
          throw new IllegalArgumentException("buildMetadata must be null or non-empty");
        }
        buildMetadata.codePoints().forEach(SemanticVersion::checkValidIdentifierCodePoint);
      } else if (buildMetadata.isEmpty()) {
        buildMetadata = null;
      }
    }
    return new SemanticVersionImpl(majorVersion, minorVersion,
        patchVersion, prereleaseVersionArray, buildMetadata);
  }

  @Override
  public SemanticVersion prereleaseWithIdentifiers(@Nullable List<String> identifiers) {
    if (identifiers == null || identifiers.isEmpty()) {
      return releaseVersion();
    }
    return prereleaseWithIdentifiers(
        identifiers.stream().map(PrereleaseIdentifier::valueOf).toArray(PrereleaseIdentifier[]::new));
  }

  @Override
  public SemanticVersion releaseVersion() {
    if (prereleaseVersionArray == null) {
      return this;
    }
    return prereleaseWithIdentifiers((PrereleaseIdentifier[]) null);
  }

  @Override
  public SemanticVersion nextMajorRelease() {
    if (isPrerelease() && minorVersion == 0 && patchVersion == 0) {
      return releaseVersion();
    }
    if (majorVersion == SemanticVersion.UNSIGNED_MAX_VALUE) {
      throw new IllegalArgumentException("Major version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersion + 1, 0, 0,
        null, null);
  }

  @Override
  public SemanticVersion nextMinorRelease() {
    if (isPrerelease() && patchVersion == 0) {
      return releaseVersion();
    }
    if (minorVersion == SemanticVersion.UNSIGNED_MAX_VALUE) {
      throw new IllegalArgumentException("Minor version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersion, minorVersion + 1, 0,
        null, null);
  }

  @Override
  public SemanticVersion nextPatchRelease() {
    if (isPrerelease()) {
      return releaseVersion();
    }
    if (patchVersion == SemanticVersion.UNSIGNED_MAX_VALUE) {
      throw new IllegalArgumentException("Patch version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersion, minorVersion,
        patchVersion + 1, null, null);
  }

  private SemanticVersionImpl prereleaseWithIdentifiers(@Nullable PrereleaseIdentifier[] identifiers) {
    return new SemanticVersionImpl(
        majorVersion,
        minorVersion,
        patchVersion,
        identifiers,
        null
    );
  }

  @SuppressWarnings("ConstantConditions")
  public SemanticVersion nextPrereleaseBefore(@Nullable SemanticVersion nextRelease) {
    if (nextRelease == null) {
      if (!isPrerelease()) {
        throw new IllegalArgumentException("Need to know the next release to find the next prerelease, since "
            + this + " isn't a prerelease");
      }
      nextRelease = releaseVersion();
    } else if (BUILD_METADATA_AGNOSTIC_COMPARATOR.compare(this, nextRelease) >= 0) {
      throw new IllegalArgumentException(this + " is equivalent to or ahead of " + nextRelease);
    }
    SemanticVersionImpl nextReleaseImpl = (SemanticVersionImpl) ((nextRelease instanceof SemanticVersionImpl)
        ? nextRelease
        : SemanticVersion.valueOf(nextRelease.toString()));
    if (!releaseVersion().equals(nextReleaseImpl.releaseVersion())) {
      // Branch A: we're not a prerelease of nextReleaseImpl.releaseVersion()
      // Output an earlier prerelease of nextReleaseImpl.releaseVersion()

      // Strategy A1: return the default first prerelease of a non-prerelease nextReleaseImpl
      if (!nextReleaseImpl.isPrerelease()) {
        return nextReleaseImpl.prereleaseWithIdentifiers(DEFAULT_FIRST_PRERELEASE_ARRAY);
      }

      // Strategy A2: fail fast if nextReleaseImpl is already the lowest possible prerelease identifier for its
      // release version
      if (Arrays.equals(nextReleaseImpl.prereleaseVersionArray, MIN_VALUE_PRERELEASE_ARRAY)) {
        throw new IllegalArgumentException(nextReleaseImpl + " is already the first possible prerelease of "
            + nextReleaseImpl.releaseVersion());
      }

      // Strategy A3: look for a numeric part to change to 1, e.g. 1.0.1-alpha.5a.bravo -> 1.0.1-alpha.1a
      for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
        PrereleaseIdentifier theirIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
        if (theirIdentifier.hasNumericPart() && theirIdentifier.numericPart() != 0
            && theirIdentifier.numericPart() != 1) {
          return nextReleaseImpl.withLastPrereleaseId(i + 1,
              FIRST_PRERELEASE);
        }
      }

      // Strategy A4: look for a numeric part to change to 0, e.g. 1.0.1-1.1 -> 1.0.1-1.0
      for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
        PrereleaseIdentifier theirIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
        if (theirIdentifier.hasNumericPart() && theirIdentifier.numericPart() != 0) {
          return nextReleaseImpl.withLastPrereleaseId(i + 1, PrereleaseIdentifier.MIN_VALUE);
        }
      }

      // Strategy A5: look for a suffix that comes after a numeric part to truncate, e.g. 1.0.1-1a -> 1.0.1-1
      for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
        PrereleaseIdentifier theirIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
        if (theirIdentifier.hasNumericPart() && !(theirIdentifier.suffix().isEmpty())) {
          return nextReleaseImpl.withLastPrereleaseId(i + 1,
              new PrereleaseIdentifier(true, theirIdentifier.numericPart(), ""));
        }
      }

      // Strategy A6: replace a prerelease identifier with "0"
      for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
        PrereleaseIdentifier theirIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
        if (theirIdentifier.compareTo(PrereleaseIdentifier.MIN_VALUE) > 0) {
          return nextReleaseImpl.withLastPrereleaseId(i + 1,
              PrereleaseIdentifier.MIN_VALUE);
        }
      }

      // Strategy A7: truncate a prerelease identifier (infallible)
      return nextReleaseImpl.prereleaseWithIdentifiers(Arrays.copyOf(nextReleaseImpl.prereleaseVersionArray,
          nextReleaseImpl.prereleaseVersionArray.length - 1));
    }
    // if we've gotten this far, we're a prerelease of nextReleaseImpl.releaseVersion()

    if (!nextReleaseImpl.isPrerelease()) {
      // Branch B: We're a prerelease of nextReleaseImpl, which is a release version

      // Strategy B1: Find a numerical identifier to increment
      for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
        PrereleaseIdentifier ourIdentifier = prereleaseVersionArray[i];
        if (ourIdentifier.hasNumericPart() && ourIdentifier.numericPart() != UNSIGNED_MAX_VALUE) {
          PrereleaseIdentifier incremented = new PrereleaseIdentifier(true,
              ourIdentifier.numericPart() + 1, ourIdentifier.suffix());
          PrereleaseIdentifier[] ourIdentifiersWithIncrement = Arrays.copyOf(prereleaseVersionArray, i + 1);
          ourIdentifiersWithIncrement[i] = incremented;
          return nextReleaseImpl.prereleaseWithIdentifiers(ourIdentifiersWithIncrement);
        }
      }

      // Strategy B2: Concatenate ".1" (infallible)
      return withLastPrereleaseId(prereleaseVersionArray.length + 1, FIRST_PRERELEASE);
    }

    // Branches C & up: we're both prereleases of the same version
    final int shorterLength = Math.min(prereleaseVersionArray.length, nextReleaseImpl.prereleaseVersionArray.length);
    final int firstDifference = Arrays.mismatch(prereleaseVersionArray, nextReleaseImpl.prereleaseVersionArray);

    PrereleaseIdentifier theirIdentifier = nextReleaseImpl.prereleaseVersionArray[firstDifference];

    if (prereleaseVersionArray.length <= firstDifference) {
      // Branch C: we're a prefix of nextReleaseImpl

      // Strategy C1: If we're a prefix of their version, find a numeric part greater than 1 and replace it with 1
      // e.g. 1.0.1-0alpha.1 as between 1.0.1-0alpha and 1.0.1-0alpha.27
      if (theirIdentifier.hasNumericPart() && theirIdentifier.numericPart() != 0
          && theirIdentifier.numericPart() != 1) {
        return nextReleaseImpl.withLastPrereleaseId(firstDifference + 1,
            new PrereleaseIdentifier(true, 1, theirIdentifier.suffix()));
      }
      // Strategy C2: If we're a prefix of their version with more than one identifier that they have and we don't, and
      // they don't have any numeric parts after us, then try using the next-shortest prefix
      if (nextReleaseImpl.prereleaseVersionArray.length >= firstDifference + 2) {
        return nextReleaseImpl.prereleaseWithIdentifiers(
            Arrays.copyOf(nextReleaseImpl.prereleaseVersionArray, prereleaseVersionArray.length + 1));
      }
      // Strategy C3: fail if nextReleaseImpl is this + ".0"
      if (nextReleaseImpl.prereleaseVersionArray[firstDifference].equals(PrereleaseIdentifier.MIN_VALUE)) {
        throw new IllegalArgumentException(this + " is already the last possible version before " + nextReleaseImpl);
      }
      // Strategy C4: return this + ".0" (infallible)
      return withLastPrereleaseId(prereleaseVersionArray.length + 1, PrereleaseIdentifier.MIN_VALUE);
    }

    // Branches D & up: we're both prereleases of the same version,
    // but we differ within the first shorterLength prerelease identifiers

    PrereleaseIdentifier ourIdentifier = prereleaseVersionArray[firstDifference];

    if (theirIdentifier.hasNumericPart() && ourIdentifier.hasNumericPart()) {
      // Branch D: On the first identifier where we differ, we both have a numeric part

      long difference = theirIdentifier.numericPart() - ourIdentifier.numericPart();
      if (difference >= 2) {
        // Strategy D1: Find an identifier on which we both have a numeric part and differ by at least 2
        // e.g. 1.0.1-1alpha as between 1.0.1-0alpha and 1.0.1-2*
        PrereleaseIdentifier[] newPrereleaseVersion = Arrays.copyOf(prereleaseVersionArray, firstDifference + 1);
        newPrereleaseVersion[firstDifference] = new PrereleaseIdentifier(true,
            ourIdentifier.numericPart() + 1, ourIdentifier.suffix());
        return prereleaseWithIdentifiers(newPrereleaseVersion);
      } else if (difference == 1) {
        // Strategy D2: Find an identifier with a numeric part on which we differ by 1 and their suffix is greater than
        // ours
        // e.g. 1.0.1-1alpha as between 1.0.1-0alpha and 1.0.1-1beta
        PrereleaseIdentifier incremented = new PrereleaseIdentifier(true, ourIdentifier.numericPart() + 1,
            ourIdentifier.suffix());
        if (incremented.compareTo(theirIdentifier) < 0) {
          return withLastPrereleaseId(firstDifference + 1, incremented);
        }

        // Strategy D3: Find an identifier with a numeric part on which we differ by 1 and they have a suffix we can
        // chop off and replace with ".1"
        // e.g. 1.0.1-1 as between 1.0.1-0 and 1.0.1-1a
        if (!theirIdentifier.suffix().isEmpty()) {
          PrereleaseIdentifier incrementedWithoutSuffix
              = new PrereleaseIdentifier(true, theirIdentifier.numericPart(), "");
          PrereleaseIdentifier[] newPrereleaseVersion = Arrays.copyOf(prereleaseVersionArray, firstDifference + 2);
          newPrereleaseVersion[firstDifference] = incrementedWithoutSuffix;
          newPrereleaseVersion[firstDifference + 1] = FIRST_PRERELEASE;
          return prereleaseWithIdentifiers(newPrereleaseVersion);
        }

        // Strategy D4: Append ".1" to this (infallible)
        // e.g. 1.0.1-alpha0.1 as between 1.0.1-alpha0 and 1.0.1-alpha1
        return withLastPrereleaseId(prereleaseVersionArray.length + 1, FIRST_PRERELEASE);
      }
    }

    // Branch E: our first difference is in a suffix
    // Strategy E1: Find a number we can change to 1

    for (int i = shorterLength - 1; i > firstDifference; i--) {
      PrereleaseIdentifier theirNumIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
      if (theirNumIdentifier.hasNumericPart() && theirNumIdentifier.numericPart() != 0
          && theirNumIdentifier.numericPart() != 1) {
        return nextReleaseImpl.withLastPrereleaseId(i + 1, new PrereleaseIdentifier(true,
            1, theirNumIdentifier.suffix()));
      }
    }

    // Strategy E2: Find a number we can change to 0
    for (int i = shorterLength - 1; i > firstDifference; i--) {
      PrereleaseIdentifier theirNumIdentifier = nextReleaseImpl.prereleaseVersionArray[i];
      if (theirNumIdentifier.hasNumericPart() && theirNumIdentifier.numericPart() != 0) {
        return nextReleaseImpl.withLastPrereleaseId(i + 1, new PrereleaseIdentifier(true,
            0, theirNumIdentifier.suffix()));
      }
    }

    // Strategy E3: Find a number on our side we can increment
    for (int i = prereleaseVersionArray.length - 1; i > firstDifference; i--) {
      PrereleaseIdentifier ourNumIdentifier = prereleaseVersionArray[i];
      if (ourNumIdentifier.hasNumericPart() && ourNumIdentifier.numericPart() != UNSIGNED_MAX_VALUE) {
        return withLastPrereleaseId(i + 1, new PrereleaseIdentifier(true,
            ourNumIdentifier.numericPart() + 1, ourNumIdentifier.suffix()));
      }
    }

    // Strategy E4: Append ".1" to this (infallible since we already differ in our existing parts)
    return withLastPrereleaseId(prereleaseVersionArray.length + 1, FIRST_PRERELEASE);
  }

  @Override
  public boolean equals (Object o){
    if (this == o) {
      return true;
    }
    if (!(o instanceof SemanticVersion that)) {
      return false;
    }
    if (o instanceof SemanticVersionImpl thatImpl) {
      return majorVersion == thatImpl.majorVersion &&
          minorVersion == thatImpl.minorVersion &&
          patchVersion == thatImpl.patchVersion &&
          Arrays.equals(prereleaseVersionArray, thatImpl.prereleaseVersionArray) &&
          Objects.equals(buildMetadata, thatImpl.buildMetadata);
    }
    return majorVersion == that.majorVersion() &&
        minorVersion == that.minorVersion() &&
        patchVersion == that.patchVersion() &&
        Objects.equals(prereleaseVersion(), that.prereleaseVersion()) &&
        Objects.equals(buildMetadata, that.buildMetadata());
  }

  @Override
  public int hashCode () {
    int result = Objects.hash(majorVersion, minorVersion, patchVersion,
        buildMetadata);
    result = 31 * result + Arrays.hashCode(prereleaseVersionArray);
    return result;
  }
}