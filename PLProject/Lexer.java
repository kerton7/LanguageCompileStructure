package PLProject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Lexer {

    private final String source;
    private int currentPosition = 0;
    private int currentLine = 0;  
    private int currentColumn = 0; 

    private final List<Token> tokens = new ArrayList<>();
    private static final Map<String, TokenType> KEYWORDS = new HashMap<>();

    static {
        KEYWORDS.put("yaz", TokenType.TOKEN_KEYWORD);
        KEYWORDS.put("her", TokenType.TOKEN_KEYWORD);
        KEYWORDS.put("sürece", TokenType.TOKEN_KEYWORD);
        KEYWORDS.put("eğer", TokenType.TOKEN_KEYWORD);
        KEYWORDS.put("değilse", TokenType.TOKEN_KEYWORD);
    }

    public Lexer(String source) {
        this.source = source;
    }

    private char advance() {
        if (currentPosition >= source.length()) {
            return '\0';
        }
        char c = source.charAt(currentPosition++);
        if (c == '\n') {
            currentLine++;
            currentColumn = 0;
        } else {
            currentColumn++;
        }
        return c;
    }

    private char peek() {
        if (currentPosition >= source.length()) {
            return '\0';
        }
        return source.charAt(currentPosition);
    }

    private char peekNext(int offset) {
        if (currentPosition + offset >= source.length()) {
            return '\0';
        }
        return source.charAt(currentPosition + offset);
    }

    private void skipWhitespace() {
        while (Character.isWhitespace(peek())) {
            advance();
        }
    }

    public List<Token> lex() {
        while (peek() != '\0') {
            skipWhitespace();

            if (peek() == '\0') break; 

            int tokenStartLine = currentLine;
            int tokenStartColumn = currentColumn;
            char currentChar = peek();

            if (currentChar == '?') {
                advance(); 
                tokens.add(new Token(TokenType.TOKEN_QUESTION_MARK, "?", tokenStartLine, tokenStartColumn));
                continue; 
            }

            if (Character.isLetter(currentChar)) {
                tokens.add(handleIdentifierOrKeyword(tokenStartLine, tokenStartColumn));
                continue;
            }

            if (Character.isDigit(currentChar)) {
                tokens.add(handleNumber(tokenStartLine, tokenStartColumn));
                continue;
            }

            if (currentChar == '"') {
                tokens.add(handleStringLiteral(tokenStartLine, tokenStartColumn));
                continue;
            }

            if (isCompoundOperatorStart(currentChar)) {
                tokens.add(handleOperator(tokenStartLine, tokenStartColumn));
                continue;
            }

            if (isSingleCharOperator(currentChar)) {
                advance();
                tokens.add(new Token(TokenType.TOKEN_OPERATOR, String.valueOf(currentChar), tokenStartLine, tokenStartColumn));
                continue;
            }

            if (isSeparator(currentChar)) {
                advance();
                tokens.add(new Token(TokenType.TOKEN_SEPARATOR, String.valueOf(currentChar), tokenStartLine, tokenStartColumn));
                continue;
            }

            throw new RuntimeException(
                "Leksik Analiz Hatası: Hatalı karakter: '" + currentChar + "' konumunda " +
                (tokenStartLine + 1) + ". satır, " + (tokenStartColumn + 1) + ". sütun."
            );
        }

        tokens.add(new Token(TokenType.TOKEN_EOF, null, currentLine, currentColumn));
        return tokens;
    }

    private Token handleIdentifierOrKeyword(int line, int column) {
        int start = currentPosition;

        while (currentPosition < source.length() && Character.isLetterOrDigit(source.charAt(currentPosition))) {
            advance();
        }
        String value = source.substring(start, currentPosition);
        TokenType type = KEYWORDS.getOrDefault(value, TokenType.TOKEN_IDENTIFIER);
        return new Token(type, value, line, column);
    }

    private Token handleNumber(int line, int column) {
        int start = currentPosition;
        while (currentPosition < source.length() && Character.isDigit(source.charAt(currentPosition))) {
            advance();
        }
        String value = source.substring(start, currentPosition);
        return new Token(TokenType.TOKEN_INTEGER_LITERAL, value, line, column);
    }

    private Token handleStringLiteral(int line, int column) {
        advance(); 
        int start = currentPosition;
        while (peek() != '"' && peek() != '\n' && peek() != '\0') {
            advance();
        }
        if (peek() == '"') {
            String value = source.substring(start, currentPosition);
            advance(); 
            return new Token(TokenType.TOKEN_STRING_LITERAL, value, line, column);
        } else {
            throw new RuntimeException("Lexer Hatası: Kapanış tırnak işareti bekleniyordu. Konum: Satır " + (line + 1) + ", Sütun " + (column + 1));
        }
    }

    private Token handleOperator(int line, int column) {
        char firstChar = advance();

        if (peek() != '\0') {
            char nextChar = peek();
            String twoCharOp = "" + firstChar + nextChar;

            switch (twoCharOp) {
                case "==":
                case "!=":
                case "<=":
                case ">=":
                    advance();
                    return new Token(TokenType.TOKEN_OPERATOR, twoCharOp, line, column);
            }
        }
        return new Token(TokenType.TOKEN_OPERATOR, String.valueOf(firstChar), line, column);
    }

    private boolean isCompoundOperatorStart(char c) {
        return c == '=' || c == '!' || c == '<' || c == '>';
    }

    private boolean isSingleCharOperator(char c) {
        return c == '+' || c == '-' || c == '*' || c == '/';
    }

    private boolean isSeparator(char c) {
        return c == '(' || c == ')' || c == '{' || c == '}' || c == ';';
    }
}