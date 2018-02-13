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
import cop5556sp18.Scanner.Kind;
import static cop5556sp18.Scanner.Kind.*;

public class SimpleParser {

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

	SimpleParser(Scanner scanner) {
		this.scanner = scanner;
		t = scanner.nextToken();
	}

	public void parse() throws SyntaxException {
		program();
		matchEOF();
	}

	/*
	 * Program ::= Identifier Block
	 */
	public void program() throws SyntaxException {
		match(IDENTIFIER);
		block();
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
	public void block() throws SyntaxException {
		match(LBRACE);
		while (isKind(firstDec) | isKind(firstStatement)) {
			if (isKind(firstDec)) {
				declaration();
			} else if (isKind(firstStatement)) {
				statement();
			}
			match(SEMI);
		}
		match(RBRACE);

	}

	/*
	 * Declaration ::= image Identifier ( epsilon | [Expression, Expression] | Type
	 * Identifier
	 */
	public void declaration() throws SyntaxException {

		if (isKind(Kind.KW_image)) {
			match(KW_image);
			match(IDENTIFIER);

			if (isKind(LSQUARE)) {
				match(LSQUARE);
				safeExpression();
				match(COMMA);
				safeExpression();
				match(RSQUARE);
			}
		} else {
			switch (t.kind) {
			case KW_int:
				match(KW_int);
				break;
			case KW_float:
				match(KW_float);
				break;
			case KW_boolean:
				match(KW_boolean);
				break;
			case KW_filename:
				match(KW_filename);
				break;
			default:
				throw new SyntaxException(t, "Illegal token at the start of declaration \n Error at " + t.pos);
			}
			match(IDENTIFIER);
		}

	}

	/*
	 * Statement ::= StatementInput | StatementWrite | StatementAssignment |
	 * StatementWhile | StatementIf | StatementShow | StatementSleep
	 */
	public void statement() throws SyntaxException {
		switch (t.kind) {

		// StatementInput ::= ​input IDENTIFIER from @ ​Expression
		case KW_input: {
			match(KW_input);
			match(IDENTIFIER);
			match(KW_from);
			match(OP_AT);
			safeExpression();
		}
			break;

		// StatementWrite ::= ​write IDENTIFIER to IDENTIFIER
		case KW_write: {
			match(KW_write);
			match(IDENTIFIER);
			match(KW_to);
			match(IDENTIFIER);
		}
			break;

		// StatementAssignment ::= LHS ​:=​ Expression
		// LHS ::= ​IDENTIFIER​ (epsilon | ​PixelSelector) | Color ​(​ ​IDENTIFIER PixelSelector ​)​
		case IDENTIFIER:
		case KW_red:
		case KW_green:
		case KW_blue:
		case KW_alpha: {

			// Match LHS
			lhs();
			match(OP_ASSIGN);
			safeExpression();
		}
			break;

		// StatementWhile ::= ​while (​Expression ​)​ Block
		case KW_while: {
			match(KW_while);
			match(LPAREN);
			safeExpression();
			match(RPAREN);
			block();
		}
			break;

		// StatementIf ::= ​if (​ Expression ​)​ Block
		case KW_if: {
			match(KW_if);
			match(LPAREN);
			safeExpression();
			match(RPAREN);
			block();
		}
			break;

		// StatementShow ::= ​show​ Expression
		case KW_show: {
			match(KW_show);
			safeExpression();
		}
			break;

		// StatementSleep ::= ​sleep​ Expressio
		case KW_sleep: {
			match(KW_sleep);
			safeExpression();
		}
		break;
		
		default:
			throw new SyntaxException(t, "Illegal token at the start of statement \n Error at " + t.pos);
		}
	}

	public void safeExpression() throws SyntaxException {
		if (isKind(firstExpression)) {
			expression();
		} else {
			throw new SyntaxException(t, "Illegal token at the start of expression \n Error at " + t.pos);
		}
	}

	/*
	 * LHS ::= ​IDENTIFIER​ (epsilon | ​PixelSelector) | Color ​(​ ​IDENTIFIER​
	 * PixelSelector ​)
	 */
	public void lhs() throws SyntaxException {
		if (isKind(IDENTIFIER)) {
			match(IDENTIFIER);

			// PixelSelector ::= ​[ ​Expression ​,​ Expression ​]
			if (isKind(LSQUARE)) {
				pixelSelector();
			}
		} else {
			color();
			match(LPAREN);
			match(IDENTIFIER);
			pixelSelector();
			match(RPAREN);
		}
	}

	/*
	 * Expression ::= OrExpression(epsilon | ​?​ Expression ​:​ Expression)
	 */
	public void expression() throws SyntaxException {
		orExpression();

		if (isKind(OP_QUESTION)) {
			match(OP_QUESTION);
			safeExpression();
			match(OP_COLON);
			safeExpression();
		}
	}

	/*
	 * OrExpression ::= AndExpression ( ​|​ AndExpression ) *
	 */
	public void orExpression() throws SyntaxException {
		andExpression();

		while (isKind(OP_OR)) {
			match(OP_OR);
			andExpression();
		}
	}

	/*
	 * AndExpression ::= EqExpression ( ​&​ EqExpression )*
	 */
	public void andExpression() throws SyntaxException {
		eqExpression();

		while (isKind(OP_AND)) {
			match(OP_AND);
			eqExpression();
		}
	}

	/*
	 * EqExpression ::= RelExpression ( (​==​ | ​!=​ ) RelExpression )*
	 */
	public void eqExpression() throws SyntaxException {
		relExpression();

		while (isKind(OP_EQ) | isKind(OP_NEQ)) {
			if (t.kind == OP_EQ) {
				match(OP_EQ);
			} else if (t.kind == OP_NEQ) {
				match(OP_NEQ);
			}
			relExpression();
		}
	}

	/*
	 * RelExpression ::= AddExpression ( (​<​ | ​>​ | ​<=​ | ​>=​ ) AddExpression)*
	 */
	public void relExpression() throws SyntaxException {
		addExpression();

		while (isKind(OP_GE) || isKind(OP_LE) || isKind(OP_GT) || isKind(OP_LT)) {
			switch (t.kind) {
			case OP_GE:
				match(OP_GE);
				break;
			case OP_LE:
				match(OP_LE);
				break;
			case OP_GT:
				match(OP_GT);
				break;
			case OP_LT:
				match(OP_LT);
				break;
			default :
				throw new SyntaxException(t, "Illegal token in relational expression \n Error at " + t.pos);
			}
			andExpression();
		}

	}

	/*
	 * AddExpression ::= MultExpression ( ( ​+​ | ​-​ ) MultExpression )*
	 */
	public void addExpression() throws SyntaxException {
		multExpression();

		while (isKind(OP_PLUS) | isKind(OP_MINUS)) {
			if (t.kind == OP_PLUS) {
				match(OP_PLUS);
			} else if (t.kind == OP_MINUS) {
				match(OP_MINUS);
			}
			multExpression();
		}

	}

	/*
	 * MultExpression := PowerExpression ( ( ​*​ | ​/​ | ​%​ ) PowerExpression )*
	 */
	public void multExpression() throws SyntaxException {
		powerExpression();

		while (isKind(OP_TIMES) || isKind(OP_DIV) || isKind(OP_MOD)) {
			switch (t.kind) {
			case OP_TIMES:
				match(OP_TIMES);
				break;
			case OP_DIV:
				match(OP_DIV);
				break;
			case OP_MOD:
				match(OP_MOD);
				break;
			}
			powerExpression();
		}
	}

	/*
	 * PowerExpression := UnaryExpression (​**​ PowerExpression | ε)
	 */
	public void powerExpression() throws SyntaxException {
		unaryExpression();

		if (isKind(OP_POWER)) {
			match(OP_POWER);
			powerExpression();
		}
	}

	/*
	 * UnaryExpression ::= ​+​ UnaryExpression | ​-​ UnaryExpression |
	 * UnaryExpressionNotPlusMinus
	 */
	public void unaryExpression() throws SyntaxException {
		if (isKind(OP_PLUS)) {
			match(OP_PLUS);
			unaryExpression();
		} else if (isKind(OP_MINUS)) {
			match(OP_MINUS);
			unaryExpression();
		} else {
			unaryExpressionNotPlusMinus();
		}
	}

	/*
	 * UnaryExpressionNotPlusMinus ::= ​!​ UnaryExpression | Primary
	 */
	public void unaryExpressionNotPlusMinus() throws SyntaxException {
		if (isKind(OP_EXCLAMATION)) {
			match(OP_EXCLAMATION);
			unaryExpression();
		} else {
			primary();
		}
	}

	/*
	 * Primary ::= ​INTEGER_LITERAL ​| ​BOOLEAN_LITERAL ​| ​FLOAT_LITERAL ​|(​
	 * Expression​ ) ​| FunctionApplication |​ IDENTIFIER (epsilon ​| PixelSelector)
	 * | PredefinedName | PixelConstructor
	 */
	public void primary() throws SyntaxException {
		switch (t.kind) {

		case INTEGER_LITERAL:
			match(INTEGER_LITERAL);
			break;

		case BOOLEAN_LITERAL:
			match(BOOLEAN_LITERAL);
			break;

		case FLOAT_LITERAL:
			match(FLOAT_LITERAL);
			break;

		// Expression
		case LPAREN: {
			match(LPAREN);
			safeExpression();
			match(RPAREN);
		}
			break;

		case LPIXEL:
			pixelConstructor();
			break;

		// IDENTIFIER (epsilon ​| PixelSelector)
		case IDENTIFIER: {
			match(IDENTIFIER);
			if (isKind(LSQUARE)) {
				pixelSelector();
			}
		}
			break;

		/*
		 * FunctionApplication, PredefinedName FunctionApplication ::= FunctionName​ (
		 * (​ Expression ​)​ | ​[​ Expression ​,​ Expression ​] )
		 */
		default: {
			if (isKind(firstFunctionName)) {
				functionApplication();
			} else if (isKind(firstPredefinedname)) {
				predefinedName();
			}
		}

		} // end switch

	}

	/*
	 * FunctionApplication ::= FunctionName​ ( (​ Expression ​)​ | ​[​ Expression
	 * ​,​ Expression ​] )
	 */
	public void functionApplication() throws SyntaxException {
		functionName();
		if (isKind(LPAREN)) {
			match(LPAREN);
			safeExpression();
			match(RPAREN);
		} else if (isKind(LSQUARE)) {
			match(LSQUARE);
			safeExpression();
			match(COMMA);
			safeExpression();
			match(RSQUARE);
		}
	}

	/*
	 * FunctionName ::= ​sin​ | ​cos​ | ​atan​ | ​abs​ | ​log​ | ​cart_x ​| ​cart_y
	 * ​| ​ polar_a ​| ​polar_r | int​ | ​float ​| ​width​ | ​height ​| Color
	 */
	public void functionName() throws SyntaxException {
		switch (t.kind) {
		case KW_sin:
			match(KW_sin);
			break;
		case KW_cos:
			match(KW_cos);
			break;
		case KW_atan:
			match(KW_atan);
			break;
		case KW_abs:
			match(KW_abs);
			break;
		case KW_log:
			match(KW_log);
			break;
		case KW_cart_x:
			match(KW_cart_x);
			break;
		case KW_cart_y:
			match(KW_cart_y);
			break;
		case KW_polar_a:
			match(KW_polar_a);
			break;
		case KW_polar_r:
			match(KW_polar_r);
			break;
		case KW_int:
			match(KW_int);
			break;
		case KW_float:
			match(KW_float);
			break;
		case KW_width:
			match(KW_width);
			break;
		case KW_height:
			match(KW_height);
			break;
		default: {
			if (isKind(firstColor)) {
				color();
			}
		}

		}
	}

	/*
	 * PredefinedName ::= ​Z ​| ​default_height ​| ​default_width
	 */
	public void predefinedName() throws SyntaxException {
		switch (t.kind) {
		case KW_Z:
			match(KW_Z);
			break;
		case KW_default_height:
			match(Kind.KW_default_height);
			break;
		case KW_default_width:
			match(Kind.KW_default_width);
			break;
		}
	}

	/*
	 * PixelConstructor ::= ​<< ​ Expression​ , ​Expression ​,​ Expression ​,​
	 * Expression ​>>
	 */
	public void pixelConstructor() throws SyntaxException {
		match(LPIXEL);
		safeExpression();
		match(COMMA);
		safeExpression();
		match(COMMA);
		safeExpression();
		match(COMMA);
		safeExpression();
		match(RPIXEL);

	}

	/*
	 * PixelSelector ::= ​[ ​Expression ​,​ Expression ​]
	 */
	public void pixelSelector() throws SyntaxException {
		match(LSQUARE);
		safeExpression();
		match(COMMA);
		safeExpression();
		match(RSQUARE);
	}

	/*
	 * Color ::= ​red​ | ​green​ | ​blue​ | ​alpha
	 */
	public void color() throws SyntaxException {
		switch (t.kind) {
		case KW_red:
			match(KW_red);
			break;
		case KW_green:
			match(KW_green);
			break;
		case KW_blue:
			match(KW_blue);
			break;
		case KW_alpha:
			match(KW_alpha);
			break;
		}
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
			throw new SyntaxException(t, "Unexpecetd EOF token"); // TODO give a better error message!
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
		throw new SyntaxException(t, "Expected a EOF token. Token does not match"); // TODO give a better error message!
	}

}
