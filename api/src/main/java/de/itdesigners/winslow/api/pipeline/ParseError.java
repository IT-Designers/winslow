package de.itdesigners.winslow.api.pipeline;

public class ParseError {

    public final int    line;
    public final int    column;
    public final String message;

    public ParseError(int line, int column, String message) {
        this.line    = line;
        this.column  = column;
        this.message = message;
    }
}
