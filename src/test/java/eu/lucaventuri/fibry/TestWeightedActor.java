package eu.lucaventuri.fibry;

import org.junit.jupiter.api.Test;

import eu.lucaventuri.common.SystemUtils;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;


class TestWeightedActor {

    @Test
    void testNormalPool() {
        AtomicInteger num = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        int numThreads = 10;

        var leader
            = ActorSystem.anonymous()
            .poolParams(PoolParameters.fixedSize(numThreads), null)
            .newPool(msg-> {
            int n = num.incrementAndGet();
            max.updateAndGet( m -> Math.max(m, n));
            SystemUtils.sleep(10);
            num.decrementAndGet();
        });

        for(int i = 0; i < 50; i++)
            leader.sendMessage(i);

        leader.sendPoisonPill();
        leader.waitForExit();

        assertEquals(10, max.get());
    }

    @Test
    void testWeightedPoolWeight1() {
        AtomicInteger num = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        int numThreads = 10;

        var leader = ActorSystem.anonymous().poolParams(PoolParameters.fixedSize(numThreads), null).newWeightedPool(msg-> {
            int n = num.incrementAndGet();
            max.updateAndGet( m -> Math.max(m, n));
            SystemUtils.sleep(10);
            num.decrementAndGet();
        });

        for(int i=0; i<50; i++)
            leader.sendMessage(i, 1);

        leader.sendPoisonPill();
        leader.waitForExit();

        assertEquals(10, max.get());
    }

    @Test
    void testWeightedPoolWeight2() {
        AtomicInteger num = new AtomicInteger();
        AtomicInteger max = new AtomicInteger();
        int numThreads = 10;

        var leader= ActorSystem.anonymous().poolParams(PoolParameters.fixedSize(numThreads), null)
            .newWeightedPool(msg-> {
            int n = num.incrementAndGet();
            max.updateAndGet( m -> Math.max(m, n));
            SystemUtils.sleep(10);
            num.decrementAndGet();
        });

        for(int i=0; i<50; i++)
            leader.sendMessage(i, 3);

        leader.sendPoisonPill();
        leader.waitForExit();

        assertEquals(3, max.get());
    }
}
