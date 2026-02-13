package net.hypixel.nerdbot.marmalade.storage.database.repository;

import com.mongodb.client.MongoClient;
import net.hypixel.nerdbot.marmalade.storage.database.model.reminder.Reminder;
import net.hypixel.nerdbot.marmalade.storage.repository.Repository;

public class ReminderRepository extends Repository<Reminder> {

    public ReminderRepository(MongoClient mongoClient, String databaseName) {
        super(mongoClient, databaseName, "reminders", "uuid");
    }

    @Override
    protected String getId(Reminder entity) {
        return entity.getUuid().toString();
    }
}