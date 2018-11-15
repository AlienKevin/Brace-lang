package basicScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;

public class Scanner {
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private String source;
	private String target;
	private static final List<String> keywords = Arrays
			.asList(new String[] { "if", "else", "elif", "for", "while", "repeat" });
	private List<String> variables = new ArrayList<>();
	private Scanner nestedScanner;
	private int closingBraceIndex = 0;
	private int numberOfElif = 1;

	public Scanner(String source) {
		setSource(source);
	}
	
	public void setVariables(List<String> variables) {
		this.variables.clear();
		for (String variable : variables) {
			this.variables.add(variable);
		}
	}
	
	public void setSource(String source) {
		this.source = source;
		this.target = "";
	}

	public String scanTokens() {
		while (!isAtEnd()) {
			start = current;
			scanToken();
		}
		return target;
	}

	private void scanToken() {
		char c = advance();
		if (current == closingBraceIndex) {
			for (int i = 0; i < numberOfElif; i++) {
				addToken("\nEnd");
			}
		}
		switch (c) {
		case '/':
			if (match('/')) {// single line comment
				while (peek() != '\n' && !isAtEnd()) {
					advance();
				}
			} else if (match('*')) {// multi-line comment
				while (!(peek() == '*' && peek(1) == '/') && !isAtEnd()) {
					advance();
				}
				safeAdvance();// skip over '*'
				safeAdvance();// skip over '/'
				safeSkipLine();
			} else {// division sign
				addToken("/");
			}
			break;
		case '"':// string
			string();
			break;
		case '}':
			closingBrace();
			break;
		case '\n':
			line++;
			// addToken("\n");
			break;
		case '{':
		case '\t':
			// ignore all '{'
			break;
		case '$':
			variable();
			break;
		default:
			if (Utils.isAlpha(c)) {
				identifier();
			} else {// add all other characters to target
				addToken(Character.toString(c));
			}
		}
	}
	
	private void variable() {
		while (!isAtEnd() && Utils.isAlphaNumeric(peek())) {
			advance();
		}
		String variableName = source.substring(start + 1, current);
		System.out.println(variableName);
		if (!variables.contains(variableName)) {
			variables.add(variableName);
		}
		int listIndex = variables.indexOf(variableName) + 1;
		addToken("L1(" + listIndex + ")");
	}

	private void closingBrace() {
		int n = 0;
		char c;
		while (Character.isWhitespace(c = peek(n)) && !isAtEnd()) {
			n++;
		}
		if (Utils.isAlpha(c)) {
			int index = current + n;
			while (Utils.isAlpha(source.charAt(index))) {
				index++;
			}
			String identifier = source.substring(current + n, index);
			if (identifier.equals("else") || identifier.equals("elif")) {
				// do NOT append "End"
			} else {
				addToken("\nEnd");
			}
		} else {
			addToken("\nEnd");
		}
	}

	private void string() {
		while (peek() != '"' && !isAtEnd()) {
			advance();
		}
		advance();// include the closing '"'
		addToken(source.substring(start, current));
	}

	private void identifier() {
		while (Utils.isAlphaNumeric(peek())) {
			advance();
		}
		String identifier = source.substring(start, current);
		if (keywords.contains(identifier)) {
			switch (identifier) {
			case "if":
				processIf();
				break;
			case "elif":
				// processElif();
				break;
			default:
				keyword(identifier);
			}
		} else {// other ti-basic keywords, like "getKey"
			addToken(identifier);
		}
	}

	private void processElif() {
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		String text = source.substring(start, current).replace("elif", "Else\nIf") + "\nThen";
		addToken(text);
		if (current > closingBraceIndex) {
			boolean countElif = true;
			closingBraceIndex = current - 1;
			while (countElif) {
				closingBraceIndex = findClosingBrace(closingBraceIndex + 1);
				int index = closingBraceIndex + 1;
				while (!lookAtEnd(index) && Character.isWhitespace(lookAt(index))) {
					index++;
				}
				if (lookAtEnd(index)) {
					countElif = false;
				}
				if (Utils.isAlpha(lookAt(index))) {
					int identifierStart = index;
					while (!lookAtEnd(index) && Utils.isAlpha(lookAt(index))) {
						index++;
					}
					String identifier = source.substring(identifierStart, index);
					System.out.println(identifier);
					if (identifier.equals("elif")) {
						numberOfElif++;
					} else {
						countElif = false;
						numberOfElif = 1;
					}
				} else {
					countElif = false;
					numberOfElif = 1;
				}
			}
		}
	}

	private boolean lookAtEnd(int index) {
		if (index >= source.length()) {
			return true;
		}
		return false;
	}

	private char lookAt(int index) {
		return source.charAt(index);
	}

	private int findClosingBrace(final int startIndex) {
		Stack<Boolean> braceStack = new Stack<>();
		int currentIndex = startIndex;
		// System.out.println(lookAt(currentIndex));
		while (!braceStack.isEmpty() || currentIndex == startIndex) {
			char c = source.charAt(currentIndex);
			if (c == '{') {
				braceStack.push(true);
			} else if (c == '}') {
				braceStack.pop();
			}
			currentIndex++;
		}
		// System.out.println(lookAt(currentIndex-1));
		return currentIndex - 1;
	}

	private void processIf() {
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		// always append "Then" after "if"
		nestedScanner = new Scanner(source.substring(start + 2, current));
		nestedScanner.setVariables(this.variables);
		String conditionalExpression = nestedScanner.scanTokens();
		this.setVariables(nestedScanner.variables);
		String text = "If" + conditionalExpression + "\nThen";
		addToken(text);
	}

	private void keyword(String keyword) {
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		String capitalizedKeyword = keyword.substring(0, 1).toUpperCase() + keyword.substring(1);
		String text = source.substring(start, current).replace(keyword, capitalizedKeyword);
		addToken(text);
	}

	private void addToken(String token) {
		this.target += token;
	}

	private boolean match(char expected) {
		if (isAtEnd())
			return false;
		if (source.charAt(current) != expected)
			return false;
		current++;
		return true;
	}

	private char peek() {
		return peek(0);
	}

	private char peek(int n) {
		if (isAtEnd())
			return '\0';
		return source.charAt(current + n);
	}

	private char advance() {
		current++;
		return source.charAt(current - 1);
	}

	private char safeAdvance() {
		if (!isAtEnd()) {
			return advance();
		}
		return '\0';
	}

	private void safeSkipLine() {
		if (!isAtEnd()) {
			if (lookAt(current) == '\r') {// Windows newline
				advance();
				if (lookAt(current) == '\n') {
					advance();
				}
			} else if (lookAt(current) == '\n') {// Unix and macOS newline
				advance();
			}
		}
	}

	private boolean isAtEnd() {
		if (current >= source.length()) {
			return true;
		}
		return false;
	}

}
