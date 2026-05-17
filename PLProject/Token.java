package PLProject;

public class Token {

    public final TokenType type; 
    public final String value; 
    public final int line;     
    public final int column;   

    public Token(TokenType type, String value, int line, int column) {
        this.type = type;
        this.value = value;
        this.line = line;
        this.column = column;
    }

    @Override
    public String toString() {
        return "Token{"
                + "type=" + type
                + ", value='" + (value != null ? value : "null") + '\'' 
                + ", line=" + (line + 1) 
                + ", column=" + (column + 1)
                + '}';
    }
}