package basicScript;

import java.util.List;

public class Utils {

	public static boolean isAlphaNumeric(char c) {
		return Utils.isAlpha(c) || Utils.isDigit(c);
	}

	public static boolean isDigit(char c) {
		if (c >= '0' && c <= '9') {
			return true;
		}
		return false;
	}

	public static boolean isAlpha(char c) {
		if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) {
			return true;
		}
		return false;
	}
	
	public static boolean isNewline(char c) {
		return c == '\r' || c == '\n';
	}
	public static boolean isNewline(String str) {
		for (int i = 0; i < str.length(); i++) {
			if (!isNewline(str.charAt(i))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * IMPORTANT: may need to expand the definition of space!!!
	 * @param c
	 * @return
	 */
	public static boolean isSpace(char c) {
		return c == '\t' || c == ' ';
	}
	
	public static void incrementListElement(List<Integer> list, int index) {
		int value = list.get(index);
		list.set(index, value + 1);
	}
	
	public static void clearListElement(List<Integer> list, int index) {
		list.set(index, 0);
	}
	
	public static <T> T getListElement(List<T> list, int index) {
		if (list.size() < index + 1) {
			return null;
		}
		return list.get(index);
	}
	
	/**
	 * Initialize a list with value, if the value is an object reference,
	 * the same object reference will be added to all elements of the list
	 * @param list the list to initialize
	 * @param size the size to initialize into
	 * @param value the default value to fill
	 */
	public static <T> List<T> initializeList(List<T> list, int size, T value) {
		for (int i = 0; i < size; i++) {
			list.add(value);
		}
		return list;
	}
	
	public static int substringIndexOf(String token, String str, int start) {
		return substringIndexOf(token, str, start, str.length());
	}

	public static int substringIndexOf(String token, String str, int start, int end) {
		int tokenIndex = str.substring(start, end).indexOf(token);
		if (tokenIndex != -1) {
			return tokenIndex + start;
		} else {
			return -1;
		}
	}
	
}
