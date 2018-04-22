/**
 * Starter code for CodeGenerator.java used n the class project in COP5556 Programming Language Principles 
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

import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

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
import cop5556sp18.AST.LHSIdent;
import cop5556sp18.AST.LHSPixel;
import cop5556sp18.AST.LHSSample;
import cop5556sp18.AST.PixelSelector;
import cop5556sp18.AST.Program;
import cop5556sp18.AST.StatementAssign;
import cop5556sp18.AST.StatementIf;
import cop5556sp18.AST.StatementInput;
import cop5556sp18.AST.StatementShow;
import cop5556sp18.AST.StatementSleep;
import cop5556sp18.AST.StatementWhile;
import cop5556sp18.AST.StatementWrite;

import cop5556sp18.CodeGenUtils;
import cop5556sp18.Scanner.Kind;

public class CodeGenerator implements ASTVisitor, Opcodes {

	/**
	 * All methods and variable static.
	 */

	static final int Z = 255;

	ClassWriter cw;
	String className;
	String classDesc;
	String sourceFileName;

	MethodVisitor mv; // visitor of method currently under construction

	/** Indicates whether genPrint and genPrintTOS should generate code. */
	final boolean DEVEL;
	final boolean GRADE;

	final int defaultWidth;
	final int defaultHeight;
	// final boolean itf = false;

	int slotCounter = 1;

	/**
	 * @param DEVEL
	 *            used as parameter to genPrint and genPrintTOS
	 * @param GRADE
	 *            used as parameter to genPrint and genPrintTOS
	 * @param sourceFileName
	 *            name of source file, may be null.
	 * @param defaultWidth
	 *            default width of images
	 * @param defaultHeight
	 *            default height of images
	 */
	public CodeGenerator(boolean DEVEL, boolean GRADE, String sourceFileName, int defaultWidth, int defaultHeight) {
		super();
		this.DEVEL = DEVEL;
		this.GRADE = GRADE;
		this.sourceFileName = sourceFileName;
		this.defaultWidth = defaultWidth;
		this.defaultHeight = defaultHeight;
	}

	@Override
	public Object visitBlock(Block block, Object arg) throws Exception {
		for (ASTNode node : block.decsOrStatements) {
			node.visit(this, null);
		}
		return null;
	}

	@Override
	public Object visitBooleanLiteral(ExpressionBooleanLiteral expressionBooleanLiteral, Object arg) throws Exception {
		mv.visitLdcInsn(expressionBooleanLiteral.value);

		return null;
	}

	@Override
	public Object visitDeclaration(Declaration declaration, Object arg) throws Exception {

		declaration.slot = slotCounter;
		slotCounter++;

		Kind decType = declaration.type;

		if (decType == Kind.KW_image) {
			if (declaration.height != null && declaration.width != null) {
				declaration.width.visit(this, arg);
				declaration.height.visit(this, arg);
			} else {
				// load values on to stack
				mv.visitLdcInsn(defaultWidth);
				mv.visitLdcInsn(defaultHeight);
			}
			// generate code to instantiate an image. visit method and put produced output
			// on the top of stack
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "makeImage",
					RuntimeImageSupport.makeImageSig, false);
			// pop the value from top of stack and store in the current slot of ASM array
			mv.visitVarInsn(ASTORE, declaration.slot);
		}

		return null;

	}

	// TODO
	@Override
	public Object visitExpressionBinary(ExpressionBinary expressionBinary, Object arg) throws Exception {

		Expression exp0 = expressionBinary.leftExpression;
		Expression exp1 = expressionBinary.rightExpression;

		Type lType = null, rType = null;

		if (exp0 != null)
			lType = exp0.getType();
		if (exp1 != null)
			rType = exp1.getType();

		Kind op = expressionBinary.op;

		if (lType == Type.INTEGER && rType == Type.INTEGER) {

			expressionBinary.leftExpression.visit(this, arg);
			expressionBinary.rightExpression.visit(this, arg);

			if (op == Kind.OP_POWER) {
				mv.visitInsn(I2D);
				mv.visitInsn(I2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2I);

			} else if (op == Kind.OP_PLUS) {
				mv.visitInsn(IADD);
			} else if (op == Kind.OP_MINUS) {
				mv.visitInsn(ISUB);
			} else if (op == Kind.OP_TIMES) {
				mv.visitInsn(IMUL);
			} else if (op == Kind.OP_DIV) {
				mv.visitInsn(IDIV);
			} else if (op == Kind.OP_MOD) {
				mv.visitInsn(IREM);
			} else if (op == Kind.OP_AND) {
				mv.visitInsn(IAND);
			} else if (op == Kind.OP_OR) {
				mv.visitInsn(IOR);
			} else if (op == Kind.OP_EQ) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPEQ, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_NEQ) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPNE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_GE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPGE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_GT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPGT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPLE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPLT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			}

		} else if (lType == Type.FLOAT && rType == Type.FLOAT) {

			expressionBinary.leftExpression.visit(this, arg);
			expressionBinary.rightExpression.visit(this, arg);

			if (op == Kind.OP_POWER) {
				mv.visitInsn(F2D);
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			} else if (op == Kind.OP_PLUS) {
				mv.visitInsn(FADD);
			} else if (op == Kind.OP_MINUS) {
				mv.visitInsn(FSUB);
			} else if (op == Kind.OP_TIMES) {
				mv.visitInsn(FMUL);
			} else if (op == Kind.OP_DIV) {
				mv.visitInsn(FDIV);
			} else if (op == Kind.OP_MOD) {
				mv.visitInsn(FREM);
			} else if (op == Kind.OP_EQ) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(FCMPG, setTrue);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				// remove top of the stack ?
				// mv.visitInsn(POP);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_NEQ) {
				mv.visitInsn(FCMPG);
			} else if (op == Kind.OP_GE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitInsn(FCMPL);
				mv.visitJumpInsn(IFGE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_GT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitInsn(FCMPL);
				mv.visitJumpInsn(IFGT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitInsn(FCMPL);
				mv.visitJumpInsn(IFLE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitInsn(FCMPL);
				mv.visitJumpInsn(IFLT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			}

		} else if (lType == Type.FLOAT && rType == Type.INTEGER) {

			expressionBinary.leftExpression.visit(this, arg);
			expressionBinary.rightExpression.visit(this, arg);

			if (op == Kind.OP_POWER) {
				mv.visitInsn(F2D);
				mv.visitInsn(I2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			} else if (op == Kind.OP_PLUS) {
				mv.visitInsn(I2F);
				mv.visitInsn(FADD);
			} else if (op == Kind.OP_MINUS) {
				mv.visitInsn(I2F);
				mv.visitInsn(FSUB);
			} else if (op == Kind.OP_TIMES) {
				mv.visitInsn(I2F);
				mv.visitInsn(FMUL);
			} else if (op == Kind.OP_DIV) {
				mv.visitInsn(I2F);
				mv.visitInsn(FDIV);
			} else if (op == Kind.OP_MOD) {
				mv.visitInsn(I2F);
				mv.visitInsn(FREM);
			}

		} else if (lType == Type.INTEGER && rType == Type.FLOAT) {

			expressionBinary.leftExpression.visit(this, arg);
			expressionBinary.rightExpression.visit(this, arg);

			if (op == Kind.OP_POWER) {
				mv.visitInsn(I2D);
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "pow", "(DD)D", false);
				mv.visitInsn(D2F);
			} else if (op == Kind.OP_PLUS) {
				mv.visitInsn(I2F);
				mv.visitInsn(FADD);
			} else if (op == Kind.OP_MINUS) {
				mv.visitInsn(I2F);
				mv.visitInsn(FSUB);
			} else if (op == Kind.OP_TIMES) {
				mv.visitInsn(I2F);
				mv.visitInsn(FMUL);
			} else if (op == Kind.OP_DIV) {
				mv.visitInsn(I2F);
				mv.visitInsn(FDIV);
			} else if (op == Kind.OP_MOD) {
				mv.visitInsn(I2F);
				mv.visitInsn(FREM);
			}

		} else if (lType == Type.BOOLEAN && rType == Type.BOOLEAN) {

			expressionBinary.leftExpression.visit(this, arg);
			expressionBinary.rightExpression.visit(this, arg);

			if (op == Kind.OP_AND) {
				mv.visitInsn(IAND);
			} else if (op == Kind.OP_OR) {
				mv.visitInsn(IOR);
			} else if (op == Kind.OP_EQ) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPEQ, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_NEQ) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPNE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_GE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPGE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_GT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPGT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LE) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPLE, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			} else if (op == Kind.OP_LT) {
				Label setTrue = new Label();
				Label endLabel = new Label();

				mv.visitJumpInsn(IF_ICMPLT, setTrue);
				mv.visitInsn(ICONST_0);
				mv.visitJumpInsn(GOTO, endLabel);
				mv.visitLabel(setTrue);
				mv.visitInsn(ICONST_1);
				mv.visitLabel(endLabel);
			}
		}

		return null;

	}

	@Override
	public Object visitExpressionConditional(ExpressionConditional expressionConditional, Object arg) throws Exception {

		Label falseLabel = new Label();
		Label trueLabel = new Label();
		Label endLabel = new Label();

		expressionConditional.guard.visit(this, arg);

		mv.visitJumpInsn(IFEQ, falseLabel);
		mv.visitLabel(trueLabel);
		expressionConditional.trueExpression.visit(this, arg);
		mv.visitJumpInsn(GOTO, endLabel);
		mv.visitLabel(falseLabel);
		expressionConditional.falseExpression.visit(this, arg);
		mv.visitLabel(endLabel);

		return null;
	}

	@Override
	public Object visitExpressionFloatLiteral(ExpressionFloatLiteral expressionFloatLiteral, Object arg)
			throws Exception {
		mv.visitLdcInsn(expressionFloatLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithExpressionArg(
			ExpressionFunctionAppWithExpressionArg expressionFunctionAppWithExpressionArg, Object arg)
			throws Exception {

		Kind function = expressionFunctionAppWithExpressionArg.function;
		expressionFunctionAppWithExpressionArg.e.visit(this, arg);

		if (function == Kind.KW_sin) {
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			mv.visitInsn(D2F);

		} else if (function == Kind.KW_cos) {
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			mv.visitInsn(D2F);

		} else if (function == Kind.KW_atan) {
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "atan", "(D)D", false);
			mv.visitInsn(D2F);

		} else if (function == Kind.KW_log) {
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "log", "(D)D", false);
			mv.visitInsn(D2F);

		} else if (function == Kind.KW_abs) {

			if (expressionFunctionAppWithExpressionArg.e.getType() == Type.INTEGER) {
				mv.visitInsn(I2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(D2I);

			} else if (expressionFunctionAppWithExpressionArg.e.getType() == Type.FLOAT) {
				mv.visitInsn(F2D);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "abs", "(D)D", false);
				mv.visitInsn(D2F);
			}

		} else if (function == Kind.KW_int) {
			if (expressionFunctionAppWithExpressionArg.e.getType() == Type.FLOAT) {
				mv.visitInsn(F2I);
			}

		} else if (function == Kind.KW_float) {
			if (expressionFunctionAppWithExpressionArg.e.getType() == Type.INTEGER) {
				mv.visitInsn(I2F);
			}

		} else if (function == Kind.KW_width) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "getWidth", RuntimeImageSupport.getWidthSig,
					false);

		} else if (function == Kind.KW_height) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "getHeight",
					RuntimeImageSupport.getHeightSig, false);

		} else if (function == Kind.KW_red) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getRed", RuntimePixelOps.getRedSig, false);

		} else if (function == Kind.KW_green) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getGreen", RuntimePixelOps.getGreenSig, false);

		} else if (function == Kind.KW_blue) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getBlue", RuntimePixelOps.getBlueSig, false);

		} else if (function == Kind.KW_alpha) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getAlpha", RuntimePixelOps.getAlphaSig, false);
		}

		return null;
	}

	@Override
	public Object visitExpressionFunctionAppWithPixel(ExpressionFunctionAppWithPixel expressionFunctionAppWithPixel,
			Object arg) throws Exception {

		Kind name = expressionFunctionAppWithPixel.name;

		if (name == Kind.KW_cart_x) {
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(F2D);
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "cos", "(D)D", false);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
		} else if (name == Kind.KW_cart_y) {
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(F2D);
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(F2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "sin", "(D)D", false);
			mv.visitInsn(DMUL);
			mv.visitInsn(D2I);
		} else if (name == Kind.KW_polar_a) {
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(I2D);
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(I2D);
			// mv.visitInsn(SWAP);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "atan2", "(DD)D", false);
			mv.visitInsn(D2F);
		} else if (name == Kind.KW_polar_r) {
			expressionFunctionAppWithPixel.e0.visit(this, arg);
			mv.visitInsn(I2D);
			expressionFunctionAppWithPixel.e1.visit(this, arg);
			mv.visitInsn(I2D);
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Math", "hypot", "(DD)D", false);
			mv.visitInsn(D2F);
		}

		return null;
	}

	@Override
	public Object visitExpressionIdent(ExpressionIdent expressionIdent, Object arg) throws Exception {

		if (Types.getType(expressionIdent.dec.type) == Type.INTEGER
				|| Types.getType(expressionIdent.dec.type) == Type.BOOLEAN) {
			mv.visitVarInsn(ILOAD, expressionIdent.dec.slot);
		} else if (Types.getType(expressionIdent.dec.type) == Type.FLOAT) {
			mv.visitVarInsn(FLOAD, expressionIdent.dec.slot);
		} else {
			mv.visitVarInsn(ALOAD, expressionIdent.dec.slot);
		}

		return null;
	}

	@Override
	public Object visitExpressionIntegerLiteral(ExpressionIntegerLiteral expressionIntegerLiteral, Object arg)
			throws Exception {
		// This one is all done!
		mv.visitLdcInsn(expressionIntegerLiteral.value);
		return null;
	}

	@Override
	public Object visitExpressionPixel(ExpressionPixel expressionPixel, Object arg) throws Exception {

		mv.visitVarInsn(ALOAD, expressionPixel.getDec().slot);
		expressionPixel.pixelSelector.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "getPixel", RuntimeImageSupport.getPixelSig,
				false);

		return null;
	}

	@Override
	public Object visitExpressionPixelConstructor(ExpressionPixelConstructor expressionPixelConstructor, Object arg)
			throws Exception {

		expressionPixelConstructor.alpha.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getAlpha", RuntimePixelOps.getAlphaSig, false);
		expressionPixelConstructor.red.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getRed", RuntimePixelOps.getRedSig, false);
		expressionPixelConstructor.green.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getGreen", RuntimePixelOps.getGreenSig, false);
		expressionPixelConstructor.blue.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "getBlue", RuntimePixelOps.getBlueSig, false);

		mv.visitMethodInsn(INVOKESTATIC, RuntimePixelOps.className, "makePixel", RuntimePixelOps.makePixelSig, false);

		return null;

	}

	@Override
	public Object visitExpressionPredefinedName(ExpressionPredefinedName expressionPredefinedName, Object arg)
			throws Exception {

		if (expressionPredefinedName.name == Kind.KW_Z) {
			mv.visitLdcInsn(Z);
		} else if (expressionPredefinedName.name == Kind.KW_default_width) {
			mv.visitLdcInsn(defaultWidth);
		} else if (expressionPredefinedName.name == Kind.KW_default_height) {
			mv.visitLdcInsn(defaultHeight);
		}

		return null;
	}

	// TODO Missing cases ?
	@Override
	public Object visitExpressionUnary(ExpressionUnary expressionUnary, Object arg) throws Exception {

		expressionUnary.expression.visit(this, arg);

		if (expressionUnary.op == Kind.OP_MINUS) {

			if (expressionUnary.expression.getType() == Type.INTEGER) {
				mv.visitInsn(INEG);
			} else if (expressionUnary.expression.getType() == Type.FLOAT) {
				mv.visitInsn(FNEG);
			}
		} else if (expressionUnary.op == Kind.OP_EXCLAMATION) {

			if (expressionUnary.expression.getType() == Type.INTEGER) {
				mv.visitLdcInsn(-1);
				mv.visitInsn(IXOR);
			} else if (expressionUnary.expression.getType() == Type.BOOLEAN) {
				mv.visitLdcInsn(true);
				mv.visitInsn(IXOR);
			}
		}

		return null;
	}

	// TODO test
	@Override
	public Object visitLHSIdent(LHSIdent lhsIdent, Object arg) throws Exception {

		if (Types.getType(lhsIdent.getDec().type) == Type.INTEGER) {
			mv.visitVarInsn(ISTORE, lhsIdent.getDec().slot);
		} else if (Types.getType(lhsIdent.getDec().type) == Type.FLOAT) {
			mv.visitVarInsn(FSTORE, lhsIdent.getDec().slot);
		} else if (Types.getType(lhsIdent.getDec().type) == Type.BOOLEAN) {
			mv.visitVarInsn(ISTORE, lhsIdent.getDec().slot);
		} else if (Types.getType(lhsIdent.getDec().type) == Type.FILE) {
			mv.visitVarInsn(ASTORE, lhsIdent.getDec().slot);
		} else if (Types.getType(lhsIdent.getDec().type) == Type.IMAGE) {
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "deepCopy", RuntimeImageSupport.deepCopySig,
					false);
			mv.visitVarInsn(ASTORE, lhsIdent.getDec().slot);
		}

		return null;
	}

	@Override
	public Object visitLHSPixel(LHSPixel lhsPixel, Object arg) throws Exception {
		mv.visitVarInsn(ALOAD, lhsPixel.getDec().slot);
		lhsPixel.pixelSelector.visit(this, arg);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "setPixel", RuntimeImageSupport.setPixelSig,
				false);

		return null;
	}

	@Override
	public Object visitLHSSample(LHSSample lhsSample, Object arg) throws Exception {

		Kind col = lhsSample.color;
		mv.visitVarInsn(ALOAD, lhsSample.getDec().slot);
		lhsSample.pixelSelector.visit(this, arg);

		if (col == Kind.KW_red) {
			mv.visitLdcInsn(RuntimePixelOps.RED);
		} else if (col == Kind.KW_green) {
			mv.visitLdcInsn(RuntimePixelOps.GREEN);
		} else if (col == Kind.KW_blue) {
			mv.visitLdcInsn(RuntimePixelOps.BLUE);
		} else if (col == Kind.KW_alpha) {
			mv.visitLdcInsn(RuntimePixelOps.ALPHA);
		}

		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "updatePixelColor",
				RuntimeImageSupport.updatePixelColorSig, false);

		return null;
	}

	@Override
	public Object visitPixelSelector(PixelSelector pixelSelector, Object arg) throws Exception {

		pixelSelector.ex.visit(this, arg);
		if (pixelSelector.ex.getType() == Type.FLOAT) {
			mv.visitInsn(F2I);
		}

		pixelSelector.ey.visit(this, arg);
		if (pixelSelector.ey.getType() == Type.FLOAT) {
			mv.visitInsn(F2I);
		}

		return null;
	}

	@Override
	public Object visitProgram(Program program, Object arg) throws Exception {

		cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
		/**
		 * COMPUTE_FRAMES - Flag to automatically compute the stack map frames of
		 * methods from scratch cw = new ClassWriter(0); If the call to mv.visitMaxs(1,
		 * 1) crashes, it is sometimes helpful to temporarily run it without
		 * COMPUTE_FRAMES. You probably won't get a completely correct classfile, but
		 * you will be able to see the code that was generated.
		 * 
		 */

		className = program.progName;
		classDesc = "L" + className + ";";
		String sourceFileName = (String) arg;
		cw.visit(52, ACC_PUBLIC + ACC_SUPER, className, null, "java/lang/Object", null);
		cw.visitSource(sourceFileName, null);

		// create main method
		mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "main", "([Ljava/lang/String;)V", null, null);
		// initialize
		mv.visitCode();

		// add label before first instruction
		Label mainStart = new Label();
		mv.visitLabel(mainStart);

		CodeGenUtils.genLog(DEVEL, mv, "entering main");

		program.block.visit(this, arg);

		// generates code to add string to log
		CodeGenUtils.genLog(DEVEL, mv, "leaving main");

		// adds the required (by the JVM) return statement to main
		mv.visitInsn(RETURN);

		// adds label at end of code
		Label mainEnd = new Label();
		mv.visitLabel(mainEnd);
		mv.visitLocalVariable("args", "[Ljava/lang/String;", null, mainStart, mainEnd, 0);
		// Because we use ClassWriter.COMPUTE_FRAMES as a parameter in the
		// constructor,
		// asm will calculate this itself and the parameters are ignored.
		// If you have trouble with failures in this routine, it may be useful
		// to temporarily change the parameter in the ClassWriter constructor
		// from COMPUTE_FRAMES to 0.
		// The generated classfile will not be correct, but you will at least be
		// able to see what is in it.
		mv.visitMaxs(0, 0);

		// terminate construction of main method
		mv.visitEnd();

		// terminate class construction
		cw.visitEnd();

		// generate classfile as byte array and return
		return cw.toByteArray();
	}

	@Override
	public Object visitStatementAssign(StatementAssign statementAssign, Object arg) throws Exception {

		statementAssign.e.visit(this, arg);
		statementAssign.lhs.visit(this, arg);

		return null;
	}

	@Override
	public Object visitStatementIf(StatementIf statementIf, Object arg) throws Exception {

		Label falseLabel = new Label();
		Label trueLabel = new Label();

		statementIf.guard.visit(this, arg);
		mv.visitJumpInsn(IFEQ, falseLabel);
		mv.visitLabel(trueLabel);
		statementIf.b.visit(this, arg);
		mv.visitLabel(falseLabel);

		return null;
	}

	@Override
	public Object visitStatementInput(StatementInput statementInput, Object arg) throws Exception {

		mv.visitVarInsn(ALOAD, 0);
		statementInput.e.visit(this, arg);
		mv.visitInsn(AALOAD);

		if (Types.getType(statementInput.getDec().type) == Type.INTEGER) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "parseInt", "(Ljava/lang/String;)I", false);
			mv.visitVarInsn(ISTORE, statementInput.getDec().slot);

		} else if (Types.getType(statementInput.getDec().type) == Type.FLOAT) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "parseFloat", "(Ljava/lang/String;)F", false);
			mv.visitVarInsn(FSTORE, statementInput.getDec().slot);

		} else if (Types.getType(statementInput.getDec().type) == Type.BOOLEAN) {
			mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "parseBoolean", "(Ljava/lang/String;)Z", false);// is
																													// this
																													// correct?
			mv.visitVarInsn(ISTORE, statementInput.getDec().slot);

		} else if (Types.getType(statementInput.getDec().type) == Type.FILE) {
			mv.visitVarInsn(ASTORE, statementInput.getDec().slot);

		} else if (Types.getType(statementInput.getDec().type) == Type.IMAGE) {

			if (statementInput.getDec().height == null && statementInput.getDec().width == null) {

				mv.visitInsn(ACONST_NULL);
				mv.visitInsn(ACONST_NULL);
				mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "readImage",
						RuntimeImageSupport.readImageSig, false);
				mv.visitVarInsn(ASTORE, statementInput.getDec().slot);

			} else if (statementInput.getDec().height != null && statementInput.getDec().width != null) {

				statementInput.getDec().width.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);

				statementInput.getDec().height.visit(this, arg);
				mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);

				mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "readImage",
						RuntimeImageSupport.readImageSig, false);
				mv.visitVarInsn(ASTORE, statementInput.getDec().slot);
			}
		}

		return null;
	}

	@Override
	public Object visitStatementShow(StatementShow statementShow, Object arg) throws Exception {
		/**
		 * TODO refactor and complete implementation.
		 * 
		 * For integers, booleans, and floats, generate code to print to console. For
		 * images, generate code to display in a frame.
		 * 
		 * In all cases, invoke CodeGenUtils.genLogTOS(GRADE, mv, type); before
		 * consuming top of stack.
		 */
		statementShow.e.visit(this, arg);
		Type type = statementShow.e.getType();
		switch (type) {
		case INTEGER: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(I)V", false);
		}
			break;
		case BOOLEAN: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Z)V", false);

		}
			break;
		case FLOAT: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(F)V", false);
		}
			break;
		case FILE: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitFieldInsn(Opcodes.GETSTATIC, "java/lang/System", "out", "Ljava/io/PrintStream;");
			mv.visitInsn(Opcodes.SWAP);
			mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/PrintStream", "println", "(Ljava/lang/String;)V", false);
		}
			break;
		case IMAGE: {
			CodeGenUtils.genLogTOS(GRADE, mv, type);
			mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "makeFrame",
					RuntimeImageSupport.makeFrameSig, false);
			mv.visitInsn(POP);
		}
			break;
		default: {
			// TODO throw exception ?
		}

		}
		return null;
	}

	@Override
	public Object visitStatementSleep(StatementSleep statementSleep, Object arg) throws Exception {

		statementSleep.duration.visit(this, arg);

		if (statementSleep.duration.getType() == Type.INTEGER) {
			mv.visitInsn(I2L);
		} else if (statementSleep.duration.getType() == Type.FLOAT) {
			mv.visitInsn(F2L);
		}

		mv.visitMethodInsn(INVOKESTATIC, "java/lang/Thread", "sleep", "(J)V", false);

		return null;
	}

	@Override
	public Object visitStatementWhile(StatementWhile statementWhile, Object arg) throws Exception {

		Label whilestart = new Label();
		Label whileblock = new Label();

		mv.visitJumpInsn(GOTO, whilestart);
		mv.visitLabel(whileblock);
		statementWhile.b.visit(this, arg);
		mv.visitLabel(whilestart);
		statementWhile.guard.visit(this, arg);
		mv.visitJumpInsn(IFNE, whileblock);

		return null;
	}

	@Override
	public Object visitStatementWrite(StatementWrite statementWrite, Object arg) throws Exception {
		mv.visitVarInsn(ALOAD, statementWrite.sourceDec.slot);
		mv.visitVarInsn(ALOAD, statementWrite.destDec.slot);
		mv.visitMethodInsn(INVOKESTATIC, RuntimeImageSupport.className, "write", RuntimeImageSupport.writeSig, false);

		return null;
	}

}
