package com.github.rami_sabbagh.telegram.alice_framework.bots.alice;

import com.github.rami_sabbagh.telegram.alice_framework.commands.CommandsHandler;
import com.github.rami_sabbagh.telegram.alice_framework.commands.authorizers.StandardAuthorizer;
import com.github.rami_sabbagh.telegram.alice_framework.interactivity.InteractivityHandler;
import com.github.rami_sabbagh.telegram.alice_framework.mongodb.ChatsTracker;
import com.github.rami_sabbagh.telegram.alice_framework.mongodb.commands.DemoteCommand;
import com.github.rami_sabbagh.telegram.alice_framework.mongodb.commands.PromoteCommand;
import com.github.rami_sabbagh.telegram.alice_framework.mongodb.commands.authorizers.AuthorizeWithMongoDB;
import com.github.rami_sabbagh.telegram.alice_framework.pipes.ConsumeOncePipe;
import com.github.rami_sabbagh.telegram.alice_framework.pipes.Pipe;
import com.github.rami_sabbagh.telegram.alice_framework.redis.interactivity.RedisInteractivityHandler;
import com.github.rami_sabbagh.telegram.alice_framework.utilities.SilentExecutor;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import io.lettuce.core.RedisClient;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import org.bson.Document;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.concurrent.ExecutorService;

import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Collection.ADMINS;
import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Collection.CHATS;
import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Command.*;

public abstract class AliceBot extends TelegramLongPollingBot {

    /**
     * The authorization token of the bot.
     */
    public final String botToken;

    /**
     * The username of the bot.
     */
    public final String botUsername;

    /**
     * The user id of the bot's creator.
     */
    public final int botCreatorID;

    /**
     * The MongoDB client of the bot.
     */
    public final MongoClient mongoClient;

    /**
     * The MongoDB database of the bot.
     */
    public final MongoDatabase mongoDatabase;

    /**
     * The MongoDB collection of the bot's admins.
     */
    public final MongoCollection<Document> adminsCollection;

    /**
     * The MongoDB collection of the bot's chats.
     */
    public final MongoCollection<Document> chatsCollection;

    /**
     * The Redis namespace of the bot.
     *
     * It's a string which prefixes all the fields which the bot uses in the database.
     */
    public final String redisNamespace;

    /**
     * The Redis client of the bot.
     */
    public final RedisClient redisClient;

    /**
     * The stateful Redis connection of the bot.
     */
    public final StatefulRedisConnection<String, String> redisConnection;

    /**
     * The Redis commands of the bot.
     */
    public final RedisCommands<String, String> redisCommands;

    /**
     * The ExecutorService for the async bot methods.
     */
    public final ExecutorService executor;

    /**
     * The SilentExecutor of the bot.
     */
    public final SilentExecutor silent;

    /**
     * The updates pipe of the bot.
     */
    public final Pipe<Update> updatePipe;

    /**
     * The commands authorizer of the bot.
     */
    public final StandardAuthorizer authorizer;

    /**
     * The chats tracker of the bot.
     */
    public final ChatsTracker chatsTracker;

    /**
     * The commands handler of the bot.
     */
    public final CommandsHandler commandsHandler;

    /**
     * The interactivity handler of the bot.
     */
    public final InteractivityHandler interactivityHandler;

    public AliceBot(AliceOptions options) {
        super(getDefaultBotOptions(options));

        botToken = options.botToken();
        botUsername = options.botUsername();
        botCreatorID = options.botCreatorId();

        mongoClient = MongoClients.create(options.mongoConnectionURI());
        mongoDatabase = mongoClient.getDatabase(options.mongoDatabaseName());
        adminsCollection = mongoDatabase.getCollection(options.mongoCollectionName(ADMINS));
        chatsCollection = mongoDatabase.getCollection(options.mongoCollectionName(CHATS));

        redisNamespace = options.redisNamespace();
        redisClient = RedisClient.create(options.redisConnectionURI());
        redisConnection = redisClient.connect();
        redisCommands = redisConnection.sync();

        executor = this.exe;
        silent = new SilentExecutor(this);

        updatePipe = new ConsumeOncePipe<>();
        authorizer = new AuthorizeWithMongoDB(silent, botCreatorID, adminsCollection);
        chatsTracker = new ChatsTracker(botUsername, chatsCollection);
        commandsHandler = new CommandsHandler(botUsername, silent, authorizer);
        interactivityHandler = new RedisInteractivityHandler(redisNamespace, redisCommands);

        updatePipe.registerHandler(chatsTracker);
        updatePipe.registerHandler(commandsHandler);
        updatePipe.registerHandler(interactivityHandler);

        if (options.enableDefaultCommand(PROMOTE))
            commandsHandler.registerCommand(new PromoteCommand(adminsCollection, silent, botCreatorID));
        if (options.enableDefaultCommand(DEMOTE))
            commandsHandler.registerCommand(new DemoteCommand(adminsCollection, silent, botCreatorID));
    }

    private static DefaultBotOptions getDefaultBotOptions(AliceOptions options) {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setMaxThreads(options.threadsCount());
        return botOptions;
    }

    @Override
    public void onUpdateReceived(Update update) {
        updatePipe.process(update);
    }

    @Override
    public void onClosing() {
        //Telegram API (Shutdown the async executor)
        super.onClosing();
        //MongoDB
        mongoClient.close();
        //Redis
        redisConnection.close();
        redisClient.shutdown();
    }

    @Override
    public String getBotToken() {
        return botToken;
    }

    @Override
    public String getBotUsername() {
        return botUsername;
    }
}
