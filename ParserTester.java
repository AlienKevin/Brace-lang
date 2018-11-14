package basicScript;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ParserTester {
	public static void main(String[] args) throws IOException {
		Path script = Paths.get("src//basicScript//testScript.txt");
		Path basic = Paths.get("src//basicScript//testBasic.txt");
		Parser parser = new Parser(script, basic);
		parser.parse();
		System.out.println("parsed");
	}
}
