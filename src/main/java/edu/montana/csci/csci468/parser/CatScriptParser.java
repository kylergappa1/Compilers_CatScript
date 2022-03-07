package edu.montana.csci.csci468.parser;

import edu.montana.csci.csci468.parser.expressions.*;
import edu.montana.csci.csci468.parser.statements.*;
import edu.montana.csci.csci468.tokenizer.CatScriptTokenizer;
import edu.montana.csci.csci468.tokenizer.Token;
import edu.montana.csci.csci468.tokenizer.TokenList;
import edu.montana.csci.csci468.tokenizer.TokenType;

import java.util.ArrayList;
import java.util.List;

import static edu.montana.csci.csci468.tokenizer.TokenType.*;

public class CatScriptParser {

    private TokenList tokens;
    private FunctionDefinitionStatement currentFunctionDefinition;

    public CatScriptProgram parse(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();

        // first parse an expression
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = null;
        try {
            expression = parseExpression();
        } catch(RuntimeException re) {
            // ignore :)
        }
        if (expression == null || tokens.hasMoreTokens()) {
            tokens.reset();
            while (tokens.hasMoreTokens()) {
                program.addStatement(parseProgramStatement());
            }
        } else {
            program.setExpression(expression);
        }

        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    public CatScriptProgram parseAsExpression(String source) {
        tokens = new CatScriptTokenizer(source).getTokens();
        CatScriptProgram program = new CatScriptProgram();
        program.setStart(tokens.getCurrentToken());
        Expression expression = parseExpression();
        program.setExpression(expression);
        program.setEnd(tokens.getCurrentToken());
        return program;
    }

    //============================================================
    //  Statements
    //============================================================

    private Statement parseProgramStatement() {
        Statement printStmt = parsePrintStatement();
        if (printStmt != null) {
            return printStmt;
        }
        return new SyntaxErrorStatement(tokens.consumeToken());
    }

    private Statement parsePrintStatement() {
        if (tokens.match(PRINT)) {

            PrintStatement printStatement = new PrintStatement();
            printStatement.setStart(tokens.consumeToken());

            require(LEFT_PAREN, printStatement);
            printStatement.setExpression(parseExpression());
            printStatement.setEnd(require(RIGHT_PAREN, printStatement));

            return printStatement;
        } else {
            return null;
        }
    }

    //============================================================
    //  Expressions
    //============================================================

    private Expression parseExpression() {
        return parseEqualityExpression();
    }

    private Expression parseAdditiveExpression() {
        Expression expression = parseFactorExpression();
        while (tokens.match(PLUS, MINUS)) {
            Token operator = tokens.consumeToken();
            Expression rightHandSide = (tokens.matchAndConsume(PLUS, MINUS)) ? new IntegerLiteralExpression("1") : parseFactorExpression();
            AdditiveExpression additiveExpression = new AdditiveExpression(operator, expression, rightHandSide);
            additiveExpression.setStart(expression.getStart());
            additiveExpression.setEnd(rightHandSide.getEnd());
            additiveExpression.setToken(operator);
            expression = additiveExpression;
        }
        return expression;
    }

    private Expression parseFactorExpression() {
        Expression expression = parseUnaryExpression();
        while (tokens.match(SLASH, STAR, OR, AND)) {
            Token operator = tokens.consumeToken();
            final Expression rightHandSide = parseUnaryExpression();
            FactorExpression factorExpression = new FactorExpression(operator, expression, rightHandSide);
            factorExpression.setStart(expression.getStart());
            factorExpression.setEnd(rightHandSide.getEnd());
            factorExpression.setToken(operator);
            expression = factorExpression;
        }
        return expression;

    }

    private Expression parseEqualityExpression() {
        Expression lhs = parseComparisonExpression();
        if (tokens.match(EQUAL_EQUAL, BANG_EQUAL)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseEqualityExpression();
            rhs.setToken(token);
            return new EqualityExpression(token, lhs, rhs);
        } else {
            return lhs;
        }
    }

    private Expression parseComparisonExpression() {
        Expression lhs = parseAdditiveExpression();
        if (tokens.match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseAdditiveExpression();
            rhs.setToken(token);
            return new ComparisonExpression(token, lhs, rhs);
        } else {
            return lhs;
        }
    }

    private Expression parseUnaryExpression() {
        if (tokens.match(MINUS, NOT)) {
            Token token = tokens.consumeToken();
            Expression rhs = parseUnaryExpression();
            UnaryExpression unaryExpression = new UnaryExpression(token, rhs);
            unaryExpression.setStart(token);
            unaryExpression.setEnd(rhs.getEnd());
            return unaryExpression;
        } else {
            return parsePrimaryExpression();
        }
    }

    private FunctionCallExpression parseFunctionExpression(Token start) {
        List<Expression> argumentList = new ArrayList<>();
        boolean terminated = true;
        tokens.consumeToken();
        if (!tokens.matchAndConsume(RIGHT_PAREN)) {
            argumentList.add(parseExpression());
            while (tokens.matchAndConsume(COMMA)) {
                argumentList.add(parseExpression());
            }
            terminated = false;
        }
        FunctionCallExpression functionCallExpression = new FunctionCallExpression(start.getStringValue(), argumentList);
        functionCallExpression.setToken(start);
        if (!terminated) {
            require(RIGHT_PAREN, functionCallExpression, ErrorType.UNTERMINATED_ARG_LIST);
        }
        return functionCallExpression;
    }

    // With enough if statements i can do anything
    private Expression parsePrimaryExpression() {
        Token token;
        if (tokens.match(INTEGER)) {
            token = tokens.consumeToken();
            IntegerLiteralExpression integerExpression = new IntegerLiteralExpression(token.getStringValue());
            integerExpression.setToken(token);
            return integerExpression;
        } else if (tokens.match(IDENTIFIER)) {
            token = tokens.consumeToken();
            if (tokens.match(LEFT_PAREN)) {
                return parseFunctionExpression(token);
            }
            if (tokens.match(LEFT_BRACKET)) {
                return parseIndexExpression(token);
            }
            if (tokens.match(QUESTION)) {
                return parseNullCheckExpression(token);
            } else {
                IdentifierExpression identifierExpression = new IdentifierExpression(token.getStringValue());
                identifierExpression.setToken(token);
                return identifierExpression;
            }
        } else if (tokens.match(STRING)) {
            token = tokens.consumeToken();
            StringLiteralExpression stringExpression = new StringLiteralExpression(token.getStringValue());
            stringExpression.setToken(token);
            return stringExpression;
        } else if (tokens.match(TRUE)) {
            token = tokens.consumeToken();
            BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(true);
            booleanExpression.setToken(token);
            return booleanExpression;
        } else if (tokens.match(FALSE)) {
            token = tokens.consumeToken();
            BooleanLiteralExpression booleanExpression = new BooleanLiteralExpression(false);
            booleanExpression.setToken(token);
            return booleanExpression;
        } else if (tokens.match(NULL)) {
            token = tokens.consumeToken();
            NullLiteralExpression nullLiteralExpression = new NullLiteralExpression();
            nullLiteralExpression.setToken(token);
            return nullLiteralExpression;
        } else if (tokens.matchAndConsume(LEFT_BRACKET)) {
            List<Expression> listArguments = new ArrayList<>(0);
            boolean terminated = true;
            if (!tokens.matchAndConsume(RIGHT_BRACKET)) {
                listArguments.add(parseExpression());
                while (tokens.matchAndConsume(COMMA)) {
                    listArguments.add(parseExpression());
                }
                terminated = false;
            }
            ListLiteralExpression listLiteralExpression = new ListLiteralExpression(listArguments);
            if (!terminated) {
                require(RIGHT_BRACKET, listLiteralExpression, ErrorType.UNTERMINATED_LIST);
            }
            return listLiteralExpression;
        } else if (tokens.matchAndConsume(LEFT_PAREN)) {
            Expression expression = parseExpression();
            ParenthesizedExpression parenthesizedExpression = new ParenthesizedExpression(expression);
            require(RIGHT_PAREN, parenthesizedExpression);
            if(tokens.matchAndConsume(QUESTION)){
                return parseTernaryExpression(parenthesizedExpression);
            }
            return parenthesizedExpression;
        }
        return new SyntaxErrorExpression(tokens.consumeToken());
    }

    private Expression parseTernaryExpression(ParenthesizedExpression parenthesizedExpression) {
        TernaryExpression ternaryExpression = new TernaryExpression();
        ternaryExpression.setParenthesizedExpression(parenthesizedExpression);
        ternaryExpression.setTrueExpression(parseExpression());
        require(COLON, ternaryExpression);
        ternaryExpression.setFalseExpression(parseExpression());
        return ternaryExpression;
    }

    private Expression parseNullCheckExpression(Token variableName) {
        NullCheckExpression nullCheckExpression = new NullCheckExpression();
        nullCheckExpression.setVariable(variableName);
        nullCheckExpression.setStart(variableName);
        nullCheckExpression.setEnd(tokens.consumeToken());
        return nullCheckExpression;
    }

    private Expression parseIndexExpression(Token variableName) {
        IndexExpression indexExpression = new IndexExpression();
        indexExpression.setStart(tokens.getCurrentToken());
        ArrayList<Expression> indexes = new ArrayList<>();
        while(tokens.matchAndConsume(LEFT_BRACKET) && tokens.hasMoreTokens()){
            indexes.add(parsePrimaryExpression());
            require(RIGHT_BRACKET, indexExpression);
        }
        indexExpression.setVariableName(variableName.getStringValue());
        indexExpression.setIndexes(indexes);
        return indexExpression;
    }

    //============================================================
    //  Parse Helpers
    //============================================================
    private Token require(TokenType type, ParseElement elt) {
        return require(type, elt, ErrorType.UNEXPECTED_TOKEN);
    }

    private Token require(TokenType type, ParseElement elt, ErrorType msg) {
        if(tokens.match(type)){
            return tokens.consumeToken();
        } else {
            elt.addError(msg, tokens.getCurrentToken());
            return tokens.getCurrentToken();
        }
    }

}
