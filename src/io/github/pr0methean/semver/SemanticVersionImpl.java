package io.github.pr0methean.semver;

import com.google.common.primitives.UnsignedLongs;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Represents a semantic version complying with https://semver.org/spec/v1.0.0.html.
 */
record SemanticVersionImpl(
        long majorVersionUnsignedUnboxed,
        long minorVersionUnsignedUnboxed,
        long patchVersionUnsignedUnboxed,
        @Nullable PrereleaseIdentifier[] prereleaseVersionArray,
        @Nullable String buildMetadata)
    implements SemanticVersion {
  public static final PrereleaseIdentifier FIRST_PRERELEASE = new PrereleaseIdentifier(true, 1, "");
  private static final PrereleaseIdentifier[] DEFAULT_FIRST_PRERELEASE_ARRAY
      = {FIRST_PRERELEASE};

  /**
   * Equivalent to {@code comparing(keyExtractor, UnsignedLongs::compare)} without the autoboxing.
   */
  @SuppressWarnings("UnstableApiUsage")
  static <T> Comparator<T> comparingUnsignedLong(ToLongFunction<? super T> keyExtractor) {
    return (x, y) -> UnsignedLongs.compare(keyExtractor.applyAsLong(x), keyExtractor.applyAsLong(y));
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

  @Override
  public boolean isPrerelease() {
    return prereleaseVersionArray != null;
  }

  @SuppressWarnings("UnstableApiUsage")
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder(UnsignedLongs.toString(majorVersionUnsignedUnboxed)).append('.')
        .append(UnsignedLongs.toString(minorVersionUnsignedUnboxed)).append('.')
        .append(UnsignedLongs.toString(patchVersionUnsignedUnboxed));
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
    return new SemanticVersionImpl(majorVersionUnsignedUnboxed, minorVersionUnsignedUnboxed,
        patchVersionUnsignedUnboxed, prereleaseVersionArray, buildMetadata);
  }

  @Override
  public SemanticVersion prereleaseWithIdentifiers(List<String> identifiers) {
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
    if (isPrerelease() && minorVersionUnsignedUnboxed == 0 && patchVersionUnsignedUnboxed == 0) {
      return releaseVersion();
    }
    if (majorVersionUnsignedUnboxed == SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
      throw new IllegalArgumentException("Major version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersionUnsignedUnboxed + 1, 0, 0,
        null, null);
  }

  @Override
  public SemanticVersion nextMinorRelease() {
    if (isPrerelease() && patchVersionUnsignedUnboxed == 0) {
      return releaseVersion();
    }
    if (minorVersionUnsignedUnboxed == SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
      throw new IllegalArgumentException("Minor version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersionUnsignedUnboxed, minorVersionUnsignedUnboxed + 1, 0,
        null, null);
  }

  @Override
  public SemanticVersion nextPatchRelease() {
    if (isPrerelease()) {
      return releaseVersion();
    }
    if (patchVersionUnsignedUnboxed == SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
      throw new IllegalArgumentException("Patch version would overflow a long treated as unsigned");
    }
    return new SemanticVersionImpl(majorVersionUnsignedUnboxed, minorVersionUnsignedUnboxed,
        patchVersionUnsignedUnboxed + 1, null, null);
  }

  private SemanticVersionImpl prereleaseWithIdentifiers(PrereleaseIdentifier[] identifiers) {
    return new SemanticVersionImpl(
        majorVersionUnsignedUnboxed,
        minorVersionUnsignedUnboxed,
        patchVersionUnsignedUnboxed,
        identifiers,
        null
    );
  }

  @Override
  public SemanticVersion nextPrereleaseBefore(@Nullable SemanticVersion nextRelease) {
    if (nextRelease == null) {
      if (!isPrerelease()) {
        throw new IllegalArgumentException("Need to know the next release to find the next prerelease, since "
            + this + " isn't a prerelease");
      }
      nextRelease = releaseVersion();
    }
    SemanticVersionImpl nextReleaseImpl = (SemanticVersionImpl) ((nextRelease instanceof SemanticVersionImpl)
        ? nextRelease
        : SemanticVersion.valueOf(nextRelease.toString()));
    if (isPrerelease()) {
      SemanticVersion thisWithLastNumberIncremented = null;
      boolean finished = false;
      // First pass: look for a non-maximal numeric part and increment it
      for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
        if (prereleaseVersionArray[i].hasNumericPart() &&
            prereleaseVersionArray[i].numericPartUnsignedUnboxed() != SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
          PrereleaseIdentifier[] prereleaseIdentifiers = Arrays.copyOf(prereleaseVersionArray,
              i + 1);
          prereleaseIdentifiers[i] = new PrereleaseIdentifier(true,
              prereleaseVersionArray[i].numericPartUnsignedUnboxed() +
                  1,
              prereleaseVersionArray[i].suffix());
          thisWithLastNumberIncremented = prereleaseWithIdentifiers(prereleaseIdentifiers);
          finished = true;
          break;
        }
      }
      if (!finished) {
        // Try an extension
        thisWithLastNumberIncremented =
            withLastPrereleaseId(this, prereleaseVersionArray.length + 1, FIRST_PRERELEASE);
      }
      if (compareTo(thisWithLastNumberIncremented) < 0 && thisWithLastNumberIncremented.compareTo(nextReleaseImpl) < 0) {
        return thisWithLastNumberIncremented;
      }
    }
    SemanticVersion nextWithLastNumberDecremented = null;
    if (!nextReleaseImpl.isPrerelease()) {
      nextWithLastNumberDecremented = nextReleaseImpl.prereleaseWithIdentifiers(DEFAULT_FIRST_PRERELEASE_ARRAY);
    } else {
      // Second pass: look for a positive numeric part and decrement it or remove the suffix
      boolean finished = false;
      for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
        if (nextReleaseImpl.prereleaseVersionArray[i].hasNumericPart() &&
            nextReleaseImpl.prereleaseVersionArray[i].numericPartUnsignedUnboxed() != 0) {
          nextWithLastNumberDecremented = withLastPrereleaseId(nextReleaseImpl, i + 1,
              new PrereleaseIdentifier(true,
                  nextReleaseImpl.prereleaseVersionArray[i].numericPartUnsignedUnboxed() +
                      (nextReleaseImpl.prereleaseVersionArray[i].suffix().isEmpty() ? -1 : 0),
                  ""));
          finished = true;
          break;
        }
      }
      if (!finished) {
        // Third pass: try replacing a non-numeric identifier with "1"
        for (int i = nextReleaseImpl.prereleaseVersionArray.length - 1; i >= 0; i--) {
          if (!nextReleaseImpl.prereleaseVersionArray[i].hasNumericPart()) {
            nextWithLastNumberDecremented = SemanticVersionImpl.withLastPrereleaseId(nextReleaseImpl, i + 1, FIRST_PRERELEASE);
            finished = true;
            break;
          }
        }
        if (!finished) {
          // Try a prefix
          PrereleaseIdentifier[] prereleaseIdentifiers = Arrays.copyOf(nextReleaseImpl.prereleaseVersionArray,
              nextReleaseImpl.prereleaseVersionArray.length - 1);

          nextWithLastNumberDecremented = nextReleaseImpl.prereleaseWithIdentifiers(prereleaseIdentifiers);
        }
      }
    }
    if (compareTo(nextWithLastNumberDecremented) < 0 && nextWithLastNumberDecremented.compareTo(nextReleaseImpl) < 0) {
      return nextWithLastNumberDecremented;
    }
    SemanticVersion thisExtended = null;
    if (!isPrerelease()) {
      thisExtended = nextPrereleaseBefore(nextPatchRelease());
    } else {
      // Fourth pass: try an extension of a prefix
      boolean finished1 = false;
      for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
        if (prereleaseVersionArray[i].hasNumericPart() &&
            prereleaseVersionArray[i].numericPartUnsignedUnboxed() != SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
          thisExtended = withLastPrereleaseId(this, i + 1, new PrereleaseIdentifier(
              true, prereleaseVersionArray[i].numericPartUnsignedUnboxed() + 1, ""));
          finished1 = true;
          break;
        }
      }
      if (!finished1) {
        // Fifth pass: try a non-numeric identifier followed by "1"
        for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
          if (!prereleaseVersionArray[i].hasNumericPart()) {
            thisExtended = withLastPrereleaseId(this, i + 2, FIRST_PRERELEASE);
            break;
          }
        }
      }
    }
    if (thisExtended != null && compareTo(thisExtended) < 0 && thisExtended.compareTo(nextReleaseImpl) < 0) {
      return thisExtended;
    }
    SemanticVersion lowerBoundExtendingWithZero = null;
    if (!isPrerelease()) {
      lowerBoundExtendingWithZero = nextPrereleaseBefore(nextPatchRelease());
    } else {
      // Sixth pass: look for a positive numeric part and extend it
      boolean finished3 = false;
      for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
        if (prereleaseVersionArray[i].hasNumericPart() &&
            prereleaseVersionArray[i].numericPartUnsignedUnboxed() != SemanticVersion.UNSIGNED_MAX_VALUE_UNBOXED) {
          lowerBoundExtendingWithZero =
              withLastPrereleaseId(this,
                  i + 2, PrereleaseIdentifier.MIN_VALUE);
          finished3 = true;
          break;
        }
      }
      if (!finished3) {
        // Seventh pass: try replacing/extending a non-numeric identifier with "0"
        for (int i = prereleaseVersionArray.length - 1; i >= 0; i--) {
          if (!prereleaseVersionArray[i].hasNumericPart()) {
            lowerBoundExtendingWithZero = withLastPrereleaseId(this, i + 2, PrereleaseIdentifier.MIN_VALUE);
            break;
          }
        }
      }
    }
    if (lowerBoundExtendingWithZero != null
        && compareTo(lowerBoundExtendingWithZero) < 0 && lowerBoundExtendingWithZero.compareTo(nextReleaseImpl) < 0) {
      return lowerBoundExtendingWithZero;
    }
    throw new IllegalArgumentException("Can't find a prerelease between " + this + " and " + nextRelease);
  }

  private static SemanticVersion withLastPrereleaseId(SemanticVersionImpl semanticVersion,
      int newLength, PrereleaseIdentifier finalPrereleaseId) {
    PrereleaseIdentifier[] prereleaseIdentifiers = Arrays.copyOf(semanticVersion.prereleaseVersionArray,
        newLength);
    prereleaseIdentifiers[newLength - 1] = finalPrereleaseId;
    return semanticVersion.prereleaseWithIdentifiers(prereleaseIdentifiers);
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof SemanticVersion that)) {
      return false;
    }
    if (o instanceof SemanticVersionImpl thatImpl) {
      return majorVersionUnsignedUnboxed == thatImpl.majorVersionUnsignedUnboxed &&
          minorVersionUnsignedUnboxed == thatImpl.minorVersionUnsignedUnboxed &&
          patchVersionUnsignedUnboxed == thatImpl.patchVersionUnsignedUnboxed &&
          Arrays.equals(prereleaseVersionArray, thatImpl.prereleaseVersionArray) &&
          Objects.equals(buildMetadata, thatImpl.buildMetadata);
    }
    return majorVersionUnsignedUnboxed == that.majorVersionUnsignedUnboxed() &&
        minorVersionUnsignedUnboxed == that.minorVersionUnsignedUnboxed() &&
        patchVersionUnsignedUnboxed == that.patchVersionUnsignedUnboxed() &&
        Objects.equals(prereleaseVersion(), that.prereleaseVersion()) &&
        Objects.equals(buildMetadata, that.buildMetadata());
  }

  @Override
  public int hashCode() {
    int result = Objects.hash(majorVersionUnsignedUnboxed, minorVersionUnsignedUnboxed, patchVersionUnsignedUnboxed,
        buildMetadata);
    result = 31 * result + Arrays.hashCode(prereleaseVersionArray);
    return result;
  }
}
