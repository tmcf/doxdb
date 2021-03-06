package net.trajano.doxdb;

import java.io.Serializable;
import java.util.Arrays;
import java.util.concurrent.ThreadLocalRandom;

/**
 * An generated ID object. Unlike a UUID, it has less character restrictions. It
 * is a {@value #LENGTH} character random number string. Implementation wise it
 * uses a less secure but more performant {@link ThreadLocalRandom} generator to
 * generate new values.
 *
 * @author Archimedes
 */
public final class DoxID implements
    Serializable {

    /**
     * Set of allowed characters in the ID. Lower case letters or numbers only.
     * This provides higher than 128-bits but less than 256-bits of randommess.
     * Lower case letters are used to ensure exports using DOXID as a file name
     * will work on case insensitive file systems.
     */
    private static final char[] ALLOWED;

    /**
     * This is transpose of the {@link #ALLOWED} array used to look up
     * characters to ensure they are allowed.
     */
    private static final boolean[] ALLOWED_MAP;

    /**
     * Size of the ID in bytes.
     */
    public static final int LENGTH = 32;

    /**
     * bare_field_name.
     */
    private static final long serialVersionUID = -1726922680838907757L;

    static {
        ALLOWED = "0123456789abcdefghijklmnopqrstuvwxyz".toCharArray();
        ALLOWED_MAP = new boolean[256];
        for (final char allowedChar : ALLOWED) {
            ALLOWED_MAP[allowedChar] = true;
        }
    }

    /**
     * Generate a new instance with a randomized ID value.
     *
     * @return
     */
    public static DoxID generate() {

        return new DoxID();

    }

    private final char[] b = new char[LENGTH];

    /**
     * Calculated hashcode value.
     */
    private final int hash;

    private DoxID() {

        final ThreadLocalRandom random = ThreadLocalRandom.current();
        int currentHash = LENGTH;
        for (int i = 0; i < LENGTH; ++i) {
            final int rand = random.nextInt(0, ALLOWED.length);
            b[i] = ALLOWED[rand];
            currentHash += b[i];
        }
        hash = currentHash;

    }

    public DoxID(final String s) {

        if (s.length() != LENGTH) {
            throw new IllegalArgumentException("input needs to be " + LENGTH + " in length.");
        }
        int currentHash = LENGTH;
        final char[] chars = s.toCharArray();
        for (int i = 0; i < LENGTH; ++i) {
            if (ALLOWED_MAP[chars[i]]) {
                b[i] = chars[i];
                currentHash += chars[i];
            } else {
                throw new IllegalArgumentException("Invalid character for DoxID");
            }

        }
        hash = currentHash;
    }

    @Override
    public boolean equals(final Object obj) {

        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof DoxID)) {
            return false;
        }
        final DoxID other = (DoxID) obj;
        return Arrays.equals(b, other.b);
    }

    @Override
    public int hashCode() {

        return hash;
    }

    /**
     * This is the string representation of the key. This is normally used for
     * persisting.
     */
    @Override
    public String toString() {

        return new String(b);
    }
}
