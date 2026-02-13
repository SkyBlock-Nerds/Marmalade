package net.hypixel.nerdbot.marmalade.storage.database.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import net.hypixel.nerdbot.marmalade.storage.database.model.punishment.Punishment;
import net.hypixel.nerdbot.marmalade.storage.database.model.punishment.PunishmentType;
import net.hypixel.nerdbot.marmalade.storage.repository.Repository;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class PunishmentRepository extends Repository<Punishment> {

    private static final int MAX_QUERY_LIMIT = 500;
    private static final Map<String, Boolean> indexCreationStatus = new ConcurrentHashMap<>();

    public PunishmentRepository(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName, "punishments", "punishmentId", 30, TimeUnit.MINUTES);
        ensureIndexes();
    }

    @Override
    protected String getId(Punishment entity) {
        return entity.getPunishmentId();
    }

    public List<Punishment> findByTargetUserId(String targetUserId) {
        return queryDocuments(Filters.eq("targetUserId", targetUserId));
    }

    public List<Punishment> findByModeratorUserId(String moderatorUserId) {
        return queryDocuments(Filters.eq("moderatorUserId", moderatorUserId));
    }

    public List<Punishment> findByTargetAndType(String targetUserId, PunishmentType type) {
        Bson filter = Filters.and(
            Filters.eq("targetUserId", targetUserId),
            Filters.eq("type", type.name())
        );
        return queryDocuments(filter);
    }

    public long countByTargetUserId(String targetUserId) {
        return getMongoCollection().countDocuments(Filters.eq("targetUserId", targetUserId));
    }

    public long countByTargetAndType(String targetUserId, PunishmentType type) {
        Bson filter = Filters.and(
            Filters.eq("targetUserId", targetUserId),
            Filters.eq("type", type.name())
        );
        return getMongoCollection().countDocuments(filter);
    }

    private List<Punishment> queryDocuments(Bson filter) {
        List<Punishment> results = new ArrayList<>();
        for (Document doc : getMongoCollection()
            .find(filter)
            .sort(Sorts.descending("createdAt"))
            .limit(MAX_QUERY_LIMIT)) {

            Punishment punishment = documentToEntity(doc);
            cacheObject(punishment);
            results.add(punishment);
        }
        return results;
    }

    private void ensureIndexes() {
        MongoCollection<Document> collection = getMongoCollection();
        IndexOptions options = new IndexOptions().background(true);

        createIndexIfNotExists(collection, "targetUserId", Indexes.ascending("targetUserId"), options);
        createIndexIfNotExists(collection, "moderatorUserId", Indexes.ascending("moderatorUserId"), options);
        createIndexIfNotExists(collection, "type", Indexes.ascending("type"), options);
        createIndexIfNotExists(collection, "createdAt", Indexes.ascending("createdAt"), options);
    }

    private void createIndexIfNotExists(MongoCollection<Document> collection,
                                        String indexName, Bson indexKey, IndexOptions options) {
        String indexKeyStr = indexKey.toBsonDocument().toJson();
        String cacheKey = indexName + ":" + indexKeyStr;

        if (indexCreationStatus.getOrDefault(cacheKey, false)) {
            return;
        }

        try {
            for (Document idx : collection.listIndexes()) {
                String existingKey = idx.get("key", Document.class).toBsonDocument().toJson();
                if (existingKey.equals(indexKeyStr)) {
                    indexCreationStatus.put(cacheKey, true);
                    log.debug("Index {} already exists", indexName);
                    return;
                }
            }

            collection.createIndex(indexKey, options);
            indexCreationStatus.put(cacheKey, true);
            log.info("Created index {} in background", indexName);
        } catch (Exception e) {
            log.error("Failed to create index {}", indexName, e);
        }
    }
}
