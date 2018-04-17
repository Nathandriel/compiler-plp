package cop5556sp18;


import cop5556sp18.Scanner.Kind;
import cop5556sp18.Scanner.Token;
import cop5556sp18.Types.Type;
import cop5556sp18.AST.ASTNode;
import cop5556sp18.AST.ASTVisitor;
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

public class TypeChecker implements ASTVisitor {


	TypeChecker() {
	}

	@SuppressWarnings("serial")
	public static class SemanticException extends Exception {
		Token t;

		public SemanticException(Token t, String message) {
			super(message);
			this.t = t;
		}
	}

	SymbolTable symbolTable = new SymbolTable();
	
	// Name is only used for naming the output file. 
	// Visit the child block to type check program.
	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {
		program.block.visit(this, arg);
		return program;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {

		symbolTable.enterScope();
		
		for (ASTNode decOrStat : block.decsOrStatements) {
			if (decOrStat instanceof Declaration) {
				((Declaration) decOrStat).visit(this, null);
			} else if (decOrStat instanceof Statement) {
				((Statement) decOrStat).visit(this, null);
			} else {
				throw new SemanticException(block.firstToken, "Declaration or Statement is expected");
			}
		}
		
		symbolTable.leaveScope();
		return block;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {
		
		boolean flag = symbolTable.insert(declaration.name, declaration);
		if (!flag) {
			throw new SemanticException(declaration.firstToken, "Ident " + declaration.name + " declaration already exists in scope");
		}
		
		Expression e0 = declaration.width;
		Expression e1 = declaration.height;
		
		if (e0 != null) e0 = (Expression) e0.visit(this, null);
		if (e1 != null) e1 = (Expression) e1.visit(this, null);
		
		if (e0 != null && e1 != null) {
			if (e0.getType() != e1.getType()) {
				throw new SemanticException(declaration.firstToken, "Both expresions in declaration should be of the same type");
			}
		}
		
		if ((e0 == null && e1 != null) || (e1 == null && e0 != null)) {
			throw new SemanticException(declaration.firstToken, "Both expresions in declaration should be null or non-null");
		}
		
		return declaration;
		
	}

	
	
	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg) throws Exception {
		statementWrite.setSourceDec(symbolTable.lookup(statementWrite.sourceName));
		statementWrite.setDestDec(symbolTable.lookup(statementWrite.destName));
		
		if (statementWrite.getSourceDec() != null && statementWrite.getDestDec() != null ) {
			if (Types.getType(statementWrite.getSourceDec().type) == Type.IMAGE && Types.getType(statementWrite.getDestDec().type) == Type.FILE) {
				return statementWrite;
			} else {
				throw new SemanticException(statementWrite.firstToken, "Type Mismatch. Source should be of type image and Destination should be file");
			}
		}
		throw new SemanticException(statementWrite.firstToken, "Source or Destination declaration not found in StatementWrite");
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws Exception {
		statementInput.setDec(symbolTable.lookup(statementInput.destName));

		Expression exp = statementInput.e;
		exp = (Expression) exp.visit(this, null);
		
		if (statementInput.getDec() != null && exp.getType() == Type.INTEGER ) {
			return statementInput;
		}
		throw new SemanticException(statementInput.firstToken, "Type Mismatch. Can't input " + statementInput.destName + " from expression of type " + exp.getType());
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {
		LHS lhs = statementAssign.lhs;
		Expression exp = statementAssign.e;
		lhs = (LHS) lhs.visit(this, null);
		exp = (Expression) exp.visit(this, null);
		
		if (exp.getType() != lhs.getType()) {
			throw new SemanticException(exp.firstToken, "Type mismatch: Expression of type " + exp.getType() + " cannot be assigned to " + lhs.getFirstToken().getText());
		}
		return statementAssign;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg) throws Exception {
		Expression exp = statementShow.e;
		exp = (Expression) exp.visit(this, null);
		Type t = exp.getType();
		
		if (t == Type.INTEGER || t == Type.BOOLEAN || t == Type.FLOAT || t == Type.IMAGE) {
			return statementShow;
		}
		throw new SemanticException(exp.firstToken, "Type mismatch: Expression of type " + t + " cannot be used in a show statement");
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws Exception {
		Expression exp = statementIf.guard;
		exp = (Expression) exp.visit(this, null);
		
		if (exp.getType() == Type.BOOLEAN) {
			Block block = statementIf.b ;
			block.visit(this, null);
			return statementIf;
		}
		
		throw new SemanticException(exp.firstToken, "The guard expression of IF statement is not of type Boolean");
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws Exception {
		Expression exp = statementWhile.guard;
		exp = (Expression) exp.visit(this, null);
		
		if (exp.getType() == Type.BOOLEAN) {
			Block block = statementWhile.b;
			block.visit(this, null);
			return statementWhile;
		}
		
		throw new SemanticException(exp.firstToken, "The guard expression of WHILE statement is not of type Boolean");

	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg) throws Exception {
		Expression exp = statementSleep.duration;
		exp = (Expression) exp.visit(this, null);
		
		if (exp.getType() == Type.INTEGER) {
			return statementSleep;
		}
		
		throw new SemanticException(exp.firstToken, "The duration of SLEEP statement is not an Integer");
		
	}

	
	

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg) throws Exception {
		lhsSample.setDec(symbolTable.lookup(lhsSample.name));
		
		if (lhsSample.getDec() != null && lhsSample.getDec().type == Kind.KW_image) {
			lhsSample.setType(Type.INTEGER);
			return lhsSample;
		}
		throw new SemanticException(lhsSample.firstToken, "Declaration not found in scope in LHSSample");
}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg) throws Exception {
		lhsPixel.setDec(symbolTable.lookup(lhsPixel.name));
		
		if (lhsPixel.getDec() != null && lhsPixel.getDec().type == Kind.KW_image) {
			lhsPixel.setType(Type.INTEGER);
			return lhsPixel;
		}
		throw new SemanticException(lhsPixel.firstToken, "Declaration not found in scope in LHSPixel");
	}

	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg) throws Exception {
		lhsIdent.setDec(symbolTable.lookup(lhsIdent.name));
		
		if (lhsIdent.getDec() != null) {
			lhsIdent.setType(Types.getType(lhsIdent.getDec().type));
			return lhsIdent;
		}
		throw new SemanticException(lhsIdent.firstToken, "Declaration not found in scope in LHSIdent");
	}

	
	
	
	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {
		Expression e0 = pixelSelector.ex;
		Expression e1 = pixelSelector.ey;
		
		e0 = (Expression) e0.visit(this, null);
		e1 = (Expression) e1.visit(this, null);
		
		if (e0.getType() == e1.getType()) {
			if (e0.getType() == Type.INTEGER || e0.getType() == Type.FLOAT) {
				return pixelSelector;
			} else {
				throw new SemanticException(pixelSelector.firstToken, "Expressions in pixelSelector should be of type Integer or Float");
			}
		} else {
			throw new SemanticException(pixelSelector.firstToken, "Type mismatch. Expressions in pixelSelector should be of same type");
		}
		
	}

	@Override
	public Object visitExpressionConditional(ExpressionConditional expressionConditional, Object arg) throws Exception {
		Expression exp0 = expressionConditional.guard;
		Expression exp1 = expressionConditional.trueExpression;
		Expression exp2 = expressionConditional.falseExpression;
		
		Expression e0 = (Expression) exp0.visit(this, null);
		Expression e1 = (Expression) exp1.visit(this, null);
		Expression e2 = (Expression) exp2.visit(this, null);
		
		if ((e0.getType() == Type.BOOLEAN) && (e1.getType() == e2.getType())) {
			expressionConditional.setType(e1.getType());
			return expressionConditional;
		}
		throw new SemanticException(expressionConditional.firstToken, "Type mismatch in ExpressionConditional");
	}

	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws Exception {
		Expression leftExp = expressionBinary.leftExpression;
		Expression rightExp = expressionBinary.rightExpression;
		
		Expression e0 = (Expression) leftExp.visit(this, null);
		Expression e1 = (Expression) rightExp.visit(this, null);
		Kind op = expressionBinary.op;
		
		if (e0.getType() == Type.BOOLEAN && e1.getType() == Type.BOOLEAN) {
			switch(op) {
				case OP_LT:
				case OP_GT:
				case OP_LE:
				case OP_GE:
				case OP_EQ:
				case OP_NEQ:
				case OP_AND:
				case OP_OR: {
					expressionBinary.setType(Type.BOOLEAN);
					return expressionBinary;
				}
				default:
					throw new SemanticException(leftExp.firstToken, "Invalid operation performed on boolean expressions " + leftExp + " and " + rightExp);
				
			}
		} else if (e0.getType() == Type.INTEGER && e1.getType() ==Type.INTEGER) {
			switch(op) {
				case OP_PLUS:
				case OP_MINUS:
				case OP_TIMES:
				case OP_DIV:
				case OP_MOD:
				case OP_POWER:
				case OP_AND:
				case OP_OR: {
					expressionBinary.setType(Type.INTEGER);
					return expressionBinary;
				}
				case OP_LT:
				case OP_GT:
				case OP_LE:
				case OP_GE:
				case OP_EQ:
				case OP_NEQ: {
					expressionBinary.setType(Type.BOOLEAN);
					return expressionBinary;
				}
				default:
					throw new SemanticException(leftExp.firstToken, "Invalid operation performed on integer expressions " + leftExp + " and " + rightExp);
			
			} 
		} else if (e0.getType() == Type.FLOAT && e1.getType() == Type.FLOAT) {
				switch(op) {
					case OP_PLUS:
					case OP_MINUS:
					case OP_TIMES:
					case OP_DIV:
					case OP_POWER: {
						expressionBinary.setType(Type.FLOAT);
						return expressionBinary;
					}
					case OP_LT:
					case OP_GT:
					case OP_LE:
					case OP_GE:
					case OP_EQ:
					case OP_NEQ: {
						expressionBinary.setType(Type.BOOLEAN);
						return expressionBinary;
					}
					default:
						throw new SemanticException(leftExp.firstToken, "Invalid operation performed on float expressions " + leftExp + " and " + rightExp);
				}
			} else if ((e0.getType() == Type.INTEGER && e1.getType() == Type.FLOAT) || (e0.getType()==Type.FLOAT && e1.getType()==Type.INTEGER)) {
				switch(op) {
					case OP_PLUS:
					case OP_MINUS:
					case OP_TIMES:
					case OP_DIV:
					case OP_POWER: {
						expressionBinary.setType(Type.FLOAT);
						return expressionBinary;
					}
					default:
						throw new SemanticException(leftExp.firstToken, "Invalid operation performed on float, int expressions " + leftExp + " and " + rightExp);
				}
			} else {
				throw new SemanticException(leftExp.firstToken, "Incompatible types " + leftExp + " and " + rightExp);
			}
		}
		
	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary, Object arg) throws Exception {
		Expression exp = expressionUnary.expression;
		exp = (Expression) exp.visit(this, null);
		
		expressionUnary.setType(exp.getType());
		return expressionUnary;
	}

	@Override
	public Object visitExpressionIntegerLiteral(ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		expressionIntegerLiteral.setType(Type.INTEGER);
		return expressionIntegerLiteral;
		
	}

	@Override
	public Object visitBooleanLiteral(ExpressionBooleanLiteral expressionBooleanLiteral, Object arg) throws Exception {
		expressionBooleanLiteral.setType(Type.BOOLEAN);
		return expressionBooleanLiteral;
	}

	@Override
	public Object visitExpressionPredefinedName(ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {
		expressionPredefinedName.setType(Type.INTEGER);
		return expressionPredefinedName;
	}

	@Override
	public Object visitExpressionFloatLiteral(ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		expressionFloatLiteral.setType(Type.FLOAT);
		return expressionFloatLiteral;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg, Object arg)
			throws Exception {
		Kind functionName = expressionFunctionAppWithExpressionArg.function;
		Expression exp = expressionFunctionAppWithExpressionArg.e;
		exp = (Expression) exp.visit(this, null);
		
		if (exp.getType() == Type.INTEGER) {
			switch(functionName) {
				case KW_int:
				case KW_abs:
				case KW_red:
				case KW_green:
				case KW_blue:
				case KW_alpha: {
					expressionFunctionAppWithExpressionArg.setType(Type.INTEGER);
					return expressionFunctionAppWithExpressionArg;
				}
				case KW_float:{
					expressionFunctionAppWithExpressionArg.setType(Type.FLOAT);
					return expressionFunctionAppWithExpressionArg;
				}
			}
		} else if (exp.getType() == Type.FLOAT) {
			switch(functionName) {
				case KW_float:
				case KW_abs:
				case KW_sin:
				case KW_cos:
				case KW_atan:
				case KW_log: {
					expressionFunctionAppWithExpressionArg.setType(Type.FLOAT);
					return expressionFunctionAppWithExpressionArg;
				}
				case KW_int: {
					expressionFunctionAppWithExpressionArg.setType(Type.INTEGER);
					return expressionFunctionAppWithExpressionArg;
				}
			}
		} else if (exp.getType() == Type.IMAGE) {
			switch(functionName) {
				case KW_width:
				case KW_height: {
					expressionFunctionAppWithExpressionArg.setType(Type.INTEGER);
					return expressionFunctionAppWithExpressionArg;
				}
				default:
					throw new SemanticException(expressionFunctionAppWithExpressionArg.firstToken, "Invalid function name");
				
			}
		} else {
			throw new SemanticException(expressionFunctionAppWithExpressionArg.firstToken, "Invalid combination. Valid type can't be inferred");
		}
		
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception {
		Kind functionName = expressionFunctionAppWithPixel.name;
		Expression e0 = expressionFunctionAppWithPixel.e0;
		Expression e1 = expressionFunctionAppWithPixel.e1;
		
		e0 = (Expression) e0.visit(this, null);
		e1 = (Expression) e1.visit(this, null);
		
		
		if (functionName == Kind.KW_cart_x || functionName ==Kind.KW_cart_y) {
			if (e0.getType() == Type.FLOAT && e1.getType() == Type.FLOAT) {
				expressionFunctionAppWithPixel.setType(Type.INTEGER);
				return expressionFunctionAppWithPixel;
			} else {
				throw new SemanticException(expressionFunctionAppWithPixel.firstToken, "Type mismatch in ExpressionFunctionAppWithPixel (cartesian)" );
			}
		} else if (functionName == Kind.KW_polar_a || functionName == Kind.KW_polar_r) {
			if (e0.getType() == Type.INTEGER && e1.getType() == Type.INTEGER) {
				expressionFunctionAppWithPixel.setType(Type.FLOAT);
				return expressionFunctionAppWithPixel;
			} else {
				throw new SemanticException(expressionFunctionAppWithPixel.firstToken, "Type mismatch in ExpressionFunctionAppWithPixel (polar)" );
			}
		}
		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {
		Expression eAlpha = expressionPixelConstructor.alpha;
		Expression eRed = expressionPixelConstructor.red;
		Expression eGreen = expressionPixelConstructor.green;
		Expression eBlue = expressionPixelConstructor.blue;
		
		eAlpha = (Expression) eAlpha.visit(this, null);
		eRed = (Expression) eRed.visit(this, null);
		eGreen = (Expression) eGreen.visit(this, null);
		eBlue = (Expression) eBlue.visit(this, null);
		
		if (eAlpha.getType() == Type.INTEGER && eRed.getType() == Type.INTEGER && eGreen.getType() == Type.INTEGER && eBlue.getType() == Type.INTEGER) {
			expressionPixelConstructor.setType(Type.INTEGER);
			return expressionPixelConstructor;
		}
		
		throw new SemanticException(expressionPixelConstructor.firstToken, "Type Mismatch. All expressions in ExpressionPixelConstructor should be of type Integer");
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel, Object arg) throws Exception {
		expressionPixel.setDec(symbolTable.lookup(expressionPixel.name));
		
		if (expressionPixel.getDec() != null && Types.getType(expressionPixel.getDec().type) ==Type.IMAGE) {
			expressionPixel.setType(Type.INTEGER);
			return expressionPixel;
		}
		throw new SemanticException(expressionPixel.firstToken, "Declartion not found in ExpressionPixel");
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws Exception {
		expressionIdent.setDec(symbolTable.lookup(expressionIdent.name));
		
		if (expressionIdent.getDec() != null) {
			expressionIdent.setType(Types.getType(expressionIdent.getDec().type));
			return expressionIdent;
		}
		throw new SemanticException(expressionIdent.firstToken, "Declaration not found in ExpressionIdent");
	}


}
