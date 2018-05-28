package ru.iteco.ffi.task2.sequence;

import org.junit.Before;
import org.junit.Test;

import java.math.BigInteger;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.Assert.*;

public class SequenceImplTest {

    private Sequence sequence;

    @Before
    public void init() {
        sequence = new SequenceImpl(BigInteger.ZERO, BigInteger.ONE);
    }


    @Test
    public void testSerial() {
        assertEquals(0, sequence.curval().intValue());
        assertEquals(1, sequence.next().intValue());
        assertEquals(1, sequence.curval().intValue());
    }

    @Test
    public void testParallel() throws InterruptedException {
        int threadsCount = 100;
        int maxTimeoutSeconds = 60;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadsCount);
        List<BigInteger> values = new CopyOnWriteArrayList<>(); // did'n bother with optimization here
        List<Throwable> exceptions = new CopyOnWriteArrayList<>();

        try {
            CountDownLatch allReady = new CountDownLatch(threadsCount);
            CountDownLatch allDone = new CountDownLatch(threadsCount);
            CountDownLatch afterInitBlocker = new CountDownLatch(1);
            for (int i = 0; i < threadsCount; i++) {
                threadPool.submit(() -> {
                    allReady.countDown();
                    try {
                        afterInitBlocker.await();
                        values.add(sequence.next());
                    } catch (Throwable e) {
                        exceptions.add(e);
                    } finally {
                        allDone.countDown();
                    }
                });
            }

            // check that all thread are initialized
            assertTrue("Timeout initializing threads!",
                    allReady.await(threadsCount * 10, TimeUnit.MILLISECONDS));

            // execute all threads
            afterInitBlocker.countDown();

            //check that all threads finished work at given timeout
            assertTrue("More than" + maxTimeoutSeconds + "seconds timeout!",
                    allDone.await(maxTimeoutSeconds, TimeUnit.SECONDS));

        } finally {
            threadPool.shutdownNow();
        }

        // check that all thread got values, and none got exception
        assertEquals(threadsCount, values.size());
        assertFalse(values.contains(null));
        assertEquals(0, exceptions.size());

        // check that every call got unique value
        assertEquals(threadsCount, values.stream().distinct().count());

        // check that got correct current value
        assertEquals(threadsCount, sequence.curval().intValue());
    }
}