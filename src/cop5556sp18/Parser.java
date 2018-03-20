package cop5556sp18;
/* *
 * Initial code for SimpleParser for the class project in COP5556 Programming Language Principles 
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

import cop5556sp18.Scanner.Token;
import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.Block;
import cop5556sp18.AST.Declaration;
import cop5556sp18.AST.Expression;
import cop5556sp18.AST.ExpressionBinary;
import cop5556sp18.AST.ExpressionBooleanLiteral;
import cop5556sp18.AST.ExpressionConditional;
import cop5556sp18.AST.ExpressionFloatLiteral;
import cop5556sp18.AST.ExpressionFunctionAppWithExpressionArg;
import cop5556sp18.AST.ExpressionFunctionAppWithPixel;
import cop5556sp18.AST.ExpressionIdent;
import cop5556sp18.AST.ExpressionIntegerLiteral;
import cop5556sp18.AST.ExpressionPixel;
import cop5556sp18.AST.ExpressionPixelConstructor;
import cop5556sp18.AST.ExpressionPredefinedName;
import cop5556sp18.AST.ExpressionUnary;
import cop5556sp18.AST.LHS;
import cop5556sp18.AST.LHSIdent;
import cop5556sp18.AST.LHSPixel;
import cop5556sp18.AST.LHSSample;
import cop5556sp18.AST.PixelSelector;
import cop5556sp18.AST.Program;
import cop5556sp18.AST.Statement;
import cop5556sp18.AST.StatementAssign;
import cop5556sp18.AST.StatementIf;
import cop5556sp18.AST.StatementInput;
import cop5556sp18.AST.StatementShow;
import cop5556sp18.AST.StatementSleep;
import cop5556sp18.AST.StatementWhile;
import cop5556sp18.AST.StatementWrite;
import cop5556sp18.Scanner.Kind;
import static cop5556sp18.Scanner.Kind.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Parser {

	@SuppressWarnings("serial")
	public static class SyntaxException extends Exception {
		Token t;

		public SyntaxException(Token t, String message) {
			super(message);
			this.t = t;
		}
	}

	Scanner scanner;
	Token t;

	Parser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	public Program parse() throws SyntaxException {
		Program p = program();
		matchEOF();
		return p;
	}

	/*
	 * Program ::= Identifier Block
	 */
	public Program program() throws SyntaxException {
		Token firstToken = t;
		Token progName = match(IDENTIFIER);
		Block block = block();
		return new Program(firstToken, progName, block);
	}

	Kind[] firstDec = { KW_int, KW_boolean, KW_image, KW_float, KW_filename };

	Kind[] firstStatement = { KW_input, KW_write, KW_while, KW_if, KW_show, KW_sleep, IDENTIFIER, KW_red, KW_green,
			KW_blue, KW_alpha };

	Kind[] firstExpression = { OP_PLUS, OP_MINUS, OP_EXCLAMATION, INTEGER_LITERAL, BOOLEAN_LITERAL, FLOAT_LITERAL,
			LPAREN, IDENTIFIER, LPIXEL, KW_abs, KW_sin, KW_cos, KW_atan, KW_log, KW_cart_x, KW_cart_y, KW_polar_a,
			KW_polar_r, KW_int, KW_float, KW_width, KW_height, KW_red, KW_green, KW_blue, KW_alpha, KW_Z,
			KW_default_width, KW_default_height };

	Kind[] firstFunctionName = { KW_sin, KW_cos, KW_atan, KW_log, KW_cart_x, KW_cart_y, KW_polar_a, KW_polar_r, KW_int,
			KW_float, KW_width, KW_height, KW_red, KW_green, KW_blue, KW_alpha };

	Kind[] firstPredefinedname = { KW_Z, KW_default_width, KW_default_height };

	Kind[] firstColor = { KW_red, KW_green, KW_blue, KW_alpha };

	/*
	 * Block ::= { ( (Declaration | Statement) ; )* }
	 */
	public Block block() throws SyntaxException {
		Token firstToken = t;
		match(LBRACE);
		List<ASTNode> decsOrStatements = new ArrayList<ASTNode>();
		while (isKind(firstDec) | isKind(firstStatement)) {
			if (isKind(firstDec)) {
				Declaration dec = declaration();
				decsOrStatements.add((ASTNode) dec);
			} else if (isKind(firstStatement)) {
				Statement stat = statement();
				decsOrStatements.add((ASTNode) stat);
			}
			match(SEMI);
		}
		match(RBRACE);
		
		return new Block(firstToken, decsOrStatements);

	}

	/*
	 * Declaration ::= image Identifier ( epsilon | [Expression, Expression]) | Type
	 * Identifier
	 */
	public Declaration declaration() throws SyntaxException {
		Token firstToken = t;
		Token type;
		Token identifier;
		Expression width  = null;
		Expression height = null;
		
		if (isKind(Kind.KW_image)) {
			type = match(KW_image);
			identifier = match(IDENTIFIER);

			if (isKind(LSQUARE)) {
				match(LSQUARE);
				width = safeExpression();
				match(COMMA);
				height = safeExpression();
				match(RSQUARE);
				
				//TODO can width, height be null for an image ?
				if (null == width || null ==height) {
					throw new SyntaxException(identifier, "width and height can't be null for image");
				}
			}
		} else {
			switch (t.kind) {
			case KW_int:
				type = match(KW_int);
				break;
			case KW_float:
				type = match(KW_float);
				break;
			case KW_boolean:
				type = match(KW_boolean);
				break;
			case KW_filename:
				type = match(KW_filename);
				break;
			default:
				throw new SyntaxException(t, "Illegal token at the start of declaration \n Error at " + t.pos);
			}
			identifier = match(IDENTIFIER);
		}
		
		return new Declaration(firstToken, type, identifier, width, height);

	}

	/*
	 * Statement ::= StatementInput | StatementWrite | StatementAssignment |
	 * StatementWhile | StatementIf | StatementShow | StatementSleep
	 */
	public Statement statement() throws SyntaxException {
		Token firstToken = t;
		
		switch (t.kind) {

		// StatementInput ::= ​input IDENTIFIER from @ ​Expression
		case KW_input: {
			match(KW_input);
			Token destName = match(IDENTIFIER);
			match(KW_from);
			match(OP_AT);
			Expression exp = safeExpression();
			
			return new StatementInput(firstToken, destName, exp);
		}
			

		// StatementWrite ::= ​write IDENTIFIER to IDENTIFIER
		case KW_write: {
			match(KW_write);
			Token source = match(IDENTIFIER);
			match(KW_to);
			Token dest = match(IDENTIFIER);
			
			
			return new StatementWrite(firstToken, source, dest);
		}
			

		// StatementAssignment ::= LHS ​:=​ Expression
		// LHS ::= ​IDENTIFIER​ (epsilon | ​PixelSelector) | Color ​(​ ​IDENTIFIER PixelSelector ​)​
		case IDENTIFIER:
		case KW_red:
		case KW_green:
		case KW_blue:
		case KW_alpha: {

			// Match LHS
			LHS lhs = lhs();
			match(OP_ASSIGN);
			Expression exp = safeExpression();
			
			return new StatementAssign(firstToken, lhs, exp);
		}
			

		// StatementWhile ::= ​while (​Expression ​)​ Block
		case KW_while: {
			match(KW_while);
			match(LPAREN);
			Expression whileGuardExp = safeExpression();
			match(RPAREN);
			Block block = block();
			
			return new StatementWhile(firstToken, whileGuardExp, block);
		}
			

		// StatementIf ::= ​if (​ Expression ​)​ Block
		case KW_if: {
			match(KW_if);
			match(LPAREN);
			Expression ifGuardExp = safeExpression();
			match(RPAREN);
			Block block = block();
			
			return new StatementIf(firstToken, ifGuardExp, block);
		}
			

		// StatementShow ::= ​show​ Expression
		case KW_show: {
			match(KW_show);
			Expression exp = safeExpression();
			
			return new StatementShow(firstToken, exp);
		}
			

		// StatementSleep ::= ​sleep​ Expression
		case KW_sleep: {
			match(KW_sleep);
			Expression sleepDuration = safeExpression();
			
			return new StatementSleep(firstToken, sleepDuration);
		}
		
		
		default:
			throw new SyntaxException(t, "Illegal token at the start of statement \n Error at " + t.pos);
		}
	}

	public Expression safeExpression() throws SyntaxException {
		Expression exp;
		if (isKind(firstExpression)) {
			exp = expression();
		} else {
			throw new SyntaxException(t, "Illegal token at the start of expression \n Error at " + t.pos);
		}
		return exp;
	}

	/*
	 * LHS ::= ​IDENTIFIER​ (epsilon | ​PixelSelector) | Color ​(​ ​IDENTIFIER​ PixelSelector ​)
	 */
	public LHS lhs() throws SyntaxException {
		Token firstToken = t;
		Token identifier;
		PixelSelector pixel;
		Token colorToken;
		
		if (isKind(IDENTIFIER)) {
			identifier = match(IDENTIFIER);

			// PixelSelector ::= ​[ ​Expression ​,​ Expression ​]
			if (isKind(LSQUARE)) {
				pixel = pixelSelector();
				
				return new LHSPixel(firstToken, identifier, pixel);
			} else {
				return new LHSIdent(firstToken, identifier);
			}
		} else {
			colorToken = color();
			match(LPAREN);
			identifier = match(IDENTIFIER);
			pixel = pixelSelector();
			match(RPAREN);
			
			return new LHSSample(firstToken, identifier, pixel, colorToken);
		}
	}

	/*
	 * Expression ::= OrExpression(epsilon | ​?​ Expression ​:​ Expression)
	 */
	public Expression expression() throws SyntaxException {
		Token firstToken = t;
		Expression trueExp, falseExp;
		Expression exp = orExpression();
		

		if (isKind(OP_QUESTION)) {
			match(OP_QUESTION);
			trueExp = safeExpression();
			match(OP_COLON);
			falseExp = safeExpression();
			
			return new ExpressionConditional(firstToken, exp, trueExp, falseExp);
		}
		
		return exp;
	}

	/*
	 * OrExpression ::= AndExpression ( ​|​ AndExpression ) *
	 */
	public Expression orExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = andExpression();

		while (isKind(OP_OR)) {
			Token op = match(OP_OR);
			Expression exp2 = andExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * AndExpression ::= EqExpression ( ​&​ EqExpression )*
	 */
	public Expression andExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = eqExpression();

		while (isKind(OP_AND)) {
			Token op = match(OP_AND);
			Expression exp2 = eqExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * EqExpression ::= RelExpression ( (​==​ | ​!=​ ) RelExpression )*
	 */
	public Expression eqExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = relExpression();
		Token op = null ;

		while (isKind(OP_EQ) | isKind(OP_NEQ)) {
			if (t.kind == OP_EQ) {
				op = match(OP_EQ);
			} else if (t.kind == OP_NEQ) {
				op = match(OP_NEQ);
			}
			Expression exp2 = relExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * RelExpression ::= AddExpression ( (​<​ | ​>​ | ​<=​ | ​>=​ ) AddExpression)*
	 */
	public Expression relExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = addExpression();
		Token op = null;

		while (isKind(OP_GE) || isKind(OP_LE) || isKind(OP_GT) || isKind(OP_LT)) {
			switch (t.kind) {
			case OP_GE:
				op = match(OP_GE);
				break;
			case OP_LE:
				op = match(OP_LE);
				break;
			case OP_GT:
				op = match(OP_GT);
				break;
			case OP_LT:
				op = match(OP_LT);
				break;
			default :
				throw new SyntaxException(t, "Illegal token in relational expression \n Error at " + t.pos);
			}
			Expression exp2 = andExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * AddExpression ::= MultExpression ( ( ​+​ | ​-​ ) MultExpression )*
	 */
	public Expression addExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = multExpression();
		Token op = null;

		while (isKind(OP_PLUS) | isKind(OP_MINUS)) {
			if (t.kind == OP_PLUS) {
				op = match(OP_PLUS);
			} else if (t.kind == OP_MINUS) {
				op = match(OP_MINUS);
			}
			Expression exp2 = multExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * MultExpression := PowerExpression ( ( ​*​ | ​/​ | ​%​ ) PowerExpression )*
	 */
	public Expression multExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = powerExpression();
		Token op = null;

		while (isKind(OP_TIMES) || isKind(OP_DIV) || isKind(OP_MOD)) {
			switch (t.kind) {
			case OP_TIMES:
				op = match(OP_TIMES);
				break;
			case OP_DIV:
				op = match(OP_DIV);
				break;
			case OP_MOD:
				op = match(OP_MOD);
				break;
			}
			Expression exp2 = powerExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * PowerExpression := UnaryExpression (​**​ PowerExpression | ε)
	 */
	public Expression powerExpression() throws SyntaxException {
		Token firstToken = t;
		Expression exp1 = unaryExpression();

		if (isKind(OP_POWER)) {
			Token op = match(OP_POWER);
			Expression exp2 = powerExpression();
			exp1 = new ExpressionBinary(firstToken, exp1, op, exp2);
		}
		return exp1;
	}

	/*
	 * UnaryExpression ::= ​+​ UnaryExpression | ​-​ UnaryExpression |
	 * UnaryExpressionNotPlusMinus
	 */
	public Expression unaryExpression() throws SyntaxException {
		Token firstToken = t;
		Token op;
		Expression exp1;
		
		if (isKind(OP_PLUS)) {
			op = match(OP_PLUS);
			exp1 = unaryExpression();
			return new ExpressionUnary(firstToken, op, exp1);
		} else if (isKind(OP_MINUS)) {
			op = match(OP_MINUS);
			exp1 = unaryExpression();
			return new ExpressionUnary(firstToken, op, exp1);
		} else {
			exp1 = unaryExpressionNotPlusMinus();
			return exp1;
		}
	}

	/*
	 * UnaryExpressionNotPlusMinus ::= ​!​ UnaryExpression | Primary
	 */
	public Expression unaryExpressionNotPlusMinus() throws SyntaxException {
		Token firstToken = t;
		
		if (isKind(OP_EXCLAMATION)) {
			Token op = match(OP_EXCLAMATION);
			Expression unExp = unaryExpression();
			return new ExpressionUnary(firstToken, op, unExp);
		} else {
			Expression exp = primary();
			return exp;
		}
	}

	/*
	 * Primary ::= ​INTEGER_LITERAL ​| ​BOOLEAN_LITERAL ​| ​FLOAT_LITERAL ​|​
	 * ( Expression​ ) ​| FunctionApplication |​ IDENTIFIER (epsilon ​| PixelSelector)
	 * | PredefinedName | PixelConstructor
	 */
	public Expression primary() throws SyntaxException {
		Token firstToken = t;
		
		switch (t.kind) {

		case INTEGER_LITERAL:
			Token intLiteral = match(INTEGER_LITERAL);
			return new ExpressionIntegerLiteral(firstToken, intLiteral);

		case BOOLEAN_LITERAL:
			Token boolLiteral = match(BOOLEAN_LITERAL);
			return new ExpressionBooleanLiteral(firstToken, boolLiteral);

		case FLOAT_LITERAL:
			Token floatLiteral = match(FLOAT_LITERAL);
			return new ExpressionFloatLiteral(firstToken, floatLiteral);

		// Expression
		case LPAREN: 
			match(LPAREN);
			Expression exp = safeExpression();
			match(RPAREN);
		
			return exp;
		
		// PixelConstructor
		case LPIXEL:
			Expression[] exps = pixelConstructor();
			return new ExpressionPixelConstructor(firstToken, exps[0], exps[1], exps[2], exps[3]);

		// IDENTIFIER (epsilon ​| PixelSelector)
		case IDENTIFIER: {
			Token identifier = match(IDENTIFIER);
			if (isKind(LSQUARE)) {
				PixelSelector ps = pixelSelector();
				return new ExpressionPixel(firstToken, identifier, ps);
			} else {
				return new ExpressionIdent(firstToken, identifier);
			}
		}
			

		/*
		 * FunctionApplication, PredefinedName 
		 * FunctionApplication ::= FunctionName​ ( (Expression ​)​ | ​[​ Expression ​,​ Expression ​] )
		 */
		default: {
			if (isKind(firstFunctionName)) {
				Expression funcAppExp = functionApplication();
				return funcAppExp;
			} else if (isKind(firstPredefinedname)) {
				Expression preDefName = predefinedName();
				return preDefName;
			} else {
				//Handles expressions that don't end properly
				throw new SyntaxException(t, "Syntax Error. Illegal Expression");
			}
		}

		} // end switch

	}

	/*
	 * FunctionApplication ::= FunctionName​ ( (​ Expression ​)​ | ​[​ Expression
	 * ​,​ Expression ​] )
	 */
	public Expression functionApplication() throws SyntaxException {
		Token firstToken =t;
		Token funcNameToken = functionName();
		if (isKind(LPAREN)) {
			match(LPAREN);
			Expression exp = safeExpression();
			match(RPAREN);
			
			return new ExpressionFunctionAppWithExpressionArg(firstToken, funcNameToken, exp);
		} else if (isKind(LSQUARE)) {
			match(LSQUARE);
			Expression pixExp1 = safeExpression();
			match(COMMA);
			Expression pixExp2 = safeExpression();
			match(RSQUARE);
			
			return new ExpressionFunctionAppWithPixel(firstToken, funcNameToken, pixExp1, pixExp2);
		} else {
			throw new SyntaxException(t, "Illegal value followed by FunctionName \n Expected a '(' or '[' ");
		}
	}

	/*
	 * FunctionName ::= ​sin​ | ​cos​ | ​atan​ | ​abs​ | ​log​ | ​cart_x ​| ​cart_y
	 * ​| ​ polar_a ​| ​polar_r | int​ | ​float ​| ​width​ | ​height ​| Color
	 */
	public Token functionName() throws SyntaxException {
		Token functionName = null;
		switch (t.kind) {
		case KW_sin:
			functionName = match(KW_sin);
			break;
		case KW_cos:
			functionName = match(KW_cos);
			break;
		case KW_atan:
			functionName = match(KW_atan);
			break;
		case KW_abs:
			functionName = match(KW_abs);
			break;
		case KW_log:
			functionName = match(KW_log);
			break;
		case KW_cart_x:
			functionName = match(KW_cart_x);
			break;
		case KW_cart_y:
			functionName = match(KW_cart_y);
			break;
		case KW_polar_a:
			functionName = match(KW_polar_a);
			break;
		case KW_polar_r:
			functionName = match(KW_polar_r);
			break;
		case KW_int:
			functionName = match(KW_int);
			break;
		case KW_float:
			functionName = match(KW_float);
			break;
		case KW_width:
			functionName = match(KW_width);
			break;
		case KW_height:
			functionName = match(KW_height);
			break;
		default: {
			if (isKind(firstColor)) {
				functionName = color();
			}
		}

		}
		return functionName;
	}

	/*
	 * PredefinedName ::= ​Z ​| ​default_height ​| ​default_width
	 */
	public ExpressionPredefinedName predefinedName() throws SyntaxException {
		Token firstToken = t;
		Token preDefName = null;
		
		switch (t.kind) {
		case KW_Z:
			preDefName = match(KW_Z);
			break;
		case KW_default_height:
			preDefName = match(Kind.KW_default_height);
			break;
		case KW_default_width:
			preDefName = match(Kind.KW_default_width);
			break;
		}
		return new ExpressionPredefinedName(firstToken, preDefName);
	}

	/*
	 * PixelConstructor ::= ​<< ​ Expression​ , ​Expression ​,​ Expression ​,​
	 * Expression ​>>
	 */
	public Expression[] pixelConstructor() throws SyntaxException {
		Expression[] expList = new Expression[4] ;
		
		match(LPIXEL);
		expList[0] = safeExpression();
		match(COMMA);
		expList[1] = safeExpression();
		match(COMMA);
		expList[2] = safeExpression();
		match(COMMA);
		expList[3] = safeExpression();
		match(RPIXEL);
		
		return expList;

	}

	/*
	 * PixelSelector ::= ​[ ​Expression ​,​ Expression ​]
	 */
	public PixelSelector pixelSelector() throws SyntaxException {
		Token firstToken = t;
		Expression exp1, exp2;
		match(LSQUARE);
		exp1 = safeExpression();
		match(COMMA);
		exp2 = safeExpression();
		match(RSQUARE);
		
		return new PixelSelector(firstToken, exp1, exp2);
	}

	/*
	 * Color ::= ​red​ | ​green​ | ​blue​ | ​alpha
	 */
	public Token color() throws SyntaxException {
		Token colortoken = null;
		switch (t.kind) {
		case KW_red:
			colortoken = match(KW_red);
			break;
		case KW_green:
			colortoken = match(KW_green);
			break;
		case KW_blue:
			colortoken = match(KW_blue);
			break;
		case KW_alpha:
			colortoken = match(KW_alpha);
			break;
		default:
			throw new SyntaxException(t, "Illegal value passed for color. \n Expected values are red, green, blue, alpha");
		}
		return colortoken;
	}

	protected boolean isKind(Kind kind) {
		return t.kind == kind;
	}

	protected boolean isKind(Kind... kinds) {
		for (Kind k : kinds) {
			if (k == t.kind)
				return true;
		}
		return false;
	}

	/**
	 * Precondition: kind != EOF
	 * 
	 * @param kind
	 * @return
	 * @throws SyntaxException
	 */
	private Token match(Kind kind) throws SyntaxException {
		Token tmp = t;
		if (isKind(kind)) {
			consume();
			return tmp;
		}
		throw new SyntaxException(t, "Syntax Error. \n Mismatch occured at position " + tmp.pos + "\n Expected Token : "
				+ kind + "\n Actual Token: " + tmp.kind); // TODO give a better error message!
	}

	private Token consume() throws SyntaxException {
		Token tmp = t;
		if (isKind(EOF)) {
			throw new SyntaxException(t, "Unexpecetd EOF token at " + t.pos); // TODO give a better error message!
			// Note that EOF should be matched by the matchEOF method which is called only
			// in parse().
			// Anywhere else is an error. */
		}
		t = scanner.nextToken();
		return tmp;
	}

	/**
	 * Only for check at end of program. Does not "consume" EOF so no attempt to get
	 * nonexistent next Token.
	 * 
	 * @return
	 * @throws SyntaxException
	 */
	private Token matchEOF() throws SyntaxException {
		if (isKind(EOF)) {
			return t;
		}
		throw new SyntaxException(t, "Expected a EOF token. Token does not match \n Actual Token: " + t.getText());
		// TODO give a better error message!
	}

}
