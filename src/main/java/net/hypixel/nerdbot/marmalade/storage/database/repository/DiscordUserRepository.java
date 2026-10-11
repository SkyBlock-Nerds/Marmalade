package net.hypixel.nerdbot.marmalade.storage.database.repository;

import com.mongodb.client.MongoClient;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import lombok.extern.slf4j.Slf4j;
import net.hypixel.nerdbot.marmalade.storage.database.model.user.DiscordUser;
import net.hypixel.nerdbot.marmalade.storage.database.model.user.birthday.BirthdayData;
import net.hypixel.nerdbot.marmalade.storage.database.model.user.stats.LastActivity;
import net.hypixel.nerdbot.marmalade.storage.repository.Repository;
import org.bson.BsonType;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
public class DiscordUserRepository extends Repository<DiscordUser> {

    public DiscordUserRepository(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName, "users", "discordId", 1, TimeUnit.DAYS);
    }

    @Override
    protected String getId(DiscordUser entity) {
        return entity.getDiscordId();
    }

    @Override
    protected DiscordUser documentToEntity(org.bson.Document document) {
        DiscordUser user = super.documentToEntity(document);

        if (user.getDiscordId() == null) {
            throw new IllegalStateException("Found DiscordUser document with null discordId: " + document.toJson());
        }

        return user;
    }

    @Override
    public CompletableFuture<Void> loadAllDocumentsIntoCacheAsync() {
        return super.loadAllDocumentsIntoCacheAsync().thenCompose(unused ->
            CompletableFuture.runAsync(() -> {
                getAll().forEach(discordUser -> {
                    if (discordUser.getLastActivity() == null) {
                        log.info("Last activity for {} was null. Setting to default values!", discordUser.getDiscordId());
                        discordUser.setLastActivity(new LastActivity());
                    }

                    if (discordUser.getBirthdayData() == null) {
                        log.info("Birthday data for {} was null. Setting to default values!", discordUser.getDiscordId());
                        discordUser.setBirthdayData(new BirthdayData());
                    }

                    if (discordUser.getRoleIds() == null) {
                        log.info("Role ids for {} were null. Setting to default values!", discordUser.getDiscordId());
                        discordUser.setRoleIds(new ArrayList<>());
                    }

                    cacheObject(discordUser);
                });
            })
        );
    }

    static Bson birthdaySetFilter() {
        return Filters.type("birthdayData.birthDate", BsonType.STRING);
    }

    public List<String> findIdsWithBirthdaySet() {
        List<String> ids = new ArrayList<>();
        getMongoCollection().find(birthdaySetFilter())
            .projection(Projections.include("discordId"))
            .forEach(document -> {
                String id = document.getString("discordId");
                if (id != null) {
                    ids.add(id);
                }
            });
        return ids;
    }
}