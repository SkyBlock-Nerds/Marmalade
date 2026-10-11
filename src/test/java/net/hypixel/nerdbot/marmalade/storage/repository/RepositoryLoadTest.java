package net.hypixel.nerdbot.marmalade.storage.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.bson.Document;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RepositoryLoadTest {

    private static MongoClient mongoClient;

    @BeforeAll
    static void createMongoClient() {
        mongoClient = MongoClients.create("mongodb://127.0.0.1:1");
    }

    @AfterAll
    static void closeMongoClient() {
        mongoClient.close();
    }

    private static final List<Document> MIXED = List.of(
        new Document("id", "good-1"),
        new Document("id", "bad").append("broken", true),
        new Document("other", "no-id"),
        new Document("id", "good-2")
    );

    @Test
    void asyncLoadSkipsDocumentsThatFailToConvert() {
        FixedDocumentsRepository repository = new FixedDocumentsRepository(mongoClient, MIXED);

        repository.loadAllDocumentsIntoCacheAsync().join();

        assertThat(repository.getCache().asMap()).containsOnlyKeys("good-1", "good-2");
    }

    @Test
    void syncLoadSkipsDocumentsThatFailToConvert() {
        FixedDocumentsRepository repository = new FixedDocumentsRepository(mongoClient, MIXED);

        repository.loadAllDocumentsIntoCache();

        assertThat(repository.getCache().asMap()).containsOnlyKeys("good-1", "good-2");
    }

    @Test
    void getAllDocumentsSkipsDocumentsThatFailToConvert() {
        FixedDocumentsRepository repository = new FixedDocumentsRepository(mongoClient, MIXED);

        assertThat(repository.getAllDocuments()).extracting(entity -> entity.id).containsExactly("good-1", "good-2");
        assertThat(repository.getAllDocumentsAsync().join()).extracting(entity -> entity.id)
            .containsExactly("good-1", "good-2");
    }

    @Test
    void loadDoesNotReplaceAnAlreadyCachedEntity() {
        FixedDocumentsRepository repository = new FixedDocumentsRepository(mongoClient, MIXED);
        RepositoryTest.TestEntity live = new RepositoryTest.TestEntity("good-1");
        repository.cacheObject(live);

        repository.loadAllDocumentsIntoCacheAsync().join();

        assertThat(repository.getCache().getIfPresent("good-1")).isSameAs(live);
    }

    static class FixedDocumentsRepository extends Repository<RepositoryTest.TestEntity> {

        private final List<Document> documents;

        FixedDocumentsRepository(MongoClient mongoClient, List<Document> documents) {
            super(mongoClient, "marmalade-test", "fixed-entities", "id", 1, TimeUnit.HOURS, false);
            this.documents = documents;
        }

        @Override
        protected List<Document> findAllDocuments() {
            return documents;
        }

        @Override
        protected RepositoryTest.TestEntity documentToEntity(Document document) {
            if (document.getBoolean("broken", false)) {
                throw new IllegalStateException("cannot convert");
            }
            return new RepositoryTest.TestEntity(document.getString("id"));
        }

        @Override
        protected String getId(RepositoryTest.TestEntity entity) {
            return entity.id;
        }
    }
}
