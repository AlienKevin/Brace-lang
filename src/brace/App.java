package brace;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class App {
	private static Scanner in;
	private static Map<String, Path> pathShortcuts = new HashMap<>();

	public static void main(String[] args) {
		System.out.println("Welcome to Brace CDL");
		System.out.println("You can convert files from Brace to TI-Basic here");
		System.out.println("Type \"convert\" to convert file");
		System.out.println("Type \"store\" to store file addresses");
		System.out.println("Type \"exit\" to quit");
		in = new Scanner(System.in);
		handleInput();
		// C:\Users\Kevin Li\eclipse-workspace\Brace-lang\testCases\elif\testElifScript.brace
		// C:\Users\Kevin Li\eclipse-workspace\Brace-lang\testCases\elif\testElifBasic.txt
	}

	private static void handleInput() {
		String input = promptNextLine();
		if (input.equalsIgnoreCase("exit")) {
			System.exit(0);
		} else if (input.equalsIgnoreCase("store")) {
			handleStore();
		} else if (input.equalsIgnoreCase("convert")) {
			handlePaths();
		} else {
			System.out.println("Please enter \"convert\", \"store\", or \"exit\"");
			handleInput();
		}
	}
	
	private static void handleStore() {
		System.out.println("Store a file address to reference it later");
		System.out.println("Reference name: ");
		String pathName = promptNextLine();
		System.out.println("File address: ");
		String pathAddress = promptNextLine();
		Path path = Paths.get(pathAddress);
		if (Files.exists(path)) {
			pathShortcuts.put(pathName, path);
		}
		System.out.println("Address stored");
		handleInput();// continue handling path inputs
	}
	
	private static Path handlePath(String prompt) {
		System.out.println(prompt);
		String input = promptNextLine();
		Path path = null;
		if (isStoredName(input)) {
			return pathShortcuts.get(input);
		} else {
			path = Paths.get(input);
		}
		if (Files.exists(path)) {
			return path;
		} else {
			System.out.println("File not found");
			handlePath(prompt);// reenter path
		}
		return null;
	}
	
	private static void handlePaths() {
		String braceMessage = "Enter Brace file address: ";
		Path braceFile = handlePath(braceMessage);
		String tiMessage = "Enter TI-Basic file address: ";
		Path tiFile = handlePath(tiMessage);
		Parser parser = new Parser(braceFile, tiFile);
		parser.setLog(false);
		try {
			parser.parse();
		} catch (IOException e) {
			System.out.println("File not found");
		}
		System.out.println("Conversion succeeds!");
	}
	
	private static String promptNextLine() {
		System.out.print("> ");
		return in.nextLine();
	}
	
	private static boolean isStoredName(String name) {
		for (String pathName : pathShortcuts.keySet()) {
			if (pathName.equals(name)) {
				return true;
			}
		}
		return false;
	}
}
