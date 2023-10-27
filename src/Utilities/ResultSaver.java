package Utilities;

import java.io.BufferedWriter; 
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class ResultSaver {
	 public static void appendStrToFile(String fileName,
             String str)
{
// Try block to check for exceptions
try {

// Open given file in append mode by creating an
// object of BufferedWriter class
BufferedWriter out = new BufferedWriter(
new FileWriter(fileName, true));

// Writing on output stream
out.write(str);
// Closing the connection
out.close();
}

// Catch block to handle the exceptions
catch (IOException e) {

// Display message when exception occurs
System.out.println("exception occurred" + e);
}
}

	public static void main(String[] args) {

		/*FileWriter fw = null; 
		BufferedWriter bw = null;
		PrintWriter pw = null;
		try {
			fw = new FileWriter("ProOOmpts.txt", true);
		} catch (IOException e) {appendStrToFile("ressfileappendStrToFile("ressfile.txt","this is in in test");.txt","this is in in test");
			// TODO Auto-generated catch block
			e.printStackTrace();
		} 
		bw = new BufferedWriter(fw);
		pw = new PrintWriter(bw);
		run("hi ",pw);*/
		appendStrToFile("ressfile.txt","this is in in test");
	}

	public static void run(String c ,PrintWriter pw)
	{
		//FileWriter fw = null; 
		//BufferedWriter bw = null;
		//PrintWriter pw = null;
		try {
			//fw = new FileWriter("Prompts.txt", true); 
			//bw = new BufferedWriter(fw);
		//	pw = new PrintWriter(bw); 
			pw.println(c);

			System.out.println("Data Successfully appended into file");
			pw.flush(); 
		} 
		finally 
		{ 
			pw.close();
			//bw.close(); 
			//fw.close(); 
		}

	}
}
