package basicScript;

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
