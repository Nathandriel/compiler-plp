/**
 * Starter code with JUnit tests for code generation used in the class project in COP5556 Programming Language Principles 
 * at the University of Florida, Spring 2018.
 * 
 * This software is solely for the educational benefit of students 
 * enrolled in the course during the Spring 2018 semester.  
 * 
 * This software, and any software derived from it,  may not be shared with others or posted to public web sites,
 * either during the course or afterwards.
 * 
 *  @Beverly A. Sanders, 2018
 */


package cop5556sp18;

import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.swing.JFrame;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp18.CodeGenUtils.DynamicClassLoader;
import cop5556sp18.AST.Program;

public class CodeGenTest {
	
	//determines whether show prints anything
	static boolean doPrint = true;
	
	static void show(Object s) {
		if (doPrint) {
			System.out.println(s);
		}
	}

	//determines whether a classfile is created
	static boolean doCreateFile = false;

	@Rule
	public ExpectedException thrown = ExpectedException.none();
	

	//values passed to CodeGenerator constructor to control grading and debugging output
	private boolean devel = true; //if true, print devel output
	private boolean grade = true; //if true, print grade output
	
//	private boolean devel = false; 
//	private boolean grade = false; 
	
	//sets the default width and height of newly created images.  Should be small enough to fit on screen.
	public static final int defaultWidth = 1024;
	public static final int defaultHeight = 1024;

	
	/**
	 * Generates bytecode for given input.
	 * Throws exceptions for Lexical, Syntax, and Type checking errors
	 * 
	 * @param input   String containing source code
	 * @return        Generated bytecode
	 * @throws Exception
	 */
	byte[] genCode(String input) throws Exception {
		
		//scan, parse, and type check
		Scanner scanner = new Scanner(input);
		show(input);
		scanner.scan();
		Parser parser = new Parser(scanner);
		Program program = parser.parse();
		TypeChecker v = new TypeChecker();
		program.visit(v, null);
//		show(program);  //It may be useful useful to show this here if code generation fails

		//generate code
		CodeGenerator cv = new CodeGenerator(devel, grade, null, defaultWidth, defaultHeight);
		byte[] bytecode = (byte[]) program.visit(cv, null);
		show(program); //doing it here shows the values filled in during code gen
		//display the generated bytecode
		show(CodeGenUtils.bytecodeToString(bytecode));
		
		//write byte code to file 
		if (doCreateFile) {
			String name = ((Program) program).progName;
			String classFileName = "bin/" + name + ".class";
			OutputStream output = new FileOutputStream(classFileName);
			output.write(bytecode);
			output.close();
			System.out.println("wrote classfile to " + classFileName);
		}
		
		//return generated classfile as byte array
		return bytecode;
	}
	
	/**
	 * Run main method in given class
	 * 
	 * @param className    
	 * @param bytecode    
	 * @param commandLineArgs  String array containing command line arguments, empty array if none
	 * @throws + 
	 * @throws Throwable 
	 */
	void runCode(String className, byte[] bytecode, String[] commandLineArgs) throws Exception  {
		RuntimeLog.initLog(); //initialize log used for grading.
		DynamicClassLoader loader = new DynamicClassLoader(Thread.currentThread().getContextClassLoader());
		Class<?> testClass = loader.define(className, bytecode);
		@SuppressWarnings("rawtypes")
		Class[] argTypes = {commandLineArgs.getClass()};
		Method m = testClass.getMethod("main", argTypes );
		show("Output from " + m + ":");  //print name of method to be executed
		Object passedArgs[] = {commandLineArgs};  //create array containing params, in this case a single array.
		try {
		m.invoke(null, passedArgs);	
		}
		catch (Exception e) {
			Throwable cause = e.getCause();
			if (cause instanceof Exception) {
				Exception ec = (Exception) e.getCause();
				throw ec;
			}
			throw  e;
		}
	}
	

	/**
	 * When invoked from JUnit, Frames containing images will be shown and then immediately deleted.
	 * To prevent this behavior, waitForKey will pause until a key is pressed.
	 * 
	 * @throws IOException
	 */
	void waitForKey() throws IOException {
		System.out.println("enter any char to exit");
		System.in.read();
	}

	/**
	 * When invoked from JUnit, Frames containing images will be shown and then immediately deleted.
	 * To prevent this behavior, keepFrame will keep the frame visible for 5000 milliseconds.
	 * 
	 * @throws Exception
	 */
	void keepFrame() throws Exception {
		Thread.sleep(5000);
	}
	
	
	
	


	/**
	 * Since we are not doing any optimization, the compiler will 
	 * still create a class with a main method and the JUnit test will
	 * execute it.  
	 * 
	 * The only thing it will do is append the "entering main" and "leaving main" messages to the log.
	 * 
	 * @throws Exception
	 */
	@Test
	public void emptyProg() throws Exception {
		String prog = "emptyProg";	
		String input = prog + "{}";
		byte[] bytecode = genCode(input);
		String[] commandLineArgs = {};
		runCode(prog, bytecode, commandLineArgs);
		show("Log:\n "+RuntimeLog.globalLog);
		assertEquals("entering main;leaving main;",RuntimeLog.globalLog.toString());
	}
	

	
	@Test
	public void integerLit() throws Exception {
		String prog = "intgegerLit";
		String input = prog + "{show 3;} ";	
		byte[] bytecode = genCode(input);		
		String[] commandLineArgs = {}; //create command line argument array to initialize params, none in this case		
		runCode(prog, bytecode, commandLineArgs);	
		show("Log:\n"+RuntimeLog.globalLog);
		assertEquals("entering main;3;leaving main;",RuntimeLog.globalLog.toString());
	}
	
	@Test
	public void createNonDefaultSizeImage() throws Exception {
		String input = "prog{image y[512,256];\n  show y;} ";
		byte[] bytecode = genCode(input);
		
	}
	
	@Test
	public void testExpressionPredefinedName() throws Exception {
		String input = "prog{show Z; show default_width; show default_height;}";
		byte[] bytecode = genCode(input);
		
	}
	
	@Test
	public void readFromCommandLine() throws Exception {
		String input = "prog{int x; input x from @ 0 ; show x;\nfloat y; input y from @ 1; show y;\nboolean z; input z from @ 2; show z;\ninput z from @ 3; show z;}";
		byte[] bytecode = genCode(input);
		String[] commandLineArgs = {};
		runCode(input, bytecode, commandLineArgs);
		show("Log:\n"+RuntimeLog.globalLog);
	}
	
	@Test
	public void readImageFromCommandLine() throws Exception {
		String input = "prog{image y; input y from @ 0 ; show y;} ";
		byte[] bytecode = genCode(input);
		
	}
	
	@Test
	public void testBinary1() throws Exception {
		String input = "prog{show 9 + 4;show 9 - 4;show 9 * 4;show 9 / 4;show 9 ** 4;show 9 % 4;show 9 & 4;show 9 | 4;}";
		byte[] bytecode = genCode(input);
		
	}
	
	@Test
	public void testBinary2() throws Exception {
		String input = "prog{show 9.1 + 4.5;show 9.1 - 4.5;show 9.1 * 4.5;show 8.82 / 4.2;show 9.1 ** 4.1;}";
		byte[] bytecode = genCode(input);
	}
	
	
	

}