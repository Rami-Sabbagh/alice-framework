package com.github.rami_sabbagh.telegram.alice_framework.bots.alice;

public abstract class AliceOptions {
    /* Required configuration */

    /**
     * Returns the authorization token of the bot.
     * @return The authorization token of the bot.
     */
    public abstract String botToken();

    /**
     * Returns the username of the bot.
     * @return The username of the bot.
     */
    public abstract String botUsername();

    /**
     * Returns the user id of the bot's creator.
     * @return The user id of the bot's creator.
     */
    public abstract int botCreatorId();

    /**
     * Returns the connection URI to the MongoDB of the bot.
     * @return The connection URI to the MongoDB of the bot.
     */
    public abstract String mongoConnectionURI();

    /**
     * Returns the connection URI to the redis database of the bot.
     * @return The connection URI to the redis database of the bot.
     */
    public abstract String redisConnectionURI();

    /* Customizable configuration */

    /**
     * Returns the number of threads used for async methods execution.
     * @return the number of threads used for async methods execution.
     */
    public int threadsCount() {
        return 1;
    }

    /**
     * Returns the MongoDB database name to use for the bot's collections.
     * @return The MongoDB database name to use for the bot's collections.
     */
    public String mongoDatabaseName() {
        return botUsername();
    }

    /**
     * Returns the name of the bot's MongoDB documents collection.
     * @param collection The collection to name.
     * @return The name of the bot's MongoDB documents collection.
     */
    public String mongoCollectionName(Collection collection) {
        return collection.defaultName();
    }

    /**
     * Returns the namespace to use for the bot's fields in the redis database.
     * @return The namespace to use for the bot's fields in the redis database.
     */
    public String redisNamespace() {
        return botUsername();
    }

    /**
     * Returns whether the default command should be enabled or not.
     * @param command The command under judgement.
     * @return Whether the default command should be enabled or not.
     */
    public boolean enableDefaultCommand(Command command) {
        return true;
    }

    public enum Collection {
        /**
         * A collection storing a list of the chats which the bot knows, and their types.
         */
        CHATS("chats"),

        /**
         * A collection storing the ids of the bot admins.
         */
        ADMINS("admins");

        private final String defaultName;

        Collection (String defaultName) {
            this.defaultName = defaultName;
        }

        /**
         * Returns the default name of the collection.
         * @return The default name of the collection.
         */
        protected String defaultName() {
            return defaultName;
        }
    }

    public enum Command {
        /**
         * A command for promoting new admins by existing ones.
         */
        PROMOTE,

        /**
         * A command for demoting existing admins.
         */
        DEMOTE,

        /**
         * The classic ping pong command.
         */
        PING,

        /**
         * A command for updating the commands definition of the bot.
         */
        UPDATE_COMMANDS
    }
}
