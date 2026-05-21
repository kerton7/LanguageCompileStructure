package PLProject;

import java.util.List;
import java.util.Map;
import java.util.HashMap;

public class Parser {

    private final List<Token> tokens;
    private int currentTokenIndex = 0;
    private final Map<String, Object> environment = new HashMap<>();

    public Parser(List<Token> tokens) {
        this.tokens = tokens;
    }

    private Token currentToken() {
        if (currentTokenIndex < tokens.size()) {
            return tokens.get(currentTokenIndex);
        }
        return tokens.get(tokens.size() - 1);
    }

    private Token peekNextToken() {
        if (currentTokenIndex + 1 < tokens.size()) {
            return tokens.get(currentTokenIndex + 1);
        }
        return tokens.get(tokens.size() - 1); 
    }

    private Token consumeToken() {
        Token token = currentToken();
        if (token.type != TokenType.TOKEN_EOF) {
            currentTokenIndex++;
        }
        return token;
    }

    private Token match(TokenType expectedType) {
        Token token = currentToken();
        if (token.type == expectedType) {
            return consumeToken();
        }
        throw new RuntimeException("Parse Hatası: Beklenen token -> " + expectedType
                + "ama bulunan token -> " + token.type + " ('" + token.value + "') konumunda " + (token.line + 1) + ". satır, " + (token.column + 1) + ". sütun.");
    }

    private Token match(TokenType expectedType, String expectedValue) {
        Token token = currentToken();
        if (token.type == expectedType && token.value != null && token.value.equals(expectedValue)) {
            return consumeToken();
        }
        throw new RuntimeException("Syntax Hatası: Beklenen token -> " + "' "+ expectedValue+ " ' "
                + "ama bulunan token -> " + "' " + token.value + " ' " + (token.line + 1) + ". satır, " + (token.column + 1) + ". sütun.");
    }

    public void parse() {
        while (currentToken().type != TokenType.TOKEN_EOF) {
            parseStatement();
        }
    }

    private void parseStatement() {
        Token token = currentToken();
        if (token.type == TokenType.TOKEN_KEYWORD) {
            switch (token.value) {
                case "yaz":
                    parseYazStatement(); 
                    break;
                case "her":
                    parseForLoopStatement();
                    return;
                case "sürece":
                    parseWhileLoopStatement();
                    return;
                case "eğer":
                    parseIfStatement();
                    return;
                default:
                    throw new RuntimeException("Parse Hatası: Bilinmeyen veya yanlış yerleştirilmiş keyword: " + token.value + (token.line + 1) + ". satır, " + (token.column + 1) + ". sütun.");
            }
        } else if (token.type == TokenType.TOKEN_IDENTIFIER) {
            if (peekNextToken().type == TokenType.TOKEN_OPERATOR && peekNextToken().value.equals("=")) {
                parseAssignmentStatement(); // Bu normal atama deyimi, ? bekleyecek
            } else {
                System.out.println("Expression statement olarak yorumlandı:");
                evaluateExpression();
                requireStatementTerminator("ifade");
            }
        } else {
            throw new RuntimeException("Parse Hatası: Beklenmedik token, statement başlangıcında: " + token.type + " ('" + token.value + "') konumunda " + (token.line + 1) + ". satır, " + (token.column + 1) + ". sütun.");
        }
    }

    private void requireStatementTerminator(String statementType) {
        Token current = currentToken();
        if (current.type == TokenType.TOKEN_QUESTION_MARK) { 
            consumeToken(); 
        } else {
            throw new RuntimeException("Syntax Hatası: " + statementType + " statement sonunda sadece '?' bekleniyordu ama bulundu: " + current.type + " ('" + current.value + "') " + (current.line + 1) + ". satır, " + (current.column + 1) + ". sütun.");
        }
    }

    private void parseYazStatement() {
        System.out.println("Parsing 'Yaz'statement");
        match(TokenType.TOKEN_KEYWORD, "yaz");
        match(TokenType.TOKEN_SEPARATOR, "(");

        Object valueToPrint = evaluateExpression();
        System.out.println("\n>>> Çıktı: " + valueToPrint);
        System.out.println();

        match(TokenType.TOKEN_SEPARATOR, ")");
        requireStatementTerminator("yaz");
    }

    private void parseYazStatementInsideLoop() {
        System.out.println("Parsing döngü içindeki 'yaz' statement");
        match(TokenType.TOKEN_KEYWORD, "yaz");
        match(TokenType.TOKEN_SEPARATOR, "(");

        Object valueToPrint = evaluateExpression();
        System.out.println("\n>>> Çıktı: " + valueToPrint);
        System.out.println();
        match(TokenType.TOKEN_SEPARATOR, ")");
        }

    private void parseAssignmentStatement() {
        System.out.println("Parsing 'Atama' statement");
        Token identifier = match(TokenType.TOKEN_IDENTIFIER);
        match(TokenType.TOKEN_OPERATOR, "=");

        Object evaluatedValue = evaluateExpression();

        environment.put(identifier.value, evaluatedValue);
        System.out.println();
        System.out.printf(">>> Atama: %s = %s%n", identifier.value, evaluatedValue);
        System.out.println();
        requireStatementTerminator("atama"); 
    }

    private void parseAssignmentStatementInsideLoop() {
        System.out.println("Parsing döngü içindeki 'atama' statement");
        Token identifier = match(TokenType.TOKEN_IDENTIFIER);
        match(TokenType.TOKEN_OPERATOR, "=");

        Object evaluatedValue = evaluateExpression();

        environment.put(identifier.value, evaluatedValue);
        System.out.println();
        System.out.printf(">>> Atama: %s = %s%n", identifier.value, evaluatedValue);
        System.out.println();
    }

    private void parseForLoopStatement() {
    System.out.println("Parsing 'Her' statement");
    match(TokenType.TOKEN_KEYWORD, "her");
    match(TokenType.TOKEN_SEPARATOR, "(");

    System.out.println("  For döngüsü başlığı - başlangıç ataması:");
    parseAssignmentStatementNoSemicolon();
    match(TokenType.TOKEN_SEPARATOR, ";");

    int conditionStartTokenIndex = currentTokenIndex;
    parseConditionStructure(); 
    match(TokenType.TOKEN_SEPARATOR, ";");

    int incrementStartTokenIndex = currentTokenIndex;
    // Increment tokenlarını değerlendirmeden atla (ilk iterasyonda erken çalışmasını engelle)
    while (!(currentToken().type == TokenType.TOKEN_SEPARATOR && currentToken().value.equals(")"))) {
        if (currentToken().type == TokenType.TOKEN_EOF) {
            throw new RuntimeException("For döngüsü başlığında beklenmedik dosya sonu.");
        }
        consumeToken();
    }
    match(TokenType.TOKEN_SEPARATOR, ")");

    match(TokenType.TOKEN_SEPARATOR, "{");
    int loopBodyStartTokenIndex = currentTokenIndex;

    while (true) {
        int tempCurrentIndex = currentTokenIndex;
        currentTokenIndex = conditionStartTokenIndex;
        boolean conditionResult = evaluateCondition();
        currentTokenIndex = tempCurrentIndex;

        if (!conditionResult) {
            currentTokenIndex = loopBodyStartTokenIndex; 
            int braceDepth = 1;
            while (braceDepth > 0 && currentToken().type != TokenType.TOKEN_EOF) {
                Token t = currentToken();
                if (t.type == TokenType.TOKEN_SEPARATOR) {
                    if (t.value.equals("{")) {
                        braceDepth++;
                    } else if (t.value.equals("}")) {
                        braceDepth--;
                    }
                }
                if (braceDepth > 0) {
                    consumeToken();
                }
            }
            break;
        }
        currentTokenIndex = loopBodyStartTokenIndex;
        while (!(currentToken().type == TokenType.TOKEN_SEPARATOR && currentToken().value.equals("}"))) {
            if (currentToken().type == TokenType.TOKEN_EOF) {
                throw new RuntimeException("For döngü bloğu içinde beklenmedik dosya sonu.");
            }
            
            Token innerToken = currentToken();
            if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("yaz")) {
                parseYazStatementInsideLoop();
            } else if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("eğer")) {
                parseIfStatement();
            } else if (innerToken.type == TokenType.TOKEN_IDENTIFIER && peekNextToken().type == TokenType.TOKEN_OPERATOR && peekNextToken().value.equals("=")) {
                parseAssignmentStatementInsideLoop();
            } else {
                throw new RuntimeException("For döngüsü içinde desteklenmeyen deyim tipi: " + innerToken.value);
            }
        }
        tempCurrentIndex = currentTokenIndex;
        currentTokenIndex = incrementStartTokenIndex;
        parseAssignmentStatementNoSemicolon();
        currentTokenIndex = tempCurrentIndex;
    }
    match(TokenType.TOKEN_SEPARATOR, "}");
}

    private void parseAssignmentStatementNoSemicolon() {
        Token identifier = match(TokenType.TOKEN_IDENTIFIER);
        match(TokenType.TOKEN_OPERATOR, "=");
        Object evaluatedValue = evaluateExpression();
        environment.put(identifier.value, evaluatedValue);
        System.out.printf("  >>> Döngü Başlığı Ataması: %s = %s%n", identifier.value, evaluatedValue);
    }

    private void parseWhileLoopStatement() {
    System.out.println("Parsing 'Sürece' statement");
    match(TokenType.TOKEN_KEYWORD, "sürece");
    match(TokenType.TOKEN_SEPARATOR, "(");

    int conditionStartTokenIndex = currentTokenIndex;
    parseConditionStructure();
    match(TokenType.TOKEN_SEPARATOR, ")");

    match(TokenType.TOKEN_SEPARATOR, "{");
    int loopBodyStartTokenIndex = currentTokenIndex;

    while (true) {
        int tempCurrentIndex = currentTokenIndex;
        currentTokenIndex = conditionStartTokenIndex;
        boolean conditionResult = evaluateCondition();
        currentTokenIndex = tempCurrentIndex;

        if (!conditionResult) {
            currentTokenIndex = loopBodyStartTokenIndex;
            int braceDepth = 1;
            while (braceDepth > 0 && currentToken().type != TokenType.TOKEN_EOF) {
                Token t = currentToken();
                if (t.type == TokenType.TOKEN_SEPARATOR) {
                    if (t.value.equals("{")) {
                        braceDepth++;
                    } else if (t.value.equals("}")) {
                        braceDepth--;
                    }
                }
                if (braceDepth > 0) {
                    consumeToken();
                }
            }
            break;
        }
        currentTokenIndex = loopBodyStartTokenIndex;

        while (!(currentToken().type == TokenType.TOKEN_SEPARATOR && currentToken().value.equals("}"))) {
            if (currentToken().type == TokenType.TOKEN_EOF) {
                throw new RuntimeException("While döngü bloğu içinde beklenmedik dosya sonu.");
            }
            
            Token innerToken = currentToken();
            if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("yaz")) {
                parseYazStatementInsideLoop();
            } else if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("eğer")) {
                parseIfStatement();
            } else if (innerToken.type == TokenType.TOKEN_IDENTIFIER && peekNextToken().type == TokenType.TOKEN_OPERATOR && peekNextToken().value.equals("=")) {
                parseAssignmentStatementInsideLoop();
            } else {
                throw new RuntimeException("While döngüsü içinde desteklenmeyen deyim tipi: " + innerToken.value);
            }
        }
    }
    match(TokenType.TOKEN_SEPARATOR, "}"); 
}

    private void parseIfStatement() {
        System.out.println("Parsing 'Eğer' statement");
        match(TokenType.TOKEN_KEYWORD, "eğer");
        match(TokenType.TOKEN_SEPARATOR, "(");

        boolean conditionResult = evaluateCondition();

        match(TokenType.TOKEN_SEPARATOR, ")");
        match(TokenType.TOKEN_SEPARATOR, "{");

        if (conditionResult) {
            System.out.println("  Koşul DOĞRU — eğer bloğu çalıştırılıyor.");
            while (!(currentToken().type == TokenType.TOKEN_SEPARATOR && currentToken().value.equals("}"))) {
                if (currentToken().type == TokenType.TOKEN_EOF) {
                    throw new RuntimeException("Eğer bloğu içinde beklenmedik dosya sonu.");
                }
                parseBlockInnerStatement();
            }
            match(TokenType.TOKEN_SEPARATOR, "}");

            if (currentToken().type == TokenType.TOKEN_KEYWORD && currentToken().value.equals("değilse")) {
                System.out.println("  Koşul DOĞRU olduğu için değilse bloğu atlanıyor.");
                consumeToken();
                match(TokenType.TOKEN_SEPARATOR, "{");
                skipBlock();
                match(TokenType.TOKEN_SEPARATOR, "}");
            }
        } else {
            System.out.println("  Koşul YANLIŞ — eğer bloğu atlanıyor.");
            skipBlock();
            match(TokenType.TOKEN_SEPARATOR, "}");

            if (currentToken().type == TokenType.TOKEN_KEYWORD && currentToken().value.equals("değilse")) {
                System.out.println("  Koşul YANLIŞ — değilse bloğu çalıştırılıyor.");
                consumeToken();
                match(TokenType.TOKEN_SEPARATOR, "{");
                while (!(currentToken().type == TokenType.TOKEN_SEPARATOR && currentToken().value.equals("}"))) {
                    if (currentToken().type == TokenType.TOKEN_EOF) {
                        throw new RuntimeException("Değilse bloğu içinde beklenmedik dosya sonu.");
                    }
                    parseBlockInnerStatement();
                }
                match(TokenType.TOKEN_SEPARATOR, "}");
            }
        }
    }

    private void skipBlock() {
        int braceDepth = 1;
        while (braceDepth > 0 && currentToken().type != TokenType.TOKEN_EOF) {
            Token t = currentToken();
            if (t.type == TokenType.TOKEN_SEPARATOR) {
                if (t.value.equals("{")) braceDepth++;
                else if (t.value.equals("}")) braceDepth--;
            }
            if (braceDepth > 0) consumeToken();
        }
    }

    private void parseBlockInnerStatement() {
        Token innerToken = currentToken();
        if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("yaz")) {
            parseYazStatementInsideLoop();
        } else if (innerToken.type == TokenType.TOKEN_KEYWORD && innerToken.value.equals("eğer")) {
            parseIfStatement();
        } else if (innerToken.type == TokenType.TOKEN_IDENTIFIER && peekNextToken().type == TokenType.TOKEN_OPERATOR && peekNextToken().value.equals("=")) {
            parseAssignmentStatementInsideLoop();
        } else {
            throw new RuntimeException("Blok içinde desteklenmeyen deyim tipi: " + innerToken.value
                    + " konumunda " + (innerToken.line + 1) + ". satır, " + (innerToken.column + 1) + ". sütun.");
        }
    }

    private boolean evaluateCondition() {
        System.out.println("  Condition değerlendiriliyor...");
        Object left = evaluateExpression();
        Token operatorToken = currentToken();

        if (operatorToken.type == TokenType.TOKEN_OPERATOR
                && (operatorToken.value.equals("<") || operatorToken.value.equals(">")
                || operatorToken.value.equals("==") || operatorToken.value.equals("!=")
                || operatorToken.value.equals("<=") || operatorToken.value.equals(">="))) {
            String operator = consumeToken().value;
            Object right = evaluateExpression();

            if (left instanceof Integer && right instanceof Integer) {
                int l = (int) left;
                int r = (int) right;
                switch (operator) {
                    case "<": return l < r;
                    case ">": return l > r;
                    case "==": return l == r;
                    case "!=": return l != r;
                    case "<=": return l <= r;
                    case ">=": return l >= r;
                    default:
                        throw new RuntimeException("Bilinmeyen karşılaştırma operatörü: " + operator + " konumunda " + (operatorToken.line + 1) + ". satır, " + (operatorToken.column + 1) + ". sütun.");
                }
            } else {
                throw new RuntimeException("Runtime Hatası: Karşılaştırma için desteklenmeyen türler: " + left.getClass().getSimpleName() + " ve " + right.getClass().getSimpleName() + " konumunda " + (operatorToken.line + 1) + ". satır, " + (operatorToken.column + 1) + ". sütun.");
            }
        } else {
            throw new RuntimeException("Syntax Hatası: Koşulda karşılaştırma operatörü bekleniyordu, bulundu: " + operatorToken.type + " ('" + operatorToken.value + "') konumunda " + (operatorToken.line + 1) + ". satır, " + (operatorToken.column + 1) + ". sütun.");
        }
    }

    private void parseConditionStructure() {
        System.out.println("  Condition yapısı ayrıştırılıyor (sadece ilerleme için)...");
        parseExpression();
        Token operatorToken = currentToken();

        if (operatorToken.type == TokenType.TOKEN_OPERATOR
                && (operatorToken.value.equals("<") || operatorToken.value.equals(">")
                || operatorToken.value.equals("==") || operatorToken.value.equals("!=")
                || operatorToken.value.equals("<=") || operatorToken.value.equals(">="))) {
            consumeToken();
            parseExpression();
        } else {
            throw new RuntimeException("Syntax Hatası: Koşul yapısı ayrıştırılamadı, geçerli bir karşılaştırma operatörü bekleniyordu. Konum: " + (operatorToken.line + 1) + ". satır, " + (operatorToken.column + 1) + ". sütun.");
        }
    }

    private Object evaluateExpression() {
        System.out.println("  Expression değerlendiriliyor..");
        Object left = evaluateTerm();
        while (currentToken().type == TokenType.TOKEN_OPERATOR
                && (currentToken().value.equals("+") || currentToken().value.equals("-"))) {
            String operator = consumeToken().value;
            Object right = evaluateTerm();

            if (left instanceof Integer && right instanceof Integer) {
                int l = (int) left;
                int r = (int) right;
                switch (operator) {
                    case "+":
                        left = l + r;
                        break;
                    case "-":
                        left = l - r;
                        break;
                }
            } else if (left instanceof String || right instanceof String) {
                if (operator.equals("+")) {
                    left = String.valueOf(left) + String.valueOf(right);
                } else {
                    throw new RuntimeException("Runtime Hatası: Metinsel ifadelerle desteklenmeyen operatör: " + operator + " konumunda " + (currentToken().line + 1) + ". satır, " + (currentToken().column + 1) + ". sütun.");
                }
            } else {
                throw new RuntimeException("Runtime Hatası: Desteklenmeyen ifade türleri. Konum: " + (currentToken().line + 1) + ". satır, " + (currentToken().column + 1) + ". sütun.");
            }
        }
        return left;
    }

    private void parseExpression() {
        evaluateExpression();
    }

    private Object evaluateTerm() {
        System.out.println("    Term değerlendiriliyor...");
        Object left = evaluateFactor();
        while (currentToken().type == TokenType.TOKEN_OPERATOR
                && (currentToken().value.equals("*") || currentToken().value.equals("/"))) {
            String operator = consumeToken().value;
            Object right = evaluateFactor();

            if (left instanceof Integer && right instanceof Integer) {
                int l = (int) left;
                int r = (int) right;
                switch (operator) {
                    case "*":
                        left = l * r;
                        break;
                    case "/":
                        if (r == 0) {
                            throw new RuntimeException("Runtime Hatası: Sıfıra bölme hatası. Konum: " + (currentToken().line + 1) + ". satır, " + (currentToken().column + 1) + ". sütun.");
                        }
                        left = (double)l / r;
                        break;
                }
            } else {
                throw new RuntimeException("Runtime Hatası: Desteklenmeyen ifade türleri. Konum: " + (currentToken().line + 1) + ". satır, " + (currentToken().column + 1) + ". sütun.");
            }
        }
        return left;
    }

    private Object evaluateFactor() {
        System.out.println("      Factor değerlendiriliyor...");
        Token t = currentToken();
        if (t.type == TokenType.TOKEN_IDENTIFIER) {
            consumeToken();
            if (environment.containsKey(t.value)) {
                return environment.get(t.value);
            } else {
                throw new RuntimeException("Runtime Hatası: Tanımlanmamış değişken: " + t.value + " konumunda " + (t.line + 1) + ". satır, " + (t.column + 1) + ". sütun.");
            }
        } else if (t.type == TokenType.TOKEN_INTEGER_LITERAL) {
            consumeToken();
            return Integer.parseInt(t.value);
        } else if (t.type == TokenType.TOKEN_STRING_LITERAL) {
            consumeToken();
            return t.value;
        } else if (t.type == TokenType.TOKEN_SEPARATOR && t.value.equals("(")) {
            System.out.println("      Parantezli ifade başlıyor.");
            consumeToken();
            Object result = evaluateExpression();
            match(TokenType.TOKEN_SEPARATOR, ")");
            System.out.println("      Parantezli ifade bitti.");
            return result;
        } else {
            throw new RuntimeException("Syntax Hatası: Faktörde beklenmedik belirteç: " + t.type + " ('" + t.value + "') konumunda " + (t.line + 1) + ". satır, " + (t.column + 1) + ". sütun.");
        }
    }
}