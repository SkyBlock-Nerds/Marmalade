package net.hypixel.nerdbot.marmalade.storage.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.result.UpdateResult;
import net.hypixel.nerdbot.marmalade.exception.RepositoryException;
import net.hypixel.nerdbot.marmalade.functional.Result;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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

    @Test
    void cacheAndSaveAsyncCachesThenPersists() {
        RecordingRepository repository = new RecordingRepository(mongoClient);
        TestEntity entity = new TestEntity("cache-and-save");

        repository.cacheAndSaveAsync(entity).join();

        assertThat(repository.getCache().getIfPresent("cache-and-save")).isSameAs(entity);
        assertThat(repository.saved).containsExactly(entity);
    }

    @Test
    void cacheAndSaveAsyncFailureCompletesExceptionallyButKeepsTheCachedCopy() {
        FailingSaveRepository repository = new FailingSaveRepository(mongoClient);
        TestEntity entity = new TestEntity("failing-save");

        assertThatThrownBy(() -> repository.cacheAndSaveAsync(entity).join())
            .isInstanceOf(CompletionException.class)
            .hasRootCauseMessage("save failed");
        assertThat(repository.getCache().getIfPresent("failing-save")).isSameAs(entity);
    }

    @Test
    void asyncFindOrCreatePassesTheIdToTheNewEntity() {
        CreatingRepository repository = new CreatingRepository(mongoClient);

        CreatableEntity created = repository.findOrCreateByIdAsync("new-id").join();

        assertThat(created.id).isEqualTo("new-id");
        assertThat(repository.getCache().getIfPresent("new-id")).isSameAs(created);
        assertThat(repository.saved).containsExactly(created);
    }

    @Test
    void asyncFindOrCreateReturnsTheExistingEntity() {
        CreatingRepository repository = new CreatingRepository(mongoClient);
        CreatableEntity existing = new CreatableEntity("existing-id");
        repository.cacheObject(existing);

        assertThat(repository.findOrCreateByIdAsync("existing-id").join()).isSameAs(existing);
        assertThat(repository.saved).isEmpty();
    }

    private static void expireAllEntries(RecordingRepository repository) {
        repository.getCache().policy().expireAfterAccess().orElseThrow().setExpiresAfter(Duration.ZERO);
        repository.getCache().cleanUp();
    }

    @Test
    void aDocumentChangingMidSaveIsSkippedRatherThanFailingTheSave() {
        ChangingRepository repository = new ChangingRepository(mongoClient);
        repository.cacheObject(new TestEntity("changing"));

        assertThat(repository.saveAllToDatabase()).isNull();
    }

    static class TestEntity {
        final String id;

        TestEntity(String id) {
            this.id = id;
        }
    }

    static class ChangingRepository extends Repository<TestEntity> {

        ChangingRepository(MongoClient mongoClient) {
            super(mongoClient, "marmalade-test", "test-entities", "id", 1, TimeUnit.HOURS);
        }

        @Override
        protected String getId(TestEntity entity) {
            return entity.id;
        }

        @Override
        public Document entityToDocument(Object entity) {
            throw new ConcurrentModificationException();
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

    static class FailingSaveRepository extends Repository<TestEntity> {

        FailingSaveRepository(MongoClient mongoClient) {
            super(mongoClient, "marmalade-test", "test-entities", "id", 1, TimeUnit.HOURS);
        }

        @Override
        protected String getId(TestEntity entity) {
            return entity.id;
        }

        @Override
        public UpdateResult saveToDatabase(TestEntity entity) {
            throw new IllegalStateException("save failed");
        }
    }

    public static class CreatableEntity {
        public final String id;

        public CreatableEntity() {
            this.id = null;
        }

        public CreatableEntity(String id) {
            this.id = id;
        }
    }

    static class CreatingRepository extends Repository<CreatableEntity> {

        final List<CreatableEntity> saved = new CopyOnWriteArrayList<>();

        CreatingRepository(MongoClient mongoClient) {
            super(mongoClient, "marmalade-test", "creatable-entities", "id", 1, TimeUnit.HOURS);
        }

        @Override
        protected String getId(CreatableEntity entity) {
            return entity.id;
        }

        @Override
        public Result<CreatableEntity, RepositoryException> findById(String id) {
            CreatableEntity cached = getCache().getIfPresent(id);
            return cached != null
                ? Result.success(cached)
                : Result.failure(new RepositoryException("Not found: " + id));
        }

        @Override
        public UpdateResult saveToDatabase(CreatableEntity entity) {
            saved.add(entity);
            return null;
        }
    }
}
