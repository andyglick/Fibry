package eu.lucaventuri.common;

/** Closable that can close other objects */
public interface ExtendedClosable extends AutoCloseable  {
    AutoCloseable closeOnExit(AutoCloseable... closeables);
}
