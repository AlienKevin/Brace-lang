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

}
