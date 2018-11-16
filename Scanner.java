package basicScript;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
	// functions
	private boolean isReadingFunction = false;
	private Map<String, String> functions = new HashMap<>();
	private Map<String, List<String>> functionParameters = new HashMap<>();
	private String functionName = "";
	private String functionBody = "";
	private List<String> localVariables = new ArrayList<>();
	private List<String> parameters = new ArrayList<>();
	private Stack<Boolean> braceStack = new Stack<>();
	// assignment operation
	private boolean isAssignment = false;
	private String assignmentStatement = "";
	private String assignmentExpression = "";
	// ti notation or not
	private static Notation notation = Notation.SOURCE_CODER;
	// elif
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
				while (!Utils.isNewline(peek()) && !isAtEnd()) {
					advance();
				}
				while (Utils.isNewline(peek()) && !isAtEnd()) {
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
			if (!updateBlock('}')) {
				// ignore '}' at end of function definition block
				closingBrace();
			}
			break;
		case '\n':
		case '\r':
			newLine(c);
			break;
		case '{':
			updateBlock('{');
			break;
		case '\t':
			// ignore tab
			break;
		case '$':
			variable();
			break;
		case '=':
			if (!isAtEnd() && peek() == '=') {// equality test
				addToken("=");
			}
			// assignment operation handled by variable
			break;
		default:
			if (Utils.isAlpha(c)) {
				identifier();
			} else {// add all other characters to target
				addToken(Character.toString(c));
			}
		}
	}

	private void newLine(char c) {
		line++;
		// handle newline character(s) for different operating systems
		if (c == '\r') {
			if (peek() == '\n') {
				advance();// skip over '\n'
				addToken("\r\n");
			} else {
				addToken("\r");
			}
		} else if (c == '\n') {
			if (peek() == '\r') {
				advance();// skip over '\r'
				addToken("\n\r");
			} else {
				addToken("\n");
			}
		}
		if (isAssignment) {// assignment terminated by newline
			isAssignment = false;
			terminateAssignment();
		}
	}

	private void terminateAssignment() {
		assignmentStatement = assignmentExpression + assignmentStatement + "\n";
		addToken(assignmentStatement);
		assignmentExpression = "";
		assignmentStatement = "";
	}

	private void startAssignment() {
		if (peek() == '=' && peekNext() != '=') {// assignment operation
			System.out.println("assignment statement found!");
			isAssignment = true;
		}
	}

	private void variable() {
		while (!isAtEnd() && Utils.isAlphaNumeric(peek())) {
			advance();
		}
		String variableName = source.substring(start, current);
		// System.out.println("variableName: " + variableName);
		startAssignment();
		if (isReadingFunction) {
			if (!parameters.contains(variableName)) {// function's local variables
				if (!localVariables.contains(variableName)) {
					localVariables.add(variableName);
				}
				int listIndex = localVariables.indexOf(variableName) + 1;
				// function variable scope
				addToken(mapSymbol("L") + "F" + functions.size() + "(" + listIndex + ")");
			} else {// parameter variables
				addToken(variableName);
			}
		} else {
			addToken(getVariable(variableName));
		}
	}

	private String getVariable(String variableName) {
		if (variableName.startsWith("$")) {
			if (!variables.contains(variableName)) {
				variables.add(variableName);
			}
			int listIndex = variables.indexOf(variableName) + 1;
			// main program variable scope
			return mapSymbol("L") + "M(" + listIndex + ")";
		} else {
			return variableName;
		}
	}

	/**
	 * Update brace stack to find the end of a function block
	 * 
	 * @param c
	 *            whether to ignore the "}" or not
	 * @return
	 */
	private boolean updateBlock(char c) {
		if (isReadingFunction) {
			if (c == '{') {
				braceStack.push(true);
			} else if (c == '}') {
				braceStack.pop();
			}
			if (braceStack.isEmpty()) {
				// find the end of a block of a function
				// terminate reading of function
				isReadingFunction = false;
				functions.put(functionName, functionBody);
				// clear variables used for reading function
				functionBody = "";
				localVariables.clear();
				System.out.println("functions: " + functions);
				safeSkipLine();// skip the empty line left by function definition
				return true;
			}
		}
		return false;
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
				addToken("End");
			}
		} else {
			addToken("End");
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
				 processElif();
				break;
			default:
				keyword(identifier);
			}
		} else {
			switch (identifier) {
			case "func":
				processFunctionDefinition();
				break;
			default:
				if (functions.keySet().contains(identifier)) {// function caller
					processFunctionCall(identifier);
				} else {
					// other TI-Basic keywords, like "getKey", "Str1", etc.
					startAssignment();
					addToken(identifier);
				}
			}
		}
	}

	private void processFunctionCall(String functionName) {
		int openBraceIndex = Utils.substringIndexOf("(", source, current);
		int closeBraceIndex = Utils.substringIndexOf(")", source, current);
		String parameterList = source.substring(openBraceIndex + 1, closeBraceIndex).replace(" ", "");
		List<String> callParameters = Arrays.asList(parameterList.split(","));
		String functionBody = functions.get(functionName);
		List<String> functionParameters = this.functionParameters.get(functionName);
		for (int i = 0; i < callParameters.size(); i++) {
			String parameter = functionParameters.get(i);
			String argument = getVariable(callParameters.get(i));
			System.out.println("parameter: " + parameter);
			System.out.println("argument: " + argument);
			Pattern parameterPattern = Pattern.compile(Pattern.quote(parameter) + "\\b");
			System.out.println("pattern: " + parameterPattern);
			Matcher parameterMatcher = parameterPattern.matcher(functionBody);
			if (argument.startsWith("$")) {
				functionBody = parameterMatcher.replaceAll("\\" + argument);
			} else {
				functionBody = parameterMatcher.replaceAll(argument);
			}
			System.out.println(functionBody);
		}
		while (advance() != '\n') {
			// keep skipping function call statement
		}
		addToken(functionBody);
	}

	private void processFunctionDefinition() {
		// copy-and-paste functions
		// process function header
		String functionHeader = source.substring(current, Utils.substringIndexOf("{", source, current)).trim();
		int firstBraceIndex = functionHeader.indexOf("(");
		functionName = functionHeader.substring(0, functionHeader.indexOf("(")).trim();
		String parameterList = functionHeader.substring(firstBraceIndex);
		parameterList = parameterList.substring(1, parameterList.length() - 1);// remove "(" and ")"
		parameterList = parameterList.replace(" ", "");
		// System.out.println("parameterList: " + parameterList);
		parameters = Arrays.asList(parameterList.split(","));
		functionParameters.put(functionName, parameters);
		// System.out.println("parameters: " + parameters);
		// prepare for reading funcion body
		isReadingFunction = true;
		// skip the function header
		while (advance() != '{') {
			// keep advancing
		}
		safeSkipLine(); // skip potential '\n'
		updateBlock('{');
	}

	private void processElif() {
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		String conditionalExpression = scanConditionalExpression("elif");
		addToken("Else\nIf" + conditionalExpression + "\nThen");
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

	private String mapSymbol(String symbol) {
		switch (notation) {
		case TI:
			switch (symbol) {
			case "->":
				return "→";
			case "L":
				return "ʟ";
			default:
				return null;// can throw exception if needed
			}
		case SOURCE_CODER:
			switch (symbol) {
			case "->":
				return "->";
			case "L":
				return "|L";
			default:
				return null;// can throw exception if needed
			}
		case PLAIN:
			return symbol;
		default:
			return null;// can throw exception if needed
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
		String conditionalExpression = scanConditionalExpression("if");
		String text = "If" + conditionalExpression + "\nThen";
		addToken(text);
	}
	
	private String scanConditionalExpression(String keyword) {
		nestedScanner = new Scanner(source.substring(start + keyword.length(), current));
		nestedScanner.setVariables(this.variables);
		String conditionalExpression = nestedScanner.scanTokens();
		this.setVariables(nestedScanner.variables);
		return conditionalExpression;
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
		if (isAssignment) {
			if (assignmentStatement.isEmpty()) {
				assignmentStatement += mapSymbol("->") + token;// remove possible newline
			} else {
				if (Utils.isNewline(token)) {
					// ignore newlines
				} else {
					assignmentExpression += token;
				}
			}
			System.out.println("assignmentStatement: " + assignmentStatement);
			System.out.println("assignmentExpression: " + assignmentExpression);
		} else if (isReadingFunction) {
			this.functionBody += token;
		} else {
			this.target += token;
		}
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

	private char peekNext() {
		return peek(1);
	}

	private char peek(int n) {
		if (isAtEnd())
			return '\0';
		return source.charAt(current + n);
	}

	private char peekAcrossWhitespace() {
		char c = peek();
		if (!isAtEnd() && Character.isWhitespace(c)) {
			// keep peeking ahead
			c = peek();
		}
		return c;
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
			if (peek() == '\r') {// rare newline, "\r"
				advance();
				if (peek() == '\n') {// Windows newline, "\r\n"
					advance();
				}
			} else if (peek() == '\n') {// Unix and macOS newline, "\n"
				advance();
				if (peek() == '\r') {// rare newline, "\n\r"
					advance();
				}
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
