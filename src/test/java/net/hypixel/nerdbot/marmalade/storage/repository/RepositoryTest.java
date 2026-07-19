package net.hypixel.nerdbot.marmalade.storage.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryTest {

    private static MongoClient mongoClient;

    @BeforeAll
    static void createMongoClient() {
        // Never connected to; the repositories under test override saveToDatabase and perform no I/O
        mongoClient = MongoClients.create("mongodb://127.0.0.1:1");
    }

    @AfterAll
    static void closeMongoClient() {
        mongoClient.close();
    }

    @Test
    void evictionSavesToDatabaseByDefault() throws InterruptedException {
        RecordingRepository repository = new RecordingRepository(mongoClient);
        TestEntity entity = new TestEntity("default-eviction");

        repository.cacheObject(entity);
        expireAllEntries(repository);

        assertThat(repository.saveLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(repository.saved).containsExactly(entity);
    }

    @Test
    void evictionSavesToDatabaseWhenWriteBackEnabled() throws InterruptedException {
        RecordingRepository repository = new RecordingRepository(mongoClient, true);
        TestEntity entity = new TestEntity("enabled-eviction");

        repository.cacheObject(entity);
        expireAllEntries(repository);

        assertThat(repository.saveLatch.await(5, TimeUnit.SECONDS)).isTrue();
        assertThat(repository.saved).containsExactly(entity);
    }

    @Test
    void evictionDoesNotSaveToDatabaseWhenWriteBackDisabled() throws InterruptedException {
        RecordingRepository repository = new RecordingRepository(mongoClient, false);
        TestEntity entity = new TestEntity("disabled-eviction");

        repository.cacheObject(entity);
        expireAllEntries(repository);

        assertThat(repository.saveLatch.await(300, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void explicitRemovalDoesNotSaveToDatabaseWhenWriteBackEnabled() throws InterruptedException {
        RecordingRepository repository = new RecordingRepository(mongoClient, true);
        TestEntity entity = new TestEntity("explicit-enabled");

        repository.cacheObject(entity);
        repository.getCache().invalidate(entity.id);
        repository.getCache().cleanUp();

        assertThat(repository.saveLatch.await(300, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(repository.saved).isEmpty();
    }

    @Test
    void explicitRemovalDoesNotSaveToDatabaseWhenWriteBackDisabled() throws InterruptedException {
        RecordingRepository repository = new RecordingRepository(mongoClient, false);
        TestEntity entity = new TestEntity("explicit-disabled");

        repository.cacheObject(entity);
        repository.getCache().invalidate(entity.id);
        repository.getCache().cleanUp();

        assertThat(repository.saveLatch.await(300, TimeUnit.MILLISECONDS)).isFalse();
        assertThat(repository.saved).isEmpty();
    }

    private static void expireAllEntries(RecordingRepository repository) {
        repository.getCache().policy().expireAfterAccess().orElseThrow().setExpiresAfter(Duration.ZERO);
        repository.getCache().cleanUp();
    }

    static class TestEntity {
        final String id;

        TestEntity(String id) {
            this.id = id;
        }
    }

    static class RecordingRepository extends Repository<TestEntity> {

        final List<TestEntity> saved = new CopyOnWriteArrayList<>();
        final CountDownLatch saveLatch = new CountDownLatch(1);

        RecordingRepository(MongoClient mongoClient) {
            super(mongoClient, "marmalade-test", "test-entities", "id", 1, TimeUnit.HOURS);
        }

        RecordingRepository(MongoClient mongoClient, boolean saveOnEviction) {
            super(mongoClient, "marmalade-test", "test-entities", "id", 1, TimeUnit.HOURS, saveOnEviction);
        }

        @Override
        protected String getId(TestEntity entity) {
            return entity.id;
        }

        @Override
        public UpdateResult saveToDatabase(TestEntity entity) {
            saved.add(entity);
            saveLatch.countDown();
            return null;
        }
    }
}
