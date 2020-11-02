package de.itdesigners.winslow.web.api;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.OutputStream;

public class ImmediateOutputStream extends OutputStream {

    private final @Nonnull OutputStream outputStream;

    public ImmediateOutputStream(@Nonnull OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    @Override
    public void write(@Nonnull byte[] b) throws IOException {
        this.outputStream.write(b);
        this.outputStream.flush();
    }

    @Override
    public void write(@Nonnull byte[] b, int off, int len) throws IOException {
        this.outputStream.write(b, off, len);
        this.outputStream.flush();
    }

    @Override
    public void flush() throws IOException {
        this.outputStream.flush();
    }

    @Override
    public void close() throws IOException {
        this.outputStream.close();
    }

    @Override
    public void write(int i) throws IOException {
        this.outputStream.write(i);
        this.outputStream.flush();
    }
}
