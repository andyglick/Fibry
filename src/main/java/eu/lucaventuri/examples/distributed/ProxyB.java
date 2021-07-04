package eu.lucaventuri.examples.distributed;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class ProxyB {
    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        ProxyA.startProxy(9802, "proxyB", 9801, "proxyA", 9810, "!mapActor!", "secret");
    }
}
