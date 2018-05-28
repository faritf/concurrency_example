package ru.iteco.ffi.task2.cache;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(MyCacheServiceImpl.class)
//@PowerMockRunnerDelegate(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(locations = {"classpath:applicationContext.xml"})
public class MyCacheServiceImplTest {

    //@Autowired
    // Couldn't autowire here because Spring calls constructor, which will call loadCache,
    // which in turn will call static "Files.lines" that we can mock only after context load.
    MyCacheService service;
    private Stream<String> firstStream = Stream.of("key=val1");
    private Stream<String> secondStream = Stream.of("key=val2");
    private Stream<String> thirdStream = Stream.of("key=val3");

    @Before
    public void init() throws IOException {
        PowerMockito.mockStatic(Files.class);
        Mockito.when(Files.lines(Mockito.any(Path.class)))
                .thenReturn(firstStream)
                .thenReturn(secondStream)
                .thenReturn(thirdStream);
    }

    @Test
    public void testGetSerial() throws IOException {
        service = new MyCacheServiceImpl(2, new ClassPathResource("\\kvs.txt"));

        assertEquals(0, service.getReadsCount());

        assertEquals("val1", service.get("key"));
        assertEquals(1, service.getReadsCount());

        assertEquals("val1", service.get("key"));
        assertEquals(2, service.getReadsCount());

        assertEquals("val2", service.get("key"));
        assertEquals(1, service.getReadsCount());
    }

    @Test
    public void testGetParallel() throws IOException, InterruptedException {
        int readsLimit = 33;
        service = new MyCacheServiceImpl(readsLimit, new ClassPathResource("\\kvs.txt"));

        int threadsCount = readsLimit * 3; // this means that cache will reload three times, as we mocked Files.lines in init()
        int maxTimeoutSeconds = 60;
        ExecutorService threadPool = Executors.newFixedThreadPool(threadsCount);
        List<String> values = new CopyOnWriteArrayList<>(); // did'n bother with optimization here
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
                        values.add(service.get("key"));
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


        Map<String, Long> valuesCount = values.stream()
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        // check that we got only allowed amount of values before reload
        assertEquals(Long.valueOf(readsLimit), valuesCount.get("val1"));
        assertEquals(Long.valueOf(readsLimit), valuesCount.get("val2"));
        assertEquals(Long.valueOf(readsLimit), valuesCount.get("val3"));
    }

}