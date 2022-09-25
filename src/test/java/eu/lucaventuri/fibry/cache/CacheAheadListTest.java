package eu.lucaventuri.fibry.cache;

import eu.lucaventuri.common.ConcurrentHashSet;
import eu.lucaventuri.common.SystemUtils;

import org.junit.jupiter.api.Test;

import org.assertj.core.api.Assertions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class CacheAheadListTest {
  private CacheAheadList<Integer> getCacheAhead(int minSizeRefill, int numValues, boolean emergency, int timeoutMs, int waitMs) {
    return getCacheAhead(new ArrayList<>(), minSizeRefill, numValues, emergency, timeoutMs, waitMs);
  }

  private CacheAheadList<Integer> getCacheAhead(List<Integer> actorList, int minSizeRefill, int numValues, boolean emergency, int timeoutMs, int waitMs) {
    AtomicInteger n = new AtomicInteger();

    return new CacheAheadList<>(actorList, null, (numMessages, actor) -> {
      if (waitMs > 0)
        SystemUtils.sleep(waitMs);

      var list = new ArrayList<Integer>();

      for (int i = 0; i < numValues; i++)
        list.add(n.incrementAndGet());
      actor.accept(list);
    }, minSizeRefill, timeoutMs, emergency ? () -> {
      var list = new ArrayList<Integer>();

      for (int i = 0; i < numValues; i++)
        list.add(n.incrementAndGet() + 1_000_000);
      return list;
    } : null, 0);
  }

  @Test
  void testPrefill() {
    List<Integer> actorList = new ArrayList<>();
    getCacheAhead(actorList, 100, 250, false, 1000, 0);

    SystemUtils.sleep(10);

    Assertions.assertThat(actorList.size()).isEqualTo(250);
  }

  @Test
  void testOne() {
    var cal = getCacheAhead(100, 250, false, 1000, 0);

    Assertions.assertThat(cal.get()).isEqualTo(Integer.valueOf(1));
  }

  @Test
  void test1000() {
    var cal = getCacheAhead(100, 250, false, 1000, 0);

    for (int i = 0; i < 1000; i++) {
      Assertions.assertThat(cal.get()).isEqualTo(Integer.valueOf(i + 1));
    }
  }

  @Test
  void test1000Multi() throws InterruptedException {
    var cal = getCacheAhead(100, 500, false, 10000, 5);
    Set<Integer> set = ConcurrentHashSet.build();
    int numThreads = 100;
    CountDownLatch latch = new CountDownLatch(numThreads);
    int numCalls = 1000;

    for (int thread = 0; thread < numThreads; thread++) {
      new Thread(() -> {
        for (int i = 0; i < numCalls; i++) {
          set.add(cal.get());
        }

        latch.countDown();
      }).start();
    }

    latch.await();

    Assertions.assertThat(set.size()).isEqualTo(numThreads * numCalls);

    for (int i = 0; i < numThreads * numCalls; i++)
      Assertions.assertThat(set.contains(i + 1)).isTrue();
  }

  @Test
  void testTimeout() {
    var cal = getCacheAhead(0, 1, true, 10, 120);

    Assertions.assertThat(cal.get()).isEqualTo(Integer.valueOf(1));
    Assertions.assertThat(cal.get()).isEqualTo(Integer.valueOf(1_000_002));
    Assertions.assertThat(cal.get()).isEqualTo(Integer.valueOf(1_000_003));

    System.out.println("Async refills: " + cal.getNumAsyncRefills());
    System.out.println("Sync refills: " + cal.getNumSyncRefills());
    System.out.println("Num items: " + cal.getNumItemsRetrieved());
  }
}
