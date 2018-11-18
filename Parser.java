package brace;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;

public class Parser {
	private Path scriptFile;
	private Path basicFile;

	public Parser(Path scriptFile, Path basicFile) {
		this.scriptFile = scriptFile;
		this.basicFile = basicFile;
	}

	public Parser(Path scriptFile) {
		this.scriptFile = scriptFile;
	}

	public Parser() {
		// empty constructor
	}

	public String parse() throws IOException {
		byte[] bytes = Files.readAllBytes(scriptFile);
		String input = new String(bytes, Charset.defaultCharset());
		String output = run(input);
		if (basicFile == null) {
			return output;
		} else {
			BufferedWriter writer = Files.newBufferedWriter(basicFile, Charset.forName("UTF-8"));
			writer.write(output);
			writer.close();
		}
		return null;
	}

	public String parse(String commands) {
		return run(commands);
	}

	private String run(String source) {
		Scanner scanner = new Scanner(source);
		return scanner.scanTokens();
	}

}
