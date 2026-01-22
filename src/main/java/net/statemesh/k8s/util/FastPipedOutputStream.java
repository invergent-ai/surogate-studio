package net.statemesh.k8s.util;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

public class FastPipedOutputStream extends PipedOutputStream {

    public FastPipedOutputStream(PipedInputStream snk) throws IOException {
        super(snk);
    }

    @Override
    public void write(@NotNull byte[] b) throws IOException {
        super.write(b);
        flush();
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        super.write(b, off, len);
        flush();
    }

    @Override
    public void write(int b)  throws IOException {
       super.write(b);
       flush();
    }
}
