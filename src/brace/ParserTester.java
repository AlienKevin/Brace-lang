package brace;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ParserTester {
	private static Path script;
	private static Path basic;
	public static void main(String[] args) throws IOException {
		elif();
		Parser parser = new Parser(script, basic);
		parser.parse();
		System.out.println("Script Parsed!");
	}
	private static void general() {
		script = Paths.get("testCases//general//testGeneralScript.txt");
		basic = Paths.get("testCases//general//testGeneralBasic.txt");
	}
	private static void elif() {
		script = Paths.get("testCases//elif//testElifScript.txt");
		basic = Paths.get("testCases//elif//testElifBasic.txt");
	}
}
