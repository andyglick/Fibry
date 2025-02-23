package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpExchange;

import eu.lucaventuri.common.Exceptions;
import eu.lucaventuri.fibry.distributed.HttpChannel;
import eu.lucaventuri.fibry.distributed.JacksonSerDeser;
import eu.lucaventuri.fibry.distributed.JavaSerializationSerDeser;
import eu.lucaventuri.fibry.distributed.StringSerDeser;
import eu.lucaventuri.fibry.distributed.TcpChannel;
import eu.lucaventuri.fibry.distributed.TcpReceiver;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class User implements Serializable {
    public String name;

    User() {
    }

    User(String name) {
        this.name = name;
    }
}

class PhoneNumber implements Serializable {
    public String number;

    PhoneNumber() {
    }

    PhoneNumber(String number) {
        this.number = number;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PhoneNumber that = (PhoneNumber) o;
        return Objects.equals(number, that.number);
    }

    @Override
    public int hashCode() {
        return Objects.hash(number);
    }

    @Override
    public String toString() {
        return "PhoneNumber{" +
                "number='" + number + '\'' +
                '}';
    }
}

class TestRemoteActors {

    @Test
    void testHttpNoReturn() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19001;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            return "OK";
        });

        var actor = ActorSystem.anonymous().<String>newRemoteActor("actor1", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.GET, null, null, false), StringSerDeser.INSTANCE);
        actor.sendMessage("Test");

        latch.await();

        assertTrue(true);
    }

    @Test
    void testHttpWithReturnGET() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19002;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            assertEquals("GET", ex.getRequestMethod());

            return ex.getRequestURI().getQuery();
        });

        var actor = ActorSystem.anonymous().<String, String>newRemoteActorWithReturn("actor2", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.GET, null, null, false), StringSerDeser.INSTANCE);
        var result = actor.sendMessageReturn("Test2");

        latch.await();
        System.out.println(result.get());
        assertEquals("actorName=actor2&type=java.lang.String&waitResult=true&message=Test2", result.get());
    }

    @Test
    void testHttpWithReturnPOST() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19003;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            assertEquals("POST", ex.getRequestMethod());

            return Exceptions.silence(() -> new String(ex.getRequestBody().readAllBytes()), "Error!");
        });

        var actor = ActorSystem.anonymous().<String, String>newRemoteActorWithReturn("actor3", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.POST, null, null, false), StringSerDeser.INSTANCE);
        var result = actor.sendMessageReturn("Test3");

        latch.await();
        System.out.println(result.get());
        assertEquals("Test3",result.get());
    }

    @Test
    void testHttpWithReturnPUT() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19004;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            assertEquals("PUT", ex.getRequestMethod());

            return Exceptions.silence(() -> new String(ex.getRequestBody().readAllBytes()), "Error!");
        });

        var actor = ActorSystem.anonymous().<String, String>newRemoteActorWithReturn("actor4", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.PUT, null, null, false), StringSerDeser.INSTANCE);
        var result = actor.sendMessageReturn("Test4");

        latch.await();
        System.out.println(result.get());
        assertEquals("Test4", result.get());
    }

    @Test
    void testHttpWithReturnJackson() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19005;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            System.out.println(ex.getRequestURI().getQuery());

            return "{\"number\":\"+47012345678\"}";
        });

        var user = new User("TestUser");
        var phone = new PhoneNumber("+47012345678");
        var actor = ActorSystem.anonymous().<User, PhoneNumber>newRemoteActorWithReturn("actor2", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.GET, null, null, false), new JacksonSerDeser<>(PhoneNumber.class));
        var result = actor.sendMessageReturn(user);

        latch.await(1, TimeUnit.SECONDS);
        System.out.println(result.get());
        assertEquals(phone, result.get());
    }

    @Test
    void testHttpWithReturnJavaSerialization() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 19006;

        Stereotypes.def().embeddedHttpServer(port, (HttpExchange ex) -> {
            latch.countDown();
            System.out.println(ex.getRequestURI().getQuery());

            var buf = new ByteArrayOutputStream();

            Exceptions.rethrowRuntime(() -> {
                var os = new ObjectOutputStream(buf);

                os.writeObject(new PhoneNumber("+4787654321"));
            });
            return Base64.getEncoder().encodeToString(buf.toByteArray());
        });

        var user = new User("TestUser2");
        var phone = new PhoneNumber("+4787654321");
        var actor = ActorSystem.anonymous().<User, PhoneNumber>newRemoteActorWithReturn("actor3", new HttpChannel("http://localhost:" + port + "/", HttpChannel.HttpMethod.GET, null, null, true), new JavaSerializationSerDeser<>());
        var result = actor.sendMessageReturn(user);

        latch.await(1, TimeUnit.SECONDS);
        System.out.println(result.get());
        assertEquals(phone, result.get());
    }

    @Test
    void testSerializer() {
        var ser = new JacksonSerDeser<String, String>(String.class);
        var serialized = ser.serializeToString("test1");

        System.out.println(serialized);
        System.out.println(ser.deserializeString(serialized));

        var serUser = new JacksonSerDeser<User, User>(User.class);
        var userSerialized = serUser.serializeToString(new User("User1"));
        System.out.println(userSerialized);
        System.out.println(serUser.deserializeString(userSerialized));

        assertTrue(true);
    }

    @Test
    void testTcpNoReturn() throws IOException, InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20001;
        var ser = new JacksonSerDeser<String, String>(String.class);

        // Server actor code
        TcpReceiver.startTcpReceiverProxy(port, "abc", ser, ser, false);

        ActorSystem.named("tcpActor1").newActor(str -> {
            assertEquals("test1", str);

            latch.countDown();
        });

        // Client actor code
        var ch = new TcpChannel<String, Void>(new InetSocketAddress(port), "abc", null, null, true, "chNoReturn");
        var actor = ActorSystem.anonymous().<String>newRemoteActor("tcpActor1", ch, ser);

        actor.sendMessage("test1");

        latch.await();
    }

    @Test
    void testTcpAnswerProxyType() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20002;
        var ser = new JacksonSerDeser<String, String>(String.class);

        // Server actor code
        TcpReceiver.startTcpReceiverProxy(port, "abc", ser, ser, false);

        ActorSystem.named("tcpActor2").newActorWithReturn(str -> {
            assertEquals("test2",str);

            latch.countDown();

            return str.toString().toUpperCase();
        });

        // Client actor code
        var ch = new TcpChannel<String, String>(new InetSocketAddress(port), "abc", ser, ser, true, "chAnswer");
        var actor = ActorSystem.anonymous().<String, String>newRemoteActorWithReturn("tcpActor2", ch, ser);

        var ret = actor.sendMessageReturn("test2");

        latch.await();

        assertEquals("TEST2", ret.get());
    }

    @Test
    void testTcpAnswerDirectType() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20022;
        var ser = new JacksonSerDeser<String, String>(String.class);

        // Server actor code; it's using an alias, but it does not have to
        TcpReceiver.startTcpReceiverDirectActor(port, "abc", ser, ser, false, "tcpActor2-d-alias");

        ActorSystem.named("tcpActor2-d-alias").newActorWithReturn(str -> {
            assertEquals("test2", str);

            latch.countDown();

            return str.toString().toUpperCase();
        });

        // Client actor code
        var ch = new TcpChannel<String, String>(new InetSocketAddress(port), "abc", ser, ser, false, "chAnswer");
        var actor = ActorSystem.anonymous().<String, String>newRemoteActorWithReturn("tcpActor2", ch, ser);

        var ret = actor.sendMessageReturn("test2");

        latch.await();

        assertEquals("TEST2", ret.get());
    }

    @Test
    void testTcpException() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20003;
        var ser = new JacksonSerDeser<Integer, Integer>(Integer.class);

        TcpReceiver.startTcpReceiverProxy(port, "abc", ser, ser, false);

        ActorSystem.named("tcpActor3").newActorWithReturn((Integer n) -> {
            assertEquals(n, Integer.valueOf(11));

            latch.countDown();

            return n/0;
        });

        var ch = new TcpChannel<Integer, Integer>(new InetSocketAddress(port), "abc", ser, ser, true, "chException");
        var actor = ActorSystem.anonymous().<Integer, Integer>newRemoteActorWithReturn("tcpActor3", ch, ser);

        var ret = actor.sendMessageReturn(11);

        latch.await();

        try {
            ret.get();
            Assertions.fail();
        } catch(Throwable t) {
            System.err.println("Expected exception: " + t);
            System.err.println("Cause exception: " + t.getCause());
        }
    }

    @Test
    void testTcpAnswerMix() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20004;
        var serMix = new JacksonSerDeser<String, Integer>(Integer.class);
        var serMix2 = new JacksonSerDeser<Integer, String>(String.class);

        TcpReceiver.startTcpReceiverProxy(port, "abc", serMix2, serMix2, false);

        ActorSystem.named("tcpActor4").newActorWithReturn(str -> {
            assertEquals("test2", str);

            latch.countDown();

            return str.toString().length();
        });

        var ch = new TcpChannel<String, Integer>(new InetSocketAddress(port), "abc", serMix, serMix, true, "chAnswerMix");
        var actor = ActorSystem.anonymous().<String, Integer>newRemoteActorWithReturn("tcpActor4", ch, serMix);

        var ret = actor.sendMessageReturn("test2");

        latch.await();

        assertEquals(Integer.valueOf(5), ret.get());
    }

    @Test
    void testTcpConnectionDrop() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latchReceived = new CountDownLatch(1);
        CountDownLatch latchConnectionDropped = new CountDownLatch(1);
        int port = 20005;
        var serMix = new JacksonSerDeser<String, Integer>(Integer.class);
        var serMix2 = new JacksonSerDeser<Integer, String>(String.class);
        var actorName = "tcpActor5";

        // Server
        TcpReceiver.startTcpReceiverProxy(port, "abc", serMix2, serMix2, false);

        ActorSystem.named(actorName).newActorWithReturn(str -> {
            assertEquals("test2", str);

            latchReceived.countDown();
            try {
                latchConnectionDropped.await();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            return str.toString().length();
        });

        // client
        var ch = new TcpChannel<String, Integer>(new InetSocketAddress(port), "abc", serMix, serMix, true, "chDrop");
        var actor = ActorSystem.anonymous().<String, Integer>newRemoteActorWithReturn(actorName, ch, serMix);

        var ret = actor.sendMessageReturn("test2");

        latchReceived.await();
        ch.drop();
        ch.ensureConnection();
        latchConnectionDropped.countDown();

        var value = ret.get();

        System.out.println("Received value: " + value);
        assertEquals(Integer.valueOf(5), value);
    }

    @Test
    void testTcpAliases() throws IOException, InterruptedException, ExecutionException {
        CountDownLatch latch = new CountDownLatch(1);
        int port = 20006;
        var serMix = new JacksonSerDeser<String, Integer>(Integer.class);
        var serMix2 = new JacksonSerDeser<Integer, String>(String.class);
        var actorName = "tcpActor6";

        System.out.println(1);
        TcpReceiver.startTcpReceiverProxy(port, "abc", serMix2, serMix2, false);

        System.out.println(2);
        ActorSystem.named(actorName).newActorWithReturn(str -> {
            System.out.println(6);
            assertEquals("test2", str);

            latch.countDown();

            return str.toString().length();
        });

        System.out.println(3);
        ActorSystem.setAliasResolver(name -> name.replace("alias-", ""));

        var ch = new TcpChannel<>(new InetSocketAddress(port), "abc", serMix, serMix, true, "chAnswerMix");
        var actor = ActorSystem.anonymous().<String, Integer>newRemoteActorWithReturn("alias-" + actorName, ch, serMix);

        System.out.println(4);
        var ret = actor.sendMessageReturn("test2");

        latch.await();

        System.out.println(5);
        assertEquals(Integer.valueOf(5), ret.get());
    }
}
