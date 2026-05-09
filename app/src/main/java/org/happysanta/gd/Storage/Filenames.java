package org.happysanta.gd.Storage;

import androidx.documentfile.provider.DocumentFile;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Set;

/**
 * Filename sanitization + uniqueness helpers for SAF-stored level packs.
 *
 * <p>The user picks a display name when installing a level. That same string
 * is also used as the on-disk filename (so the file is recognisable in any
 * file explorer and can be moved between devices). User input can't go to
 * the filesystem unsanitized: SAF documents land on a wide range of backing
 * filesystems (ext4 internal, FAT/exFAT SD cards, MTP, network shares),
 * each with their own forbidden characters and length limits.
 *
 * <p>The sanitizer is deliberately conservative: anything FAT/exFAT or
 * Windows would reject is stripped or replaced. Falling back to a
 * "level-{id}" name is preferable to creating a file the user can't move
 * to a PC or SD card later.
 */
public final class Filenames {

	/** Cap base-name length at 200 UTF-8 bytes; safe across FAT, exFAT, ext4. */
	private static final int MAX_BYTES = 200;

	/** Windows-reserved device names (case-insensitive, with or without ext). */
	private static final String[] RESERVED = {
			"CON", "PRN", "AUX", "NUL",
			"COM1", "COM2", "COM3", "COM4", "COM5", "COM6", "COM7", "COM8", "COM9",
			"LPT1", "LPT2", "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
	};

	private Filenames() {}

	/**
	 * Turn a user-provided display name into a safe base filename (no
	 * extension). Always returns a non-empty string; if everything got
	 * stripped, returns {@code "level-" + fallbackId}.
	 *
	 * <p>Steps: NFC normalize → replace path separators with space →
	 * strip control / Windows-reserved chars → collapse whitespace → trim
	 * leading/trailing dots and spaces → reserved-name guard → length cap
	 * (UTF-8 byte count, not Java char count, since FAT counts bytes).
	 */
	public static String sanitizeBase(String input, long fallbackId) {
		if (input == null) input = "";
		// NFC: combine "e" + combining-acute into "é" so two visually-identical
		// names always compare equal as strings (and as filenames).
		String s = Normalizer.normalize(input, Normalizer.Form.NFC);

		StringBuilder out = new StringBuilder(s.length());
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c < 0x20 || c == 0x7F) {
				// Control chars — drop entirely (replacing with space tends to
				// produce ugly runs of spaces from binary garbage in pasted text).
				continue;
			}
			switch (c) {
				case '/':
				case '\\':
					// Path separators must never reach the filesystem; replacing
					// with space (rather than dropping) preserves the word break.
					out.append(' ');
					break;
				case '<': case '>': case ':': case '"':
				case '|': case '?': case '*':
					// Windows / FAT-forbidden. Drop — replacement char would be
					// arbitrary and the result is still readable without them.
					break;
				default:
					out.append(c);
			}
		}

		// Collapse runs of whitespace (the path-separator → space replacement
		// can leave doubles).
		String collapsed = out.toString().replaceAll("\\s+", " ");

		// Strip leading/trailing dots and spaces. Trailing dots break Windows;
		// leading dots make hidden files on Unix.
		String trimmed = collapsed.replaceAll("^[.\\s]+", "").replaceAll("[.\\s]+$", "");

		// Reserved-device-name guard. "CON.mrg" is illegal on Windows (and
		// some Android file pickers reject it), so we suffix with underscore.
		String upper = trimmed.toUpperCase(Locale.ROOT);
		for (String reserved : RESERVED) {
			if (upper.equals(reserved)) {
				trimmed = trimmed + "_";
				break;
			}
		}

		// UTF-8 byte cap. Truncate at a char boundary so we never produce
		// an invalid byte sequence.
		trimmed = capUtf8Bytes(trimmed, MAX_BYTES);

		if (trimmed.isEmpty()) {
			return "level-" + fallbackId;
		}
		return trimmed;
	}

	/** Append {@code .mrg} to a sanitized base. */
	public static String withMrgExtension(String base) {
		return base + ".mrg";
	}

	/**
	 * Given a desired base and a folder, return a filename that doesn't
	 * collide with anything already in the folder OR in {@code alsoTaken}.
	 * On collision, suffixes "{base} (2).mrg", "{base} (3).mrg", etc.
	 *
	 * <p>{@code alsoTaken} is for the case where we're computing names
	 * for several rows in one pass (e.g. migration) and need to avoid
	 * collisions among rows whose files don't exist yet.
	 */
	public static String uniqueIn(DocumentFile folder, String desiredBase, Set<String> alsoTaken) {
		String candidate = withMrgExtension(desiredBase);
		if (!isTaken(folder, alsoTaken, candidate)) {
			return candidate;
		}
		for (int i = 2; i < 10000; i++) {
			candidate = desiredBase + " (" + i + ").mrg";
			if (!isTaken(folder, alsoTaken, candidate)) {
				return candidate;
			}
		}
		// Astronomically unlikely. Still need a stable fallback.
		return desiredBase + "-" + System.currentTimeMillis() + ".mrg";
	}

	/**
	 * True if {@code name} ends with {@code .mrg} (case-insensitive) and the
	 * bare-name portion isn't empty. Used by the folder scanner to skip
	 * unrelated files.
	 */
	public static boolean isMrgFilename(String name) {
		if (name == null || name.length() <= 4) return false;
		return name.toLowerCase(Locale.ROOT).endsWith(".mrg");
	}

	private static boolean isTaken(DocumentFile folder, Set<String> alsoTaken, String name) {
		if (alsoTaken != null && alsoTaken.contains(name)) return true;
		if (folder == null) return false;
		DocumentFile existing = folder.findFile(name);
		return existing != null && existing.exists();
	}

	private static String capUtf8Bytes(String s, int maxBytes) {
		if (s.getBytes(java.nio.charset.StandardCharsets.UTF_8).length <= maxBytes) {
			return s;
		}
		// Walk back char-by-char until the encoded byte count fits. Handles
		// surrogate pairs because String.length() counts code units, and a
		// surrogate pair is two code units, both of which we'd peel off
		// together when one alone fails the byte check.
		StringBuilder b = new StringBuilder(s);
		while (b.length() > 0
				&& b.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8).length > maxBytes) {
			b.deleteCharAt(b.length() - 1);
		}
		// If we cut mid-surrogate, the last char is a high surrogate — strip it.
		if (b.length() > 0 && Character.isHighSurrogate(b.charAt(b.length() - 1))) {
			b.deleteCharAt(b.length() - 1);
		}
		return b.toString();
	}
}
