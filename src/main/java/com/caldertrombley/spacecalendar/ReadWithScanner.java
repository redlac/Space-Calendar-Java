package com.caldertrombley.spacecalendar;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Scanner;


//TODO:Set utf-8 encoding on final write out.
public class ReadWithScanner {

	private Path fFilePath;
	private final static Charset ENCODING = StandardCharsets.UTF_8;
	
	/**Constructor.
	 * 
	 * @param aFileName full name of an existing, readable file.
	 */
	public ReadWithScanner(String aFileName){
		fFilePath = Paths.get(aFileName);
	}
	
	//Reads each line in the file into an array.
	public final ArrayList<String> processLineByLine() throws IOException{
		ArrayList<String> lineArray = new ArrayList<>();
		try(Scanner scanner = new Scanner(fFilePath, ENCODING.name())){
			while (scanner.hasNextLine()){
				lineArray.add(scanner.nextLine());
			}
			return lineArray;
		}
	}
}
