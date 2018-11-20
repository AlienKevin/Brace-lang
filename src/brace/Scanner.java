package brace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import logging.JSimpleLog;

/**
 * Scan the script character by character, based upon the Scanner class in
 * <i>Crafting Interpreter</i> by Bob Nystrom
 * 
 * @author Kevin Li
 *
 */
public class Scanner {
	private int start = 0;
	private int current = 0;
	private int line = 1;
	private String source;
	private String target;
	private static final List<String> keywords = Arrays
			.asList(new String[] { "if", "else", "elif", "for", "while", "repeat", "true", "false" });
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
	private static final int MAX_ELIF_NEST_LEVEL = 10;// the maximum level of nesting for "elif"s
	private List<Integer> elifCount = Utils.initializeList(new ArrayList<Integer>(), MAX_ELIF_NEST_LEVEL, 0);
	private int elifNestLevel = -1; // default before encountering any "if"s and "elif"s
	private List<Boolean> isEndOfElifs = Utils.initializeList(new ArrayList<Boolean>(), MAX_ELIF_NEST_LEVEL, false);
	// logging
	private JSimpleLog log = new JSimpleLog();

	public Scanner(String source) {
		setSource(source);
		// set up logging behavior
		log.categorize("branching", "if", "elif", "else");
		log.setFormLog("branching", false);
		log.setFormLog("identifier", true);
		log.setFormLog("closingBrace", true);
		log.setFormLog(JSimpleLog.UNSPECIFIED, false);
	}

	/**
	 * Turn on/off logging in Scanner class
	 * 
	 * @param isLogging
	 *            whether to log or not
	 */
	public void setLog(boolean isLogging) {
		if (isLogging) {
			this.log.on();
		} else {
			this.log.off();
		}
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
				skipLine();
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
				advance();// skip over the second '='
				skipSpaces();
			}
			// assignment operation handled by variable
			break;
		case '|':
			if (match('|')) {// logical or operation
				if (!Utils.isSpace(lookAt(current - 3))) {// look before the "||" for space
					addToken(" ");
				}
				addToken("or");
				if (!Utils.isSpace(peek())) {// look after the "||" for space
					addToken(" ");
				}
			} else {
				addToken("|");// single '|'
			}
			break;
		case '&':
			if (match('&')) {// logical and operation
				if (!Utils.isSpace(lookAt(current - 3))) {// look before the "&&" for space
					addToken(" ");
				}
				addToken("and");
				if (!Utils.isSpace(peek())) {// look after the "&&" for space
					addToken(" ");
				}
			} else {
				addToken("&");// single '&'
			}
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
		log.setType("assignment");
		assignmentStatement = assignmentExpression + assignmentStatement + "\n";
		addToken(assignmentStatement);
		assignmentExpression = "";
		assignmentStatement = "";
		log.reset();
	}

	private void checkAssignment() {
		log.setType("assignment");
		int index = lookAcrossSpaces(current);
		if (lookAt(index) == '=' && lookAt(index + 1) != '=') {// assignment operation
			log.out("assignment statement found!");
			isAssignment = true;
		}
		log.reset();
	}

	private void variable() {
		while (!isAtEnd() && Utils.isAlphaNumeric(peek())) {
			advance();
		}
		String variableName = source.substring(start, current);
		// System.out.println("variableName: " + variableName);
		checkAssignment();
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
		skipSpaces();// skip spaces after variable name
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
		log.setType("function");
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
				log.out("functions: " + functions);
				skipLine();// skip the empty line left by function definition
				log.reset();
				return true;
			}
		}
		log.reset();
		return false;
	}

	private void closingBrace() {
		log.setType("closingBrace");
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
		log.out("isEndOfElifs=" + isEndOfElifs);
		log.out("elifNestLevel=" + elifNestLevel);
		if (elifNestLevel >= 0) {//is in an if-elif-else chain
			if (Utils.getListElement(isEndOfElifs, elifNestLevel) == true) {
				for (int i = 0; i < elifCount.get(elifNestLevel); i++) {
					addToken("\nEnd");
				}
				if (elifNestLevel > 0) {// only decrement nest level
										// if the statement is nested
					elifNestLevel--;
				}
			}
		}
		log.reset();
	}

	private void string() {
		log.setType("string");
		while (peek() != '"' && !isAtEnd()) {
			advance();
		}
		advance();// include the closing '"'
		addToken(source.substring(start, current));
		log.reset();
	}

	private void identifier() {
		log.setType("identifier");
		while (Utils.isAlphaNumeric(peek())) {
			advance();
		}
		String identifier = source.substring(start, current);
		// System.out.println("elif count: " + elifCount);
		if (keywords.contains(identifier)) {
			switch (identifier) {
			case "if":
				processIf();
				break;
			case "elif":
				processElif();
				break;
			case "else":
				processElse();
				break;
			case "true":
				addToken("1");
				break;
			case "false":
				addToken("0");
				break;
			case "for":
				processFor();
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
					log.out("identifier: " + identifier);
					checkAssignment();
					addToken(identifier);
				}
			}
		}
		log.reset();
	}

	private void processFor() {
		log.setType("for");
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		// always append "Then" after "if"
		String conditionalExpression = scanConditionalExpression("for");
		String text = "For" + conditionalExpression;
		addToken(text);
		// restart the elif count
		log.reset();
	}

	private void processFunctionCall(String functionName) {
		log.setType("function");
		int openBraceIndex = Utils.substringIndexOf("(", source, current);
		int closeBraceIndex = Utils.substringIndexOf(")", source, current);
		String parameterList = source.substring(openBraceIndex + 1, closeBraceIndex).replace(" ", "");
		List<String> callParameters = Arrays.asList(parameterList.split(","));
		String functionBody = functions.get(functionName);
		List<String> functionParameters = this.functionParameters.get(functionName);
		for (int i = 0; i < callParameters.size(); i++) {
			String parameter = functionParameters.get(i);
			String argument = getVariable(callParameters.get(i));
			log.out("parameter: " + parameter);
			log.out("argument: " + argument);
			Pattern parameterPattern = Pattern.compile(Pattern.quote(parameter) + "\\b");
			log.out("pattern: " + parameterPattern);
			Matcher parameterMatcher = parameterPattern.matcher(functionBody);
			if (argument.startsWith("$")) {
				functionBody = parameterMatcher.replaceAll("\\" + argument);
			} else {
				functionBody = parameterMatcher.replaceAll(argument);
			}
			log.out(functionBody);
		}
		while (advance() != '\n') {
			// keep skipping function call statement
		}
		addToken(functionBody);
		log.reset();
	}

	private void processFunctionDefinition() {
		log.setType("function");
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
		skipLine(); // skip potential '\n'
		updateBlock('{');
		log.reset();
	}

	private void processElif() {
		log.setType("elif");
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		String conditionalExpression = scanConditionalExpression("elif");
		addToken("Else\nIf" + conditionalExpression + "\nThen");
		log.out("elifNestLevel: " + elifNestLevel);
		Utils.incrementListElement(elifCount, elifNestLevel);
		checkEndOfElif();
		log.reset();
	}

	private void checkEndOfElif() {
		log.setType("elif");
		int closingBraceIndex = findMatchingBrace(current);
		int index = closingBraceIndex + 1;
		if (lookAtEnd(index)) {
			// This elif statement is the last in the chain
			isEndOfElifs.set(elifNestLevel, true);
		} else {
			while (!lookAtEnd(index) && Character.isWhitespace(lookAt(index))) {
				index++;
			}
			log.out("index=" + index);
			log.out("source.length()=" + source.length());
			if (Utils.isAlpha(lookAt(index))) {
				String keyword = source.substring(index, index + 4);
				if (keyword.equals("else") || keyword.equals("elif")) {
					// This elif statement is not the last in the chain
					// Keep looking for more "elif" or "else"
				} else {
					// This elif statement is the last in the chain
					isEndOfElifs.set(elifNestLevel, true);
				}
			} else {
				// This elif statement is the last in the chain
				isEndOfElifs.set(elifNestLevel, true);
			}
		}
		log.reset();
	}

	private void processIf() {
		log.setType("if");
		while (peek() != '{' && !isAtEnd()) {
			advance();
		}
		// always append "Then" after "if"
		String conditionalExpression = scanConditionalExpression("if");
		String text = "If" + conditionalExpression + "\nThen";
		addToken(text);
		// restart the elif count
		elifNestLevel++;
		if (elifCount.size() >= (elifNestLevel + 1)) {
			Utils.clearListElement(elifCount, elifNestLevel);
		}
		log.out("elifNestLevel: " + elifNestLevel);
		checkEndOfElif();
		log.reset();
	}

	private void processElse() {
		log.setType("else");
		log.out("elifNestLevel: " + elifNestLevel);
		keyword("else");
		isEndOfElifs.set(elifNestLevel, true);
		log.reset();
	}

	private int findMatchingBrace(int start) {
		log.setCategory("branching");
		Stack<Boolean> braceStack = new Stack<>();
		for (int i = start; i < source.length(); i++) {
			char c = lookAt(i);
			if (c == '{') {
				braceStack.push(true);
			} else if (c == '}') {
				braceStack.pop();
			}
			if (braceStack.isEmpty()) {
				log.reset();
				return i;
			}
		}
		log.reset();
		return -1;
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
		log.setType("assignment");
		log.out("isAssignment: " + isAssignment);
		if (isAssignment) {
			if (!Utils.isSpace(token.charAt(0))) {
				log.out("token: " + token);
				if (assignmentStatement.isEmpty()) {
					assignmentStatement += mapSymbol("->") + token;// remove possible newline
				} else {
					if (Utils.isNewline(token)) {
						// ignore newlines
					} else {
						assignmentExpression += token;
					}
				}
				log.out("assignmentStatement: " + assignmentStatement);
				log.out("assignmentExpression: " + assignmentExpression);
			}
		} else if (isReadingFunction) {
			this.functionBody += token;
		} else {
			this.target += token;
		}
		log.reset();
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
		if (lookAtEnd(current + n)) {
			return '\0';
		}
		return source.charAt(current + n);
	}

	private char peekAcrossSpaces() {
		char c = peek();
		if (!isAtEnd() && Utils.isSpace(c)) {
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

	private void skipLine() {
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

	private void skipSpaces() {
		while (Utils.isSpace(peek())) {
			advance();
		}
	}

	private boolean isAtEnd() {
		if (current >= source.length()) {
			return true;
		}
		return false;
	}

	private boolean lookAtEnd(int index) {
		if (index >= source.length()) {
			return true;
		}
		return false;
	}

	private char lookAt(int index) {
		if (lookAtEnd(index)) {
			return '\0';
		}
		return source.charAt(index);
	}

	private int lookAcrossSpaces(int index) {
		while (Utils.isSpace(lookAt(index))) {
			index++;
		}
		return index;
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

}
