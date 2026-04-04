package net.hypixel.nerdbot.marmalade.concurrent;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

class ScheduledTaskTest {

    @Test
    void startAndStopLifecycle() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        ScheduledTask task = ScheduledTask.create("lifecycle-test", counter::incrementAndGet, Duration.ofMillis(50));

        assertThat(task.isRunning()).isFalse();

        task.start();
        assertThat(task.isRunning()).isTrue();

        Thread.sleep(200);

        assertThat(counter.get()).isGreaterThan(0);

        task.stop();
        assertThat(task.isRunning()).isFalse();
    }

    @Test
    void doubleStartIsNoOp() {
        ScheduledTask task = ScheduledTask.create("double-start-test", () -> {}, Duration.ofMillis(50));

        task.start();
        try {
            task.start(); // should log WARN and return without throwing
            assertThat(task.isRunning()).isTrue();
        } finally {
            task.stop();
        }
    }

    @Test
    void getName() {
        ScheduledTask task = ScheduledTask.create("my-task", () -> {}, Duration.ofMillis(100));
        assertThat(task.getName()).isEqualTo("my-task");
    }

    @Test
    void exceptionInTaskDoesNotKillScheduler() throws InterruptedException {
        AtomicInteger counter = new AtomicInteger(0);
        ScheduledTask task = ScheduledTask.create("exception-test", () -> {
            counter.incrementAndGet();
            throw new RuntimeException("deliberate test exception");
        }, Duration.ofMillis(50));

        task.start();
        try {
            Thread.sleep(200);
            assertThat(counter.get()).isGreaterThan(1);
        } finally {
            task.stop();
        }
    }

    @Test
    void closeStopsTask() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        ScheduledTask task = ScheduledTask.create("close-test", latch::countDown, Duration.ofMillis(50));

        task.start();

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();

        task.close();

        assertThat(task.isRunning()).isFalse();
    }
}
