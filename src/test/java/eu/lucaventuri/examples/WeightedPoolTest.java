package eu.lucaventuri.examples;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.lucaventuri.common.SystemUtils;
import eu.lucaventuri.concurrent.Lockable;
import eu.lucaventuri.fibry.Stereotypes;

import java.lang.invoke.MethodHandles;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WeightedPoolTest {

    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass().getSimpleName());

    @Test
    void testWeightedPool() throws InterruptedException {

        LOG.info("Running WeightedPoolTest::testWeightedPool");

        int heavierThreads = 3;
        int numPermits = 64;
        int lightThreads = 1000;

        testLockStrategy(Lockable.fromActor(numPermits), "Semaphore actor", numPermits, lightThreads, heavierThreads);
        testLockStrategy(Lockable.fromSemaphore(numPermits, true), "Semaphore fair", numPermits, lightThreads, heavierThreads);
        // It hangs because of unfairness
        // testLockStrategy(Lockable.fromSemaphore(numPermits, false), "Semaphore unfair", numPermits, lightThreads, heavierThreads);

        LOG.info("Exiting WeightedPoolTest::testWeightedPool");

//        System.exit(0);
    }

    @Test
    @Disabled
    void testLockStrategy(Lockable lockable, String description, int numPermits, int lightThreads, int heavierThreads) throws InterruptedException {
        System.out.println("Starting " + description);
        long start = System.currentTimeMillis();

        CountDownLatch latchHeavierExecuted = new CountDownLatch(heavierThreads);
        CountDownLatch latchCreated = new CountDownLatch(lightThreads);
        AtomicInteger numLightLocked = new AtomicInteger();

        // Start threads locking 1 permit
        for (int i = 0; i < lightThreads; i++) {
            Stereotypes.def().runOnce(() -> {
                latchCreated.countDown();
                while (latchHeavierExecuted.getCount() > 0) {
                    try(Lockable.Unlock unlock = lockable.acquire(1)) {
                        SystemUtils.sleep(1);
                        if (numLightLocked.incrementAndGet() % 1000 == 0) {
                            System.out.println("Locks so far for " + description + ": " + numLightLocked.get());
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        latchCreated.await();

        // Start threads locking all the permits
        for (int i = 0; i < heavierThreads; i++) {
            Stereotypes.def().runOnce(() -> {
                try(Lockable.Unlock unlock = lockable.acquire(numPermits)) {
                    System.out.println("Heavy thread acquired");

                    latchHeavierExecuted.countDown();

                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }

        latchHeavierExecuted.await();
        System.out.println("Time required by " + description + ": " + (System.currentTimeMillis() - start));
        System.out.println("Locks required by " + description + ": " + numLightLocked);

        assertTrue(true);
    }
}
