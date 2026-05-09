package org.happysanta.gd.Storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * SHA-256 helpers for {@code .mrg} content fingerprinting. Used to detect
 * "same filename, different content" on rescan: if a stored hash doesn't
 * match the on-disk file, the user has swapped the file out and we should
 * ask them whether to re-bind the row's metadata / scores / unlocks to the
 * new contents.
 *
 * <p>SHA-256 is overkill for tamper-detection of hand-curated level files,
 * but it's already in {@code java.security} on every Android version we
 * support, has zero collision risk for our purposes, and the cost is
 * negligible for the small (KB-range) {@code .mrg} files in play.
 */
public final class Hashing {

	private static final String ALGO = "SHA-256";
	private static final char[] HEX = "0123456789abcdef".toCharArray();

	private Hashing() {}

	/**
	 * Hash an arbitrary input stream. Caller owns closing {@code in}; we
	 * just read until EOF. Returns lowercase hex.
	 */
	public static String sha256(InputStream in) throws IOException {
		MessageDigest md = newSha256();
		byte[] buf = new byte[8192];
		int n;
		while ((n = in.read(buf)) > 0) {
			md.update(buf, 0, n);
		}
		return toHex(md.digest());
	}

	/** Convenience: hash a regular {@link File}. Closes its own stream. */
	public static String sha256(File file) throws IOException {
		InputStream in = new FileInputStream(file);
		try {
			return sha256(in);
		} finally {
			try { in.close(); } catch (IOException ignore) {}
		}
	}

	/**
	 * Fresh SHA-256 {@link MessageDigest}. Public so callers can wrap a
	 * stream in {@link java.security.DigestInputStream} and tee bytes
	 * through it while reading for some other purpose (e.g. parsing the
	 * file header) — that way the file only needs to be opened and read
	 * once instead of twice.
	 */
	public static MessageDigest newSha256() {
		try {
			return MessageDigest.getInstance(ALGO);
		} catch (NoSuchAlgorithmException e) {
			// SHA-256 has been mandatory in the JCE since Java 1.4 / Android 1.0.
			// If it's missing the platform is broken in a way we can't paper over.
			throw new IllegalStateException("SHA-256 unavailable", e);
		}
	}

	/** Lowercase hex of a digest output. */
	public static String toHex(byte[] bytes) {
		char[] out = new char[bytes.length * 2];
		for (int i = 0; i < bytes.length; i++) {
			int v = bytes[i] & 0xff;
			out[i * 2] = HEX[v >>> 4];
			out[i * 2 + 1] = HEX[v & 0x0f];
		}
		return new String(out);
	}
}
