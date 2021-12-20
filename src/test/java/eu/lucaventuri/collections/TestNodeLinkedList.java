package eu.lucaventuri.collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import eu.lucaventuri.common.Exceptions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestNodeLinkedList {
    private final AtomicInteger numInserted = new AtomicInteger();
    private final AtomicInteger numDeleted = new AtomicInteger();

    @BeforeEach
    public void setup() {
        numInserted.set(0);
        numDeleted.set(0);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    @Test
    void testEmpty() {
        NodeLinkedList list = new NodeLinkedList();

        assertEmpty(list);
    }

    @Test
    void testOne() {
        NodeLinkedList<String> list = new NodeLinkedList<>();

        list.addToTail("abc");

        assert1Node(list, "abc");

        assertEquals("abc", list.removeHead());

        assertEmpty(list);

        NodeLinkedList.Node<String> n = list.addToTail("A");
        assert1Node(list, "A");

        list.remove(n);

        assertEmpty(list);
    }

    private void assertEmpty(NodeLinkedList<String> list) {
        assertEquals(0, list.asListFromHead().size());
        assertEquals(0, list.asListFromTail().size());
        assertNull(list.peekHead());
        assertNull(list.peekTail());
        assertFalse(list.iterator().hasNext());
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void testTwo() {
        NodeLinkedList<String> list = new NodeLinkedList<>();

        list.addToTail("A");
        assert1Node(list, "A");

        list.addToTail("B");
        assert2Nodes(list, "A", "B");

        list.removeHead();
        assert1Node(list, "B");

        list.removeHead();
        assertEmpty(list);

        NodeLinkedList.Node<String> n1 = list.addToTail("A");
        assert1Node(list, "A");

        NodeLinkedList.Node<String> n2 = list.addToTail("B");
        assert2Nodes(list, "A", "B");

        list.remove(n2);
        assert1Node(list, "A");

        n2 = list.addToTail("C");
        assert2Nodes(list, "A", "C");

        list.remove(n1);
        assert1Node(list, "C");
    }

    @SuppressWarnings("UnusedAssignment")
    @Test
    void testThree() {
        NodeLinkedList<String> list = new NodeLinkedList<>();

        list.addToTail("A");
        assert1Node(list, "A");

        list.addToTail("B");
        assert2Nodes(list, "A", "B");

        list.addToTail("C");
        assert3Nodes(list, "A", "B", "C");

        list.removeHead();
        assert2Nodes(list, "B", "C");

        list.removeHead();
        assert1Node(list, "C");

        list.removeHead();
        assertEmpty(list);

        NodeLinkedList.Node<String> n1 = list.addToTail("A");
        assert1Node(list, "A");

        NodeLinkedList.Node<String> n2 = list.addToTail("B");
        assert2Nodes(list, "A", "B");

        NodeLinkedList.Node<String> n3 = list.addToTail("C");
        assert3Nodes(list, "A", "B", "C");

        list.remove(n3);
        assert2Nodes(list, "A", "B");

        n3 = list.addToTail("D");
        assert3Nodes(list, "A", "B", "D");

        list.remove(n2);
        assert2Nodes(list, "A", "D");

        NodeLinkedList.Node<String> n4 = list.addToTail("E");
        assert3Nodes(list, "A", "D", "E");

        list.remove(n1);
        assert2Nodes(list, "D", "E");
    }

    @Test
    void testSingleInsertFromEmpty() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();

        insert(list, 1, 0, 100_000).await();

        verifyIntegerList(list, 0, 100_000);
    }

    @Test
    void testSingleInsertFromSomething() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();

        list.addToTail(-1);
        list.addToTail(-2);
        list.addToTail(-3);
        insert(list, 1, 0, 100_000).await();

        verifyIntegerList(list, -3, 100_000);
    }

    @Test
    @Disabled
    void testMultiInsertFromEmpty() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();

        insert(list, 100, 0, 1000).await();

        verifyIntegerList(list, 0, 100_000);
    }

    @Test
    @Disabled
    void testMultiInsertFromSomething() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();

        list.addToTail(-1);
        list.addToTail(-2);
        list.addToTail(-3);
        insert(list, 100, 0, 1000).await();

        verifyIntegerList(list, -3, 100_000);
    }

    @Test
    @Disabled
    void testMultiInsertThenRemoveHead() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();

        insert(list, 100, 0, 1000).await();
        removeHead(list, 100, 0, 1000).await();

        verifyIntegerList(list, 0, 0);
    }

    @Test
    @Disabled
    void testMultiInsertAndRemoveHead() throws InterruptedException {
        NodeLinkedList<Integer> list = new NodeLinkedList<>();
        AtomicBoolean exit = new AtomicBoolean();

        CountDownLatch latchRemove = removeHeadUntilRemoved(list, 100, 0, 1000);
        CountDownLatch latchInsert = insert(list, 100, 0, 1000);

        new Thread(() -> {
            while (!exit.get()) {
                System.out.println("Inserted: " + numInserted.get() + " - Removed: " + numDeleted.get());
                Exceptions.silence(() -> Thread.sleep(250));
            }
        }).start();
        latchRemove.await();
        exit.set(true);
        verifyIntegerList(list, 0, 0);
    }

    private void verifyIntegerList(NodeLinkedList<Integer> list, int start, int end) {
        List<Integer> listFromTail = list.asListFromTail();
        List<Integer> listFromHead = list.asListFromHead();

        verifyIntegerList(listFromTail, start, end);
        verifyIntegerList(listFromHead, start, end);
    }

    private void verifyIntegerList(List<Integer> list, int start, int end) {
        assertEquals(end - start, list.size());

        if (end == start)
            return;

        Collections.sort(list);

        assertEquals(start, (int) list.get(0));
        assertEquals(end - 1, (int) list.get(end - start - 1));

        for (int i = 0; i < end - start; i++)
            assertEquals(i + start, (int) list.get(i));
    }

    private void assert3Nodes(NodeLinkedList<String> list, String head, String middle, String tail) {
        assertEquals(3, list.asListFromHead().size());
        assertEquals(3, list.asListFromTail().size());
        assertEquals(head, list.asListFromHead().get(0));
        assertEquals(middle, list.asListFromHead().get(1));
        assertEquals(tail, list.asListFromHead().get(2));
        assertEquals(head, list.asListFromTail().get(2));
        assertEquals(middle, list.asListFromTail().get(1));
        assertEquals(tail, list.asListFromTail().get(0));
        assertEquals(head, list.peekHead());
        assertEquals(tail, list.peekTail());

        Iterator<String> iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(head, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(middle, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(tail, iter.next());
        assertFalse(iter.hasNext());
    }

    private void assert2Nodes(NodeLinkedList<String> list, String head, String tail) {
        assertEquals(2, list.asListFromHead().size());
        assertEquals(2, list.asListFromTail().size());
        assertEquals(head, list.asListFromHead().get(0));
        assertEquals(tail, list.asListFromHead().get(1));
        assertEquals(head, list.asListFromTail().get(1));
        assertEquals(tail, list.asListFromTail().get(0));
        assertEquals(head, list.peekHead());
        assertEquals(tail, list.peekTail());

        Iterator<String> iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(head, iter.next());
        assertTrue(iter.hasNext());
        assertEquals(tail, iter.next());
        assertFalse(iter.hasNext());
    }

    private void assert1Node(NodeLinkedList<String> list, String value) {
        assertEquals(1, list.asListFromHead().size());
        assertEquals(1, list.asListFromTail().size());
        assertEquals(value, list.asListFromHead().get(0));
        assertEquals(value, list.asListFromTail().get(0));
        assertEquals(value, list.peekHead());
        assertEquals(value, list.peekTail());

        Iterator<String> iter = list.iterator();
        assertTrue(iter.hasNext());
        assertEquals(value, iter.next());
        assertFalse(iter.hasNext());
    }

    private CountDownLatch insert(NodeLinkedList<Integer> list, int numThreads, int start, int batchSize) {
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int curThread = 0; curThread < numThreads; curThread++) {
            int batchStart = start + batchSize * curThread;
            int batchEnd = batchStart + batchSize;
            new Thread(() -> {
                for (int i = batchStart; i < batchEnd; i++) {
                    list.addToTail(i);
                    numInserted.incrementAndGet();
                }

                latch.countDown();
            }).start();
        }

        return latch;
    }

    private CountDownLatch removeHead(NodeLinkedList<Integer> list, int numThreads, int start, int batchSize) {
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int curThread = 0; curThread < numThreads; curThread++) {
            int batchStart = start + batchSize * curThread;
            int batchEnd = batchStart + batchSize;
            new Thread(() -> {
                for (int i = batchStart; i < batchEnd; i++)
                    list.removeHead();

                latch.countDown();
            }).start();
        }

        return latch;
    }

    private CountDownLatch removeHeadUntilRemoved(NodeLinkedList<Integer> list, int numThreads, int start, int batchSize) {
        CountDownLatch latch = new CountDownLatch(numThreads);

        for (int curThread = 0; curThread < numThreads; curThread++) {
            int batchStart = start + batchSize * curThread;
            int batchEnd = batchStart + batchSize;
            new Thread(() -> {
                for (int i = batchStart; i < batchEnd; i++) {
                    Integer n;
                    int num=0;

                    do {
                        n = list.removeHead();
                        num++;

                        if (num%100==0)
                            Exceptions.silence(() -> Thread.sleep(1));
                    } while (n == null);

                    numDeleted.incrementAndGet();
                }

                latch.countDown();
            }).start();
        }

        return latch;
    }
}
