package me.jezza.lib;

import java.util.function.Function;

/**
 * @author Jezza
 */
public final class Strings {
	private static final char[] OBJ_REP_CHARS = "{}".toCharArray();
	private static final String SAFE_CHARS = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNPPQRSTUVWXYZ$_1234567890";

	private Strings() {
		throw new IllegalStateException();
	}

	public static String confirmSafe(String name) {
		if (!useable(name)) {
			throw new IllegalArgumentException("Experiment name must not be empty: " + name);
		}
		for (int i = 0; i < name.length(); i++) {
			char c = name.charAt(i);
			if (SAFE_CHARS.indexOf(c) == -1)
				throw new IllegalArgumentException(format("Invalid character in experiment name: \"{}\"[{}] = '{}'.", name, Integer.toString(i), Character.toString(c)));
		}
		return name;
	}

	public static boolean useable(CharSequence charSequence) {
		if (charSequence == null || charSequence.length() == 0)
			return false;
		for (int i = 0; i < charSequence.length(); i++)
			if (charSequence.charAt(i) > ' ')
				return true;
		return false;
	}

	/**
	 * Replaces occurences of "{}" in the original string with String-representations of the given objects.<br>
	 * <br>
	 * <i>NOTE: This method is very cheap to call and is highly optimized.
	 * If you want to change anything in this method, talk to Jeremy or Dirk about it first!</i>
	 */
	public static String format(final String original, final Object... objects) {
		if (original == null) {
			return null;
		}
		int length = original.length();
		if (length == 0) {
			return "";
		}
		if (objects == null || objects.length == 0) {
			return original;
		}

		int objectLength = objects.length;
		int calculatedLength = length;
		final String[] params = new String[objectLength];
		while (--objectLength >= 0) {
			final Object obj = objects[objectLength];
			if (obj != null) {
				final String param = obj.toString();
				calculatedLength += param.length();
				params[objectLength] = param;
			} else {
				calculatedLength += 4;
				params[objectLength] = "null";
			}
		}
		objectLength = objects.length;

		final char[] rep = OBJ_REP_CHARS;
		final int repLength = rep.length;
		final char[] result = new char[calculatedLength];
		char[] param;
		original.getChars(0, length, result, 0);
		for (int i = 0, index = 0, end, paramLength; i < objectLength; i++) {
			index = indexOf(result, 0, length, rep, 0, repLength, index);
			if (index < 0) {
				return new String(result, 0, length);
			}
			end = index + repLength;
			if (end > length) {
				end = length;
			}
			param = params[i].toCharArray();
			paramLength = param.length;
			// Shifts the entire result array down to fit the parameter.
			System.arraycopy(result, end, result, index + paramLength, length - end);
			// Copys the parameter into the result array.
			System.arraycopy(param, 0, result, index, paramLength);
			// The new length of the used characters.
			length = length + paramLength - (end - index);
			// Moves the index to AFTER the parameter we just inserted.
			index += paramLength;
		}
		return new String(result, 0, length);
	}

	/**
	 * Performs an indexOf search on a char array, with another char array.
	 * Think of it as lining up the two arrays, and returning the index that it matches.
	 * Or just think of it as an indexOf...
	 *
	 * @param   source       - the characters being searched.
	 * @param   target       - the characters being searched for.
	 * @param   fromIndex    - the index to begin searching from.
	 * @return - the index that the target array was found at within the source array
	 */
	public static int indexOf(final char[] source, final char[] target, final int fromIndex) {
		return indexOf(source, 0, source.length, target, 0, target.length, fromIndex);
	}

	/**
	 * Performs an indexOf search on a char array, with another char array.
	 * Think of it as lining up the two arrays, and returning the index that it matches.
	 *
	 * @param   source       - the characters being searched.
	 * @param   sourceOffset - offset of the source string.
	 * @param   sourceCount  - count of the source string.
	 * @param   target       - the characters being searched for.
	 * @param   targetOffset - offset of the target string.
	 * @param   targetCount  - count of the target string.
	 * @param   fromIndex    - the index to begin searching from.
	 * @return - the index that the target array was found at within the source array
	 */
	public static int indexOf(final char[] source, final int sourceOffset, final int sourceCount, final char[] target, final int targetOffset, final int targetCount, int fromIndex) {
		if (fromIndex >= sourceCount) {
			return targetCount == 0 ? sourceCount : -1;
		}
		if (fromIndex < 0) {
			fromIndex = 0;
		}
		if (targetCount == 0) {
			return fromIndex;
		}

		final char first = target[targetOffset];
		final int max = sourceOffset + sourceCount - targetCount;

		for (int i = sourceOffset + fromIndex; i <= max; i++) {
			// Look for first character.
			if (source[i] != first) {
				while (++i <= max && source[i] != first) {
				}
			}

			// Found first character, now look at the rest of v2
			if (i <= max) {
				int j = i + 1;
				final int end = j + targetCount - 1;
				for (int k = targetOffset + 1; j < end && source[j] == target[k]; j++, k++) {
				}
				if (j == end) {
					return i - sourceOffset; // Found whole string.
				}
			}
		}
		return -1;
	}

	/**
	 * Formats a chunk of text, replacing defined tokens by the start and end, and passes the value off to the given function.
	 *
	 * eg:
	 * <pre>
	 *  String result = formatKey("This is a {hello}", "{", "}", k -> Integer.toString(k.length()));
	 * </pre>
	 *  result = "This is a 5"
	 *
	 * <pre>
	 *  String[] values = {"First", "Second"};
	 *  String result = formatKey("This is a [0][1]", "[", "]", k -> values[Integer.valueOf(k)]);
	 * </pre>
	 * result = "This is a FirstSecond"
	 *
	 * @param input 	- The text to scan and alter
	 * @param startKey 	- The series of characters that start the token
	 * @param endKey 	- The series of characters that end the token
	 * @param transform - The function to apply to the value found between the startKey and the endKey
	 * @return - The formatted result.
	 */
	public static String formatToken(final String input, final String startKey, final String endKey, final Function<String, String> transform) {
		final int startKeyLength = startKey.length();
		final int endKeyLength = endKey.length();

		final StringBuilder output = new StringBuilder(input.length());
		int start;
		int end = -endKeyLength;
		int diff;

		while (true) {
			end += endKeyLength;
			start = input.indexOf(startKey, end);
			if (start < 0) {
				break;
			}
			diff = start - end;
			if (diff > 0) {
				if (diff == 1) {
					output.append(input.charAt(end));
				} else {
					output.append(input.substring(end, start));
				}
			}
			if (start + 1 >= input.length()) {
				throw new IllegalArgumentException("Unmatched token @ position: " + start);
			}
			end = input.indexOf(endKey, start + startKeyLength);
			if (end < 0) {
				throw new IllegalArgumentException("Unmatched token @ position: " + start);
			}
			diff = end - start - startKeyLength;
			if (diff > 0) {
				if (diff > 1) {
					output.append(transform.apply(input.substring(start + startKeyLength, end)));
				} else {
					output.append(transform.apply(String.valueOf(input.charAt(start + startKeyLength))));
				}
			} else {
				output.append(transform.apply(""));
			}
		}

		if (-1 < end && end < input.length()) {
			output.append(input.substring(end));
		}
		return output.toString();
	}
}
