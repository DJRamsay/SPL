package parser;

import java.util.List;

import lexer.Token;
import lexer.TokenType;

public class SyntaxTreeBuilder {
    private final List<Token> tokens;
    private int current = 0;

    public SyntaxTreeBuilder(List<Token> tokens) {
        this.tokens = tokens;
    }

    public SyntaxNode build() {
        SyntaxNode root = new SyntaxNode("Program");
        while (!isAtEnd()) {
            // skip stray EOF leaf if present
            if (peek().getType() == TokenType.EOF) break;
            root.addChild(parseElement());
        }
        return root;
    }

    private SyntaxNode parseElement() {
        Token t = peek();

        if (t.getType() == TokenType.LBRACE) {
            advance(); // consume '{'
            SyntaxNode block = new SyntaxNode("{");
            while (!isAtEnd() && peek().getType() != TokenType.RBRACE) {
                block.addChild(parseElement());
            }
            if (!isAtEnd() && peek().getType() == TokenType.RBRACE) {
                block.addChild(new SyntaxNode("}"));
                advance(); // consume '}'
            }
            return block;
        }

        if (t.getType() == TokenType.LPAREN) {
            advance(); // consume '('
            SyntaxNode par = new SyntaxNode("(");
            while (!isAtEnd() && peek().getType() != TokenType.RPAREN) {
                par.addChild(parseElement());
            }
            if (!isAtEnd() && peek().getType() == TokenType.RPAREN) {
                par.addChild(new SyntaxNode(")"));
                advance(); // consume ')'
            }
            return par;
        }

        if (isKeywordOrName(t.getType())) {
            Token head = advance();
            SyntaxNode node = new SyntaxNode(head.getLexeme() + " (" + head.getType() + ")");
            if (!isAtEnd() && (peek().getType() == TokenType.LBRACE || peek().getType() == TokenType.LPAREN)) {
                node.addChild(parseElement());
            }
            return node;
        }

        Token leaf = advance();
        String label = leaf.getLexeme().isEmpty() ? leaf.getType().name() : leaf.getLexeme() + " (" + leaf.getType() + ")";
        return new SyntaxNode(label);
    }

    private boolean isKeywordOrName(TokenType t) {
        switch (t) {
            case GLOB:
            case PROC:
            case FUNC:
            case MAIN:
            case VAR:
            case LOCAL:
            case IF:
            case ELSE:
            case WHILE:
            case DO:
            case UNTIL:
            case PRINT:
            case HALT:
            case RETURN:
            case IDENTIFIER:
            case NUMBER:
            case STRING:
                return true;
            default:
                return false;
        }
    }

    private boolean isAtEnd() {
        return peek().getType() == TokenType.EOF;
    }

    private Token peek() {
        return tokens.get(current);
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return tokens.get(current - 1);
    }
}
