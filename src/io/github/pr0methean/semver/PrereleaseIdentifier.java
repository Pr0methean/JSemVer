package io.github.pr0methean.semver;

import org.checkerframework.checker.signedness.qual.Unsigned;

import java.io.Serializable;
import java.util.Objects;
import java.util.OptionalLong;

/**
 * The &lt;pre-release identifier&gt; element of the BNF specification at
 * https://semver.org/#backusnaur-form-grammar-for-valid-semver-versions.
 */
record PrereleaseIdentifier(boolean hasNumericPart, @Unsigned long numericPart, String suffix)
    implements Comparable<PrereleaseIdentifier>, Serializable {

  public static final PrereleaseIdentifier MIN_VALUE = new PrereleaseIdentifier(true, 0, "");

  @Override
  public int compareTo(PrereleaseIdentifier other) {
    if (hasNumericPart() && !other.hasNumericPart()) {
      return -1; // Numeric comes first
    }
    if (!hasNumericPart()) {
      if (other.hasNumericPart()) {
        return 1; // Numeric comes first
      }
    } else {
      int result = Long.compareUnsigned(numericPart, other.numericPart);
      if (result != 0) {
        return result;
      }
    }
    return suffix().compareTo(other.suffix());
  }

  public static PrereleaseIdentifier valueOf(String input) {
    if (input.isEmpty()) {
      throw new IllegalArgumentException("Can't create an empty chunk");
    }
    int[] numericPart = input.codePoints()
        .peek(SemanticVersion::checkValidIdentifierCodePoint)
        .takeWhile(x -> x >= '0' && x <= '9')
        .toArray();
    int numericLength = numericPart.length;
    if (numericLength == 0) {
      return new PrereleaseIdentifier(false, 0, input);
    }
    int[] suffixPart = input.codePoints().skip(numericLength).toArray();
    return new PrereleaseIdentifier(true,
        Long.parseUnsignedLong(new String(numericPart, 0, numericLength)),
        new String(suffixPart, 0, suffixPart.length));
  }

  @Override
  public String toString() {
    if (!hasNumericPart) {
      return suffix;
    }
    return Long.toUnsignedString(numericPart) + suffix;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PrereleaseIdentifier that = (PrereleaseIdentifier) o;
    if (!Objects.equals(suffix, that.suffix) || hasNumericPart != that.hasNumericPart) {
      return false;
    }
    return !hasNumericPart || numericPart == that.numericPart;
  }

  @Override
  public int hashCode() {
    return Objects.hash(hasNumericPart ? numericPart : -1, suffix);
  }

  @Unsigned
  public OptionalLong numericPartIfPresent() {
    return hasNumericPart() ? OptionalLong.of(numericPart) : OptionalLong.empty();
  }
}
