package net.hypixel.nerdbot.marmalade.concurrent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutorFactoryTest {

    @Test
    void scheduledDaemonCreatesDaemonThread() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Thread> capturedThread = new AtomicReference<>();

        ScheduledExecutorService executor = ExecutorFactory.newScheduledDaemon("test-scheduled");
        try {
            executor.submit(() -> {
                capturedThread.set(Thread.currentThread());
                latch.countDown();
            });

            assertThat(latch.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(capturedThread.get().isDaemon()).isTrue();
            assertThat(capturedThread.get().getName()).isEqualTo("test-scheduled");
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void daemonPoolCreatesNumberedThreads() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(2);
        List<String> threadNames = new ArrayList<>();

        ExecutorService executor = ExecutorFactory.newDaemonPool("pool", 2);
        try {
            for (int i = 0; i < 2; i++) {
                executor.submit(() -> {
                    synchronized (threadNames) {
                        threadNames.add(Thread.currentThread().getName());
                    }
                    latch.countDown();
                });
            }

            assertThat(latch.await(2, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
            assertThat(threadNames).allSatisfy(name -> assertThat(name).startsWith("pool"));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void daemonThreadFactorySetsDaemonFlag() {
        ThreadFactory factory = ExecutorFactory.daemonThreadFactory("my-thread");
        Thread thread = factory.newThread(() -> {});

        assertThat(thread.isDaemon()).isTrue();
        assertThat(thread.getName()).isEqualTo("my-thread");
    }

    @Test
    void numberedThreadFactoryIncrements() {
        ThreadFactory factory = ExecutorFactory.daemonThreadFactory("pool", true);

        Thread t1 = factory.newThread(() -> {});
        Thread t2 = factory.newThread(() -> {});

        assertThat(t1.getName()).isEqualTo("pool-1");
        assertThat(t2.getName()).isEqualTo("pool-2");
    }
}
