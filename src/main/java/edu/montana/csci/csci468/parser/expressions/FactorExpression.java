package edu.montana.csci.csci468.parser.expressions;

import edu.montana.csci.csci468.bytecode.ByteCodeGenerator;
import edu.montana.csci.csci468.eval.CatscriptRuntime;
import edu.montana.csci.csci468.parser.CatscriptType;
import edu.montana.csci.csci468.parser.ErrorType;
import edu.montana.csci.csci468.parser.ParseError;
import edu.montana.csci.csci468.parser.SymbolTable;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenType;

public class FactorExpression extends Expression {

    private final Token operator;
    private final Expression leftHandSide;
    private final Expression rightHandSide;

    public FactorExpression(Token operator, Expression leftHandSide, Expression rightHandSide) {
        this.leftHandSide = addChild(leftHandSide);
        this.rightHandSide = addChild(rightHandSide);
        this.operator = operator;
    }

    public Expression getLeftHandSide() {
        return leftHandSide;
    }

    public Expression getRightHandSide() {
        return rightHandSide;
    }

    public boolean isMultiply() {
        return operator.getType() == TokenType.STAR;
    }

    public boolean isBool() {
        return operator.getType() == TokenType.AND || operator.getType() == TokenType.OR;
    }

    public boolean isAnd() {
        return operator.getType() == TokenType.AND;
    }

    @Override
    public String toString() {
        return super.toString() + "[" + operator.getStringValue() + "]";
    }

    @Override
    public void validate(SymbolTable symbolTable) {
        leftHandSide.validate(symbolTable);
        rightHandSide.validate(symbolTable);
        if (isBool()) {
            if (!leftHandSide.getType().equals(CatscriptType.BOOLEAN) && !rightHandSide.getType().equals(CatscriptType.BOOLEAN)) {
                addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        } else {
            if (!leftHandSide.getType().equals(CatscriptType.INT)) {
                leftHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
            if (!rightHandSide.getType().equals(CatscriptType.INT)) {
                rightHandSide.addError(ErrorType.INCOMPATIBLE_TYPES);
            }
        }
    }

    @Override
    public CatscriptType getType() {
        if (isBool()) {
            return CatscriptType.BOOLEAN;
        }
        return CatscriptType.INT;
    }

    //==============================================================
    // Implementation
    //==============================================================

    @Override
    public Object evaluate(CatscriptRuntime runtime) {
        return super.evaluate(runtime);
    }

    @Override
    public void transpile(StringBuilder javascript) {
        super.transpile(javascript);
    }

    @Override
    public void compile(ByteCodeGenerator code) {
        super.compile(code);
    }


}
