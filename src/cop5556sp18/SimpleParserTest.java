/**
* JUunit tests for the Parser for the class project in COP5556 Programming Language Principles 
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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp18.Parser;
import cop5556sp18.Scanner;
import cop5556sp18.Parser.SyntaxException;
import cop5556sp18.Scanner.LexicalException;

public class SimpleParserTest {

	// set Junit to be able to catch exceptions
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	// To make it easy to print objects and turn this output on and off
	static final boolean doPrint = true;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}

	// creates and returns a parser for the given input.
	private Parser makeParser(String input) throws LexicalException {
		show(input); // Display the input
		Scanner scanner = new Scanner(input).scan(); // Create a Scanner and initialize it
		show(scanner); // Display the Scanner
		Parser parser = new Parser(scanner);
		return parser;
	}

	/**
	 * Simple test case with an empty program. This throws an exception because it
	 * lacks an identifier and a block. The test case passes because it expects an
	 * exception
	 * 
	 * @throws LexicalException
	 * @throws SyntaxException
	 */
	@Test
	public void testEmpty() throws LexicalException, SyntaxException {
		String input = ""; // The input is the empty string.
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}

	/**
	 * Smallest legal program.
	 * 
	 * @throws LexicalException
	 * @throws SyntaxException
	 */
	@Test
	public void testSmallest() throws LexicalException, SyntaxException {
		String input = "b{}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	// This test should pass in your complete parser. It will fail in the starter
	// code.
	// Of course, you would want a better error message.
	@Test
	public void testDec0() throws LexicalException, SyntaxException {
		String input = "b{int c;}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	/*
	 * Statement Tests
	 */
	@Test
	public void testInputStatement() throws LexicalException, SyntaxException {
		String input = "input foo from @bar";

		Parser parser = makeParser(input);
		parser.statement();
	}

	@Test
	public void testAssignStatement1() throws LexicalException, SyntaxException {
		String input = "x:=false;";

		Parser parser = makeParser(input);
		parser.statement();
	}

	@Test
	public void testAssignStatement2() throws LexicalException, SyntaxException {
		String input = "x [foo,bar] := sin(30);";

		Parser parser = makeParser(input);
		parser.statement();
	}

	@Test
	public void testWriteStatement() throws LexicalException, SyntaxException {
		String input = "write foo to bar}";

		Parser parser = makeParser(input);
		parser.statement();
	}

	@Test
	public void testWhileStatement() throws LexicalException, SyntaxException {
		String input = "while(foo > bar) {}";

		Parser parser = makeParser(input);
		parser.statement();
	}

	@Test
	public void testPixelSelector() throws LexicalException, SyntaxException {
		String input = "[5>2 ? 4+7 : 5-2, 6*9]";

		Parser parser = makeParser(input);
		parser.pixelSelector();
	}
	
	@Test
	public void testFuncApp() throws LexicalException, SyntaxException {
		String input = "prog{show sin(1); show cos(1); show atan(1); show abs(1);show log(1);}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}
	
	
	@Test
	public void testFuncApp2() throws LexicalException, SyntaxException {
		String input = "prog{show sin[1,2]; show cos[1,2]; show atan[1,2]; show abs[1,2];show log[1,2];}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}
	
	
	/*
	 * Integration tests
	 */

	@Test
	public void testParser1() throws LexicalException, SyntaxException {
		String input = "demo1{image h;input h from @0;show h; sleep(4000); image g[width(h),height(h)];int x;x:=0;"
				+ "while(x<width(g)){int y;y:=0;while(y<height(g)){g[x,y]:=h[y,x];y:=y+1;};x:=x+1;};show g;sleep(4000);}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	@Test
	public void testParser2() throws LexicalException, SyntaxException {
		String input = "paintBlue{image im[256,256];int x;int y;x:=0;y:=0;while(x<width(im)) {y:=0;while(y<height(im)) {im[x,y]:=<<255,255,0,0>>;y:=y+1;};x:=x+1;};show im;}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	@Test
	public void testParser3() throws LexicalException, SyntaxException {
		String input = "PolarR2{image im[1024,1024];int x;x:=0;while(x<width(im)) {int y;y:=0;while(y<height(im)) {float p;p:=polar_r[x,y];int r;r:=int(p)%Z;im[x,y]:=<<Z,0,0,r>>;y:=y+1;};x:=x+1;};show im;}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	@Test
	public void testParser4() throws LexicalException, SyntaxException {
		String input = "moo{image cow; input cow from @farm;show cow;sleep(4000);image buffalo[width(cow),height(cow)];int x;x:=0;while(x<width(buffalo)) {int y;y:=0;while(y<height(buffalo)) {blue(buffalo[x,y]):=red(cow[x,y]);green(buffalo[x,y]):=blue(cow[x,y]);red(cow[x,y]):=green(cow[x,y]);alpha(buffalo[x,y]):=Z;y:=y+1;};x:=x+1;};show buffalo;sleep(4000);}";
		Parser parser = makeParser(input);
		parser.parse();
	}

	/*
	 * Negative Test cases
	 */
	@Test
	public void testInvalidAndExpression() throws LexicalException, SyntaxException {
		String input = "prog{if(a & ){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}
	
	
	@Test
	public void testInvalidAndExpression2() throws LexicalException, SyntaxException {
		String input = "prog{if(a && b){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}
	
	@Test
	public void testInvalidOrExpression() throws LexicalException, SyntaxException {
		String input = "prog{if(a | b |){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}

	@Test
	public void testInvalidOrExpression2() throws LexicalException, SyntaxException {
		String input = "prog{if(a | b || c){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}

	@Test
	public void testInvalidEqualsExpression() throws LexicalException, SyntaxException {
		String input = "prog{if(a==){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}

	@Test
	public void testInvalidNotEqualsExpression() throws LexicalException, SyntaxException {
		String input = "prog{if(a!=){};}";
		Parser parser = makeParser(input);
		thrown.expect(SyntaxException.class);
		parser.parse();
	}
	
	
	
}
