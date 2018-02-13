 /**
 * JUunit tests for the Scanner for the class project in COP5556 Programming Language Principles 
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp18.Scanner.LexicalException;
import cop5556sp18.Scanner.Token;
import static cop5556sp18.Scanner.Kind.*;

public class ScannerTest {

	//set Junit to be able to catch exceptions
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	
	//To make it easy to print objects and turn this output on and off
	static boolean doPrint = true;
	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}

	/**
	 *Retrieves the next token and checks that it is an EOF token. 
	 *Also checks that this was the last token.
	 *
	 * @param scanner
	 * @return the Token that was retrieved
	 */
	
	Token checkNextIsEOF(Scanner scanner) {
		Scanner.Token token = scanner.nextToken();
		assertEquals(Scanner.Kind.EOF, token.kind);
		assertFalse(scanner.hasTokens());
		return token;
	}


	/**
	 * Retrieves the next token and checks that its kind, position, length, line, and position in line
	 * match the given parameters.
	 * 
	 * @param scanner
	 * @param kind
	 * @param pos
	 * @param length
	 * @param line
	 * @param pos_in_line
	 * @return  the Token that was retrieved
	 */
	Token checkNext(Scanner scanner, Scanner.Kind kind, int pos, int length, int line, int pos_in_line) {
		Token t = scanner.nextToken();
		assertEquals(kind, t.kind);
		assertEquals(pos, t.pos);
		assertEquals(length, t.length);
		assertEquals(line, t.line());
		assertEquals(pos_in_line, t.posInLine());
		return t;
	}

	/**
	 * Retrieves the next token and checks that its kind and length match the given
	 * parameters.  The position, line, and position in line are ignored.
	 * 
	 * @param scanner
	 * @param kind
	 * @param length
	 * @return  the Token that was retrieved
	 */
	Token checkNext(Scanner scanner, Scanner.Kind kind, int length) {
		Token t = scanner.nextToken();
		assertEquals(kind, t.kind);
		assertEquals(length, t.length);
		return t;
	}
	


	/**
	 * Simple test case with an empty program.  The only Token will be the EOF Token.
	 *   
	 * @throws LexicalException
	 */
	@Test
	public void testEmpty() throws LexicalException {
		String input = "";  //The input is the empty string.  This is legal
		show(input);        //Display the input 
		Scanner scanner = new Scanner(input).scan();  //Create a Scanner and initialize it
		show(scanner);   //Display the Scanner
		checkNextIsEOF(scanner);  //Check that the only token is the EOF token.
	}
	
	/**
	 * Test illustrating how to put a new line in the input program and how to
	 * check content of tokens.
	 * 
	 * Because we are using a Java String literal for input, we use \n for the
	 * end of line character. (We should also be able to handle \n, \r, and \r\n
	 * properly.)
	 * 
	 * Note that if we were reading the input from a file, the end of line 
	 * character would be inserted by the text editor.
	 * Showing the input will let you check your input is 
	 * what you think it is.
	 * 
	 * @throws LexicalException
	 */
	@Test
	public void testSemi() throws LexicalException {
		String input = ";;\n;;";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, SEMI, 0, 1, 1, 1);
		checkNext(scanner, SEMI, 1, 1, 1, 2);
		checkNext(scanner, SEMI, 3, 1, 2, 1);
		checkNext(scanner, SEMI, 4, 1, 2, 2);
		checkNextIsEOF(scanner);
	}
	

	
	/**
	 * This example shows how to test that your scanner is behaving when the
	 * input is illegal.  In this case, we are giving it an illegal character '~' in position 2
	 * 
	 * The example shows catching the exception that is thrown by the scanner,
	 * looking at it, and checking its contents before rethrowing it.  If caught
	 * but not rethrown, then JUnit won't get the exception and the test will fail.  
	 * 
	 * The test will work without putting the try-catch block around 
	 * new Scanner(input).scan(); but then you won't be able to check 
	 * or display the thrown exception.
	 * 
	 * @throws LexicalException
	 */
	@Test
	public void failIllegalChar() throws LexicalException {
		String input = ";;~";
		show(input);
		thrown.expect(LexicalException.class);  //Tell JUnit to expect a LexicalException
		try {
			new Scanner(input).scan();
		} catch (LexicalException e) {  //Catch the exception
			show(e);                    //Display it
			assertEquals(2,e.getPos()); //Check that it occurred in the expected position
			throw e;                    //Rethrow exception so JUnit will see it
		}
	}




	@Test
	public void testParens() throws LexicalException {
		String input = "()";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, LPAREN, 0, 1, 1, 1);
		checkNext(scanner, RPAREN, 1, 1, 1, 2);
		checkNextIsEOF(scanner);
	}
	
	@Test
	public void tempTest() throws LexicalException {
		String input = ">===";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		
		
	}
	
	@Test
	public void testKeyWords() throws LexicalException {
		String input = "default_width height show write to input from cart_y polar_a abs atan log image int float boolean green alpha while if";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, KW_default_width,0,13,1,1);
		checkNext(scanner, KW_height,14,6,1,15);
		checkNext(scanner, KW_show,21,4,1,22);
		checkNext(scanner, KW_write,26,5,1,27);
		checkNext(scanner, KW_to,32,2,1,33);
		checkNext(scanner, KW_input,35,5,1,36);
		checkNext(scanner, KW_from,41,4,1,42);
		checkNext(scanner, KW_cart_y,46,6,1,47);
		checkNext(scanner, KW_polar_a,53,7,1,54);
		checkNext(scanner, KW_abs,61,3,1,62);
		checkNext(scanner, KW_atan,65,4,1,66);
		checkNext(scanner, KW_log,70,3,1,71);
		checkNext(scanner, KW_image,74,5,1,75);
		checkNext(scanner, KW_int,80,3,1,81);
		checkNext(scanner, KW_float,84,5,1,85);
		checkNext(scanner, KW_boolean,90,7,1,91);
		checkNext(scanner, KW_green,98,5,1,99);
		checkNext(scanner, KW_alpha,104,5,1,105);
		checkNext(scanner, KW_while,110,5,1,111);
		checkNext(scanner, KW_if,116,2,1,117);
		checkNextIsEOF(scanner);
	}
	
	@Test
	public void testBoolean() throws LexicalException {
		String input = "true \nfalse";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, BOOLEAN_LITERAL, 0, 4, 1, 1);
		checkNext(scanner, BOOLEAN_LITERAL, 6, 5, 2, 1);
		checkNextIsEOF(scanner);
	}
	
	@Test
	public void testZeros() throws LexicalException {
		String input = "0 00";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, INTEGER_LITERAL, 0, 1, 1, 1);
		checkNext(scanner, INTEGER_LITERAL, 2, 1, 1, 3);
		checkNext(scanner, INTEGER_LITERAL, 3, 1, 1, 4);
		checkNextIsEOF(scanner);
	}
	
	@Test
	public void testFloat() throws LexicalException {
		String input = "1.23 \n0.5 \n20.62 \n10.008 \n8.500 \n7. \n.9";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, FLOAT_LITERAL, 0, 4, 1, 1);
		checkNext(scanner, FLOAT_LITERAL, 6, 3, 2, 1);
		checkNext(scanner, FLOAT_LITERAL, 11, 5, 3, 1);
		checkNext(scanner, FLOAT_LITERAL, 18, 6, 4, 1);
		checkNext(scanner, FLOAT_LITERAL, 26, 5, 5, 1);
		checkNext(scanner, FLOAT_LITERAL, 33, 2, 6, 1);
		checkNext(scanner, FLOAT_LITERAL, 37, 2, 7, 1);
		checkNextIsEOF(scanner);
	}
	
	@Test
	public void testComplexFloat() throws LexicalException {
		String input = "3.5.2 \n.666. \n789..25";
		Scanner scanner = new Scanner(input).scan();
		show(input);
		show(scanner);
		checkNext(scanner, FLOAT_LITERAL, 0, 3, 1, 1);
		checkNext(scanner, FLOAT_LITERAL, 3, 2, 1, 4);
		checkNext(scanner, FLOAT_LITERAL, 7, 4, 2, 1);
		checkNext(scanner, DOT, 11, 1, 2, 5);
		checkNext(scanner, FLOAT_LITERAL, 14, 4, 3, 1);
		checkNext(scanner, FLOAT_LITERAL, 18, 3, 3, 5);
		checkNextIsEOF(scanner);
	}
	
	

	
}
	

