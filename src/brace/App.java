package brace;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import logging.JSimpleLog;

public class App {
	private static Scanner in;
	private static Map<String, File> pathShortcuts;
	private static final Path pathShortcutStorage = Paths.get("pathShortcuts.ser");
	private static Path basePath = Paths.get("");
	private static final JSimpleLog log = new JSimpleLog();

	public static void main(String[] args) {
		log.off();
		System.out.println("Welcome to Brace CDL");
		System.out.println("You can convert files from Brace to TI-Basic here");
		System.out.println("Type \"convert\" to convert file");
		System.out.println("Type \"store\" to store file address");
		System.out.println("Type \"exit\" to quit");
		// initialize path shortcuts
		loadPathShortcuts();
		// initialize scanner
		in = new Scanner(System.in);
		handleInput();
		//@formatter:off
		// C:\Users\Kevin Li\eclipse-workspace\Brace-lang\testCases\elif\testElifScript.brace
		// C:\Users\Kevin Li\eclipse-workspace\Brace-lang\testCases\elif\testElifBasic.txt
		//@formatter:on
	}

	private static void loadPathShortcuts() {
		try {
			if (Files.exists(pathShortcutStorage)) {// storage file is created
				try {
					// InputStream is = Files.newInputStream(pathShortcutStorage);
					FileInputStream fs = new FileInputStream(pathShortcutStorage.toFile());
					ObjectInputStream os = new ObjectInputStream(fs);
					log.out("reading object");
					Object obj = os.readObject();
					log.out("successfuly read the object");
					if (obj != null) {
						pathShortcuts = (Map<String, File>) obj;
						log.out("pathShortcuts: " + pathShortcuts);
					}
					os.close();
				} catch (IOException e) {
					log.out("pathShortcuts initialized");
					pathShortcuts = new HashMap<>();
				} catch (ClassNotFoundException e) {
					error("internal program error");
				}
			} else {// storage file is not created
				Files.createFile(pathShortcutStorage);
				log.out("pathShortcut created");
				pathShortcuts = new HashMap<>();
			}
		} catch (IOException e) {
			// error("Invalid path shortcut storage address");
		}
	}

	private static void handleInput() {
		String input = promptNextLine();
		handleCommands(input);
		handleInput();// continue prompting inputs
	}
	
	private static void handleCommands(String input) {
		switch (input.toLowerCase()) {
		case "exit":
			exit();
			break;
		case "store":
			handleStore();
			break;
		case "convert":
			handleConvert();
			break;
		default:
			if (input.startsWith("cd")) {
				try {
					Path path = Paths.get(input.substring("cd".length()).trim());
					log.out("path entered!");
					if (basePath != null) {
						path = addDir(path);
					}
					if (Files.exists(path)) {
						basePath = path;
					} else {
						throw new InvalidPathException(path.toString(), "invalid path");
					}
				} catch (InvalidPathException e) {
					error("Invalid path");
					handleInput();
				}
			} else {
				System.out.println("Please enter \"convert\", \"store\", \"exit\", or \"cd 'path'\"");
			}
		}
	}

	private static void handleStore() {
		System.out.println("Store a file address to reference it later");
		System.out.println("Reference name: ");
		String pathName = promptNextLine();
		handleStorageAddress(pathName);
		System.out.println("Address stored");
	}

	private static void handleStorageAddress(String pathName) {
		System.out.println("File address: ");
		String pathAddress = promptNextLine();
		try {
			Path path = Paths.get(pathAddress);
			path = addDir(path); // add base path
			if (Files.isRegularFile(path)) {
				pathShortcuts.put(pathName, path.toFile());
			} else {
				throw new InvalidPathException(path.toString(), "invalid path");
			}
		} catch (InvalidPathException e) {
			fileNotFound();
			handleStorageAddress(pathName);
		}
	}

	private static Path handlePath(String prompt) {
		System.out.println(prompt);
		String input = promptNextLine();
		Path path = null;
		if (isStoredName(input)) {
			return pathShortcuts.get(input).toPath();
		} else {
			path = Paths.get(input);
		}
		path = addDir(path);// add base path
		if (Files.isRegularFile(path)) {
			return path;
		} else {
			fileNotFound();
			handlePath(prompt);// reenter path
		}
		return null;
	}

	private static Path addDir(Path path) {
		return basePath.resolve(path);
	}

	private static void handleConvert() {
		String braceMessage = "Enter Brace file address: ";
		Path braceFile = handlePath(braceMessage);
		String tiMessage = "Enter TI-Basic file address: ";
		Path tiFile = handlePath(tiMessage);
		Parser parser = new Parser(braceFile, tiFile);
		parser.setLog(false);
		try {
			parser.parse();
		} catch (IOException e) {
			fileNotFound();
		}
		System.out.println("Conversion succeeds!");
	}

	private static void exit() {
		if (pathShortcuts.size() > 0) {
			// store the shortcuts
			try {
				OutputStream os = Files.newOutputStream(pathShortcutStorage);
				log.out("OutputStream is working");
				ObjectOutputStream out = new ObjectOutputStream(os);
				log.out("ObjectOutputStream is working");
				log.out("pathShortcuts: " + pathShortcuts);
				out.writeObject(pathShortcuts);
				log.out("writeObject is working");
				out.close();
			} catch (IOException e) {
				// fileNotFound();
			}
		}
		System.exit(0);
	}

	private static void error(String message) {
		System.out.println(message);
	}

	private static void fileNotFound() {
		error("File not found");
	}

	private static String promptNextLine() {
		if (basePath != null) {
			int pathLength = basePath.getNameCount();
			log.out(pathLength + "");
			if (pathLength > 1) {
				// print the second to last subpath
				System.out.print(basePath.subpath(pathLength - 2, pathLength - 1) + File.separator);
			}
			if (pathLength == 0) {// base path is C:\, D:\, etc.
				System.out.print(basePath);
			} else {
				// print the last subpath
				System.out.print(basePath.subpath(pathLength - 1, pathLength).toString());
			}
		}
		System.out.print("> ");
		String nextLine = in.nextLine();
		switch (nextLine.toLowerCase()) {
		case "exit":
			exit();
			break;
		case "back":
			handleInput();
			break;
		}
		return nextLine;
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
