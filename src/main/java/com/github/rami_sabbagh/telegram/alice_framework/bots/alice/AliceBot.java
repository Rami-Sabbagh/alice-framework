package com.github.rami_sabbagh.telegram.alice_framework.bots.alice;

import com.github.rami_sabbagh.telegram.alice_framework.commands.Command;
import com.github.rami_sabbagh.telegram.alice_framework.commands.CommandsHandler;
import com.github.rami_sabbagh.telegram.alice_framework.commands.Locality;
import com.github.rami_sabbagh.telegram.alice_framework.commands.Privacy;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;

import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Collection.ADMINS;
import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Collection.CHATS;
import static com.github.rami_sabbagh.telegram.alice_framework.bots.alice.AliceOptions.Command.*;

public abstract class AliceBot extends TelegramLongPollingBot {

    private static final Logger logger = LoggerFactory.getLogger(AliceBot.class);

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
     * <p>
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
    public final Pipe<Update> updatesPipe;

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

        updatesPipe = new ConsumeOncePipe<>();
        authorizer = new AuthorizeWithMongoDB(silent, botCreatorID, adminsCollection);
        chatsTracker = new ChatsTracker(botUsername, chatsCollection);
        commandsHandler = new CommandsHandler(botUsername, silent, authorizer);
        interactivityHandler = new RedisInteractivityHandler(redisNamespace, redisCommands);

        updatesPipe.registerHandler(chatsTracker);
        updatesPipe.registerHandler(commandsHandler);
        updatesPipe.registerHandler(interactivityHandler);

        if (options.enableDefaultCommand(PROMOTE))
            commandsHandler.registerCommand(new PromoteCommand(adminsCollection, silent, botCreatorID));
        if (options.enableDefaultCommand(DEMOTE))
            commandsHandler.registerCommand(new DemoteCommand(adminsCollection, silent, botCreatorID));

        if (options.enableDefaultCommand(CANCEL))
            commandsHandler.newCommand()
                    .name("cancel")
                    .description("Cancel the current operation 🛑")
                    .action((message, parsedCommand) -> interactivityHandler.deactivateListener(message.getChatId()))
                    .build();

        if (options.enableDefaultCommand(PING))
            commandsHandler.newCommand()
                    .name("ping")
                    .description("Pong 🏓")
                    .action((message, parsedCommand) -> silent.compose().text("Pong 🏓")
                            .chatId(message).send())
                    .build();

        if (options.enableDefaultCommand(UPDATE_COMMANDS))
            commandsHandler.newCommand()
                    .name("update_commands")
                    .privacy(Privacy.ADMIN)
                    .action((message, parsedCommand) -> {
                        Command[] commands = commandsHandler.getCommands();
                        List<BotCommand> botCommands = new ArrayList<>();

                        for (Command command : commands) {
                            if (command.locality == Locality.ALL) {
                                if (command.privacy == Privacy.PUBLIC || command.privacy == Privacy.GROUP_ADMIN) {
                                    if (command.description != null) {
                                        botCommands.add(new BotCommand()
                                                .setCommand(command.name)
                                                .setDescription(command.description));
                                    }
                                }
                            }
                        }

                        boolean success = silent.execute(new SetMyCommands().setCommands(botCommands));
                        if (success) logger.info("Updated bot commands definition");
                        else logger.error("Failed to update bot commands definition");
                        silent.compose().text(success ? "Updated commands definition successfully ✅" : "Failed to update commands definition ⚠")
                                .replyToOnlyInGroup(message).send();
                    })
                    .build();
    }

    private static DefaultBotOptions getDefaultBotOptions(AliceOptions options) {
        DefaultBotOptions botOptions = new DefaultBotOptions();
        botOptions.setMaxThreads(options.threadsCount());
        return botOptions;
    }

    @Override
    public void onUpdateReceived(Update update) {
        boolean consumed = updatesPipe.process(update);
        logger.trace(consumed ? "Consumed {}" : "Ignored {}", update);
    }

    @Override
    public void onClosing() {
        logger.info("Bot @{} shutting down...", getBotUsername());
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
