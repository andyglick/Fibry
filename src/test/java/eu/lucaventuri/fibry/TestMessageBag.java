package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Test;

import eu.lucaventuri.common.SystemUtils;
import eu.lucaventuri.functional.Either;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;

class A { }
class B extends A {}

class TestMessageBag {

    @Test
    void testEmpty() {
        MiniQueue<String> queue = MiniQueue.blocking();
        MessageBag<String, String> silo = new MessageBag<>(queue);

        new Thread(() -> {
            SystemUtils.sleep(10);
            queue.offer("abc");
        }).start();

        assertEquals("abc", silo.readMessage());
    }

    @Test
    void testInOrder() {
        MiniQueue<String> queue = MiniQueue.blocking();
        MessageBag<String, String> silo = new MessageBag<>(queue);

        new Thread(() -> {
            queue.offer("A");
            queue.offer("B");
            SystemUtils.sleep(10);
            queue.offer("C");
            queue.offer("D");
            queue.offer("E");
        }).start();

        assertEquals("A", silo.readMessage());
        assertEquals("B", silo.readMessage());
        assertEquals("C", silo.readMessage());
        assertEquals("D", silo.readMessage());
        assertEquals("E", silo.readMessage());
    }

    @Test
    void testReceive() {
        MiniQueue<Object> queue = MiniQueue.blocking();
        MessageBag<Object, Object> silo = new MessageBag<>(queue);

        new Thread(() -> {
            queue.offer("A");
            queue.offer(2);
            SystemUtils.sleep(10);
            queue.offer("BB");
            queue.offer("CC");
            queue.offer("D");
            queue.offer("E");
            queue.offer(3);
            queue.offer(4);
            queue.offer(5);
            queue.offer(6);
            queue.offer(7);
            queue.offer(7.1);
            queue.offer(9);
            queue.offer("F");
            queue.offer("FF");
            queue.offer(11);
            queue.offer("G");
            queue.offer(10);
            queue.offer(8);
            queue.offer("H");
            queue.offer(13);
            queue.offer(12);
            queue.offer(14);
            queue.offer("J");
        }).start();

        // From queue
        assertEquals(2, (int)silo.receive(Integer.class, v -> true));
        assertEquals(3, (int)silo.receive(Integer.class, v -> true));
        assertEquals(4, (int)silo.receive(Integer.class, v -> true));
        assertEquals(6, (int)silo.receive(Integer.class, v -> v %2==0));
        assertEquals(10, (int)silo.receive(Integer.class, v -> v %2==0));
        assertEquals(8, (int)silo.receive(Integer.class, v -> v %2==0));

        // From map
        assertEquals("A", silo.receive(String.class, v -> true));
        assertEquals("D", silo.receive(String.class, v -> v.length()==1));
        assertEquals("E", silo.receive(String.class, v -> v.length()==1));
        assertEquals("F", silo.receive(String.class, v -> v.length()==1));
        assertEquals("G", silo.receive(String.class, v -> v.length()==1));

        // Queue
        assertEquals(12, (int)silo.receive(Integer.class, v -> v %2==0));
        // Map
        assertEquals("H", silo.receive(String.class, v -> v.length()==1));
        // Queue
        assertEquals("J", silo.receive(String.class, v -> v.length()==1));
        // Map
        assertEquals(14, (int)silo.receive(Integer.class, v -> v %2==0));
    }

    @Test
    void testReceiveConverter() {
        MiniQueue<Either> queue = MiniQueue.blocking();
        Function<Either, Object> converter = e -> e.left();
        MessageBag<Either, Object> silo = new MessageBag<>(queue, converter);

        new Thread(() -> {
            queue.offer(Either.left("A"));
            queue.offer(Either.left(2));
            SystemUtils.sleep(10);
            queue.offer(Either.left("BB"));
            queue.offer(Either.left("CC"));
            queue.offer(Either.left("D"));
            queue.offer(Either.left("E"));
            queue.offer(Either.left(3));
            queue.offer(Either.left(4));
            queue.offer(Either.left(5));
            queue.offer(Either.left(6));
            queue.offer(Either.left(7));
            queue.offer(Either.left(7.1));
            queue.offer(Either.left(9));
            queue.offer(Either.left("F"));
            queue.offer(Either.left("FF"));
            queue.offer(Either.left(11));
            queue.offer(Either.left("G"));
            queue.offer(Either.left(10));
            queue.offer(Either.left(8));
            queue.offer(Either.left("H"));
            queue.offer(Either.left(13));
            queue.offer(Either.left(12));
            queue.offer(Either.left(14));
            queue.offer(Either.left("J"));
        }).start();

// From queue
        assertEquals(2, (int)silo.receiveAndConvert(Integer.class, v -> true));
        assertEquals(3, (int)silo.receiveAndConvert(Integer.class, v -> true));
        assertEquals(4, (int)silo.receiveAndConvert(Integer.class, v -> true));
        assertEquals(6, (int)silo.receiveAndConvert(Integer.class, v -> v %2==0));
        assertEquals(10, (int)silo.receiveAndConvert(Integer.class, v -> v %2==0));
        assertEquals(8, (int)silo.receiveAndConvert(Integer.class, v -> v %2==0));

        // From map
        assertEquals("A", silo.receiveAndConvert(String.class, v -> true));
        assertEquals("D", silo.receiveAndConvert(String.class, v -> v.length()==1));
        assertEquals("E", silo.receiveAndConvert(String.class, v -> v.length()==1));
        assertEquals("F", silo.receiveAndConvert(String.class, v -> v.length()==1));
        assertEquals("G", silo.receiveAndConvert(String.class, v -> v.length()==1));

        // Queue
        assertEquals(12, (int)silo.receiveAndConvert(Integer.class, v -> v %2==0));
        // Map
        assertEquals("H", silo.receiveAndConvert(String.class, v -> v.length()==1));
        // Queue
        assertEquals("J", silo.receiveAndConvert(String.class, v -> v.length()==1));
        // Map
        assertEquals(14, (int)silo.receiveAndConvert(Integer.class, v -> v %2==0));
    }

    @Test
    void testInheritance() {
        MiniQueue<Object> queue = MiniQueue.blocking();
        MessageBag<Object, Object> silo = new MessageBag<>(queue);
        A a1 = new A();
        A a2 = new A();
        A a3 = new A();
        B b1 = new B();
        B b2 = new B();
        B b3 = new B();

        new Thread(() -> {
            queue.offer(a1);
            queue.offer(a2);
            queue.offer(b1);
            queue.offer(b2);
            queue.offer(a3);
            queue.offer(b3);
            queue.offer(1);
        }).start();

        // From queue
        assertEquals(a1, silo.receive(A.class, v -> true));
        assertEquals(a2, silo.receive(A.class, v -> true));
        assertEquals(b1, silo.receive(A.class, v -> true));
        assertEquals(1, (int)silo.receive(Integer.class, v -> true));

        // From map
        assertEquals(a3, silo.receive(A.class, v -> true));
        assertEquals(b2, silo.receive(A.class, v -> true));
        assertEquals(b3, silo.receive(A.class, v -> true));
    }
}
