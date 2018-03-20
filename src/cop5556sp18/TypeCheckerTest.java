package cop5556sp18;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import cop5556sp18.Parser;
import cop5556sp18.Scanner;
import cop5556sp18.AST.ASTVisitor;
import cop5556sp18.AST.Program;
import cop5556sp18.TypeChecker.SemanticException;

public class TypeCheckerTest {

	/*
	 * set Junit to be able to catch exceptions
	 */
	@Rule
	public ExpectedException thrown = ExpectedException.none();

	/**
	 * Prints objects in a way that is easy to turn on and off
	 */
	static final boolean doPrint = true;

	private void show(Object input) {
		if (doPrint) {
			System.out.println(input.toString());
		}
	}

	/**
	 * Scans, parses, and type checks the input string
	 * 
	 * @param input
	 * @throws Exception
	 */
	void typeCheck(String input) throws Exception {
		show(input);
		// instantiate a Scanner and scan input
		Scanner scanner = new Scanner(input).scan();
		show(scanner);
		// instantiate a Parser and parse input to obtain and AST
		Program ast = new Parser(scanner).parse();
		show(ast);
		// instantiate a TypeChecker and visit the ast to perform type checking and
		// decorate the AST.
		ASTVisitor v = new TypeChecker();
		ast.visit(v, null);
	}



	/**
	 * Simple test case with an almost empty program.
	 * 
	 * @throws Exception
	 */
	@Test
	public void emptyProg() throws Exception {
		String input = "emptyProg{}";
		typeCheck(input);
	}

	@Test
	public void expression1() throws Exception {
		String input = "prog {show 3+4;}";
		typeCheck(input);
	}

	@Test
	public void expression2_fail() throws Exception {
		String input = "prog { show true+4; }"; //error, incompatible types in binary expression
		thrown.expect(SemanticException.class);
		try {
			typeCheck(input);
		} catch (SemanticException e) {
			show(e);
			throw e;
		}
	}
	
	
	
	//Tests
	@Test
    public void simpleImage() throws Exception {
        String input = "X{ image im[1,2]; }";
        typeCheck(input);
    }
    
    @Test
    public void simpleImageFail() throws Exception {
        String input = "X{ image im[1.0, 2]; }";
        thrown.expect(SemanticException.class);
        try {
            typeCheck(input);
        } catch (SemanticException e) {
            show(e);
            throw e;
        }
    }
    
    @Test
    public void nestedDec1() throws Exception {
        String input = "X{ int x; int y; while (x == y) {int x;}; }";
        typeCheck(input);
    }
    
    @Test
    public void nestedDec2() throws Exception {
        String input = "X{ int x; int z; while (x == y) {int x;}; }";
        thrown.expect(SemanticException.class);
        try {
            typeCheck(input);
        } catch (SemanticException e) {
            show(e);
            throw e;
        }
    }
    
    @Test
        public void nestedDec3() throws Exception {
            String input = "X{ int x; int y; while (x == y) { show x;}; }";
            typeCheck(input);
    }
    
    @Test
    public void nestedDec4() throws Exception {
        String input = "X{ int x; int y; while (x == y) { int z;}; show z;}";
        thrown.expect(SemanticException.class);
        try { typeCheck(input); } catch (SemanticException e) { show(e); throw e; }
    }

	
}
