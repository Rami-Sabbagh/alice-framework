package com.github.rami_sabbagh.telegram.alice_framework.mongodb.commands;

import com.github.rami_sabbagh.telegram.alice_framework.commands.Command;
import com.github.rami_sabbagh.telegram.alice_framework.commands.Locality;
import com.github.rami_sabbagh.telegram.alice_framework.commands.ParsedCommand;
import com.github.rami_sabbagh.telegram.alice_framework.commands.Privacy;
import com.github.rami_sabbagh.telegram.alice_framework.utilities.SilentExecutor;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChat;
import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.User;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class DemoteCommand extends Command {

    private static final Logger logger = LoggerFactory.getLogger(DemoteCommand.class);

    protected final MongoCollection<Document> admins;
    protected final SilentExecutor silent;
    protected final int creatorID;

    public DemoteCommand(MongoCollection<Document> admins, SilentExecutor silent, int creatorId) {
        this(admins, silent, creatorId, "demote", "Demote a bot administrator.");
    }

    public DemoteCommand(MongoCollection<Document> admins, SilentExecutor silent, int creatorID, String name, String description) {
        super(name, description, Locality.USER, Privacy.ADMIN);
        this.admins = admins;
        this.silent = silent;
        this.creatorID = creatorID;
    }

    /**
     * Checks if a message is from the owner of the bot.
     *
     * @param message The message to check.
     * @return {@code true} if the message was from the owner.
     */
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private boolean isOwner(Message message) {
        return message.getFrom().getId() == creatorID;
    }

    @Override
    public void action(Message message, ParsedCommand parsedCommand) {

        if ((!message.isReply() || message.getReplyToMessage().getForwardFrom() == null) && parsedCommand.parameters.isBlank()) {
            silent.compose().markdown("Send this command as a reply to a __forwarded__ message from the user you wish to demote ℹ\n"
                    + "Or send the user id in `" + parsedCommand + " [userId]` ℹ")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        int toDemote; //The id of the user to demote.

        if (!parsedCommand.parameters.isBlank()) {
            try {
                toDemote = Integer.parseInt(parsedCommand.parameters);
                Chat toPromoteChat = silent.execute(new GetChat().setChatId((long) toDemote));
                if (toPromoteChat == null) {
                    silent.compose().markdown("Invalid `userId` ⚠")
                            .replyToOnlyInGroup(message).send();
                    return;
                }
            } catch (NumberFormatException e) {
                silent.compose().markdown("Invalid `userId` ⚠")
                        .replyToOnlyInGroup(message).send();
                return;
            }
        } else {
            Message replyTo = message.getReplyToMessage();
            User toDemoteUser = replyTo.getForwardFrom();

            if (toDemoteUser.getBot()) {
                silent.compose().text("Heh, I already don't trust any bot to administrate me 😏") //EASTER_EGG
                        .replyToOnlyInGroup(message).send();
                return;
            }

            toDemote = toDemoteUser.getId();
        }

        if (toDemote == creatorID) {
            silent.compose().text("I won't ever demote my owner ❕") //EASTER_EGG
                    .replyToOnlyInGroup(message).send();
            return;
        }

        Document profile = admins.find(eq("_id", toDemote)).first();
        if (profile == null) {
            silent.compose().text("The user is already not an admin to be demoted 😅")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        Integer promotedBy = profile.getInteger("promotedBy");
        if (promotedBy == null && !isOwner(message)) {
            silent.compose().text("Only the bot's owner can demote this user ⚠")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        if (promotedBy != null && !promotedBy.equals(message.getFrom().getId()) && !isOwner(message)) {
            String bossName;
            Chat bossChat = silent.execute(new GetChat().setChatId(message.getFrom().getId().longValue()));
            if (bossChat == null)
                bossName = "{User #" + message.getFrom().getId() + "}";
            else if (bossChat.getUserName() != null)
                bossName = "@" + bossChat.getUserName();
            else {
                bossName = bossChat.getFirstName();
                if (bossChat.getLastName() != null) bossName += " " + bossChat.getLastName();
                bossName += " #" + message.getFrom().getId();
            }

            silent.compose().text("This user can be only demoted by " + bossName + " or by the owner ⚠")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        boolean success = admins.deleteOne(eq("_id", toDemote)).wasAcknowledged();
        if (!success) {
            logger.error("Failed to demote user ({}) under a request by ({})", toDemote, message.getFrom().getId());
            silent.compose().text("An error occurred while demoting ⚠")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        success = admins.updateMany(eq("promotedBy", toDemote),
                set("promotedBy", message.getFrom().getId())).wasAcknowledged();

        if (!success) {
            logger.error("Failed to transfer sub-admins from ({}) to ({})", toDemote, message.getFrom().getId());
            silent.compose().text("An error occurred while transferring sub-admins ⚠")
                    .replyToOnlyInGroup(message).send();
            return;
        }

        logger.info("User ({}) got demoted into a normal user by ({})", toDemote, message.getFrom().getId());
        silent.compose().text("Demoted successfully ✅")
                .replyToOnlyInGroup(message).send();

    }
}
