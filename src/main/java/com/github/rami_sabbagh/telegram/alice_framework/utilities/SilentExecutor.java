package com.github.rami_sabbagh.telegram.alice_framework.utilities;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.groupadministration.SetChatPhoto;
import org.telegram.telegrambots.meta.api.methods.send.*;
import org.telegram.telegrambots.meta.api.methods.stickers.AddStickerToSet;
import org.telegram.telegrambots.meta.api.methods.stickers.CreateNewStickerSet;
import org.telegram.telegrambots.meta.api.methods.stickers.SetStickerSetThumb;
import org.telegram.telegrambots.meta.api.methods.stickers.UploadStickerFile;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageMedia;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.bots.AbsSender;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.updateshandlers.SentCallback;

import java.io.Serializable;
import java.util.List;

/**
 * Executes Telegram methods <i>silently</i>, without throwing exceptions.
 * <p>
 * Intended for using the bots API without much of try/catch boilerplate filling the code.
 *
 * <ul>
 *     <li><b>On success:</b> the proper result object is returned.</li>
 *     <li><b>On failure:</b> a stacktrace is printed into {@code stderr}, and null is returned.</li>
 * </ul>
 */
public class SilentExecutor {

    private static final Logger logger = LoggerFactory.getLogger(SilentExecutor.class);

    public final AbsSender bot;

    public SilentExecutor(AbsSender bot) {
        this.bot = bot;
    }

    /**
     * Composes a message.
     *
     * @return a new {@code MessageBuilder}.
     */
    public MessageBuilder compose() {
        return new MessageBuilder(bot);
    }

    public <T extends Serializable, Method extends BotApiMethod<T>, Callback extends SentCallback<T>> boolean executeAsync(Method method, Callback callback) {
        try {
            bot.executeAsync(method, callback);
            return true;
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return false;
        }
    }

    public <T extends Serializable, Method extends BotApiMethod<T>> T execute(Method method) {
        try {
            return bot.execute(method);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendDocument sendDocument) {
        try {
            return bot.execute(sendDocument);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendPhoto sendPhoto) {
        try {
            return bot.execute(sendPhoto);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendVideo sendVideo) {
        try {
            return bot.execute(sendVideo);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendVideoNote sendVideoNote) {
        try {
            return bot.execute(sendVideoNote);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendSticker sendSticker) {
        try {
            return bot.execute(sendSticker);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendAudio sendAudio) {
        try {
            return bot.execute(sendAudio);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendVoice sendVoice) {
        try {
            return bot.execute(sendVoice);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public List<Message> execute(SendMediaGroup sendMediaGroup) {
        try {
            return bot.execute(sendMediaGroup);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Boolean execute(SetChatPhoto setChatPhoto) {
        try {
            return bot.execute(setChatPhoto);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Boolean execute(AddStickerToSet addStickerToSet) {
        try {
            return bot.execute(addStickerToSet);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Boolean execute(SetStickerSetThumb setStickerSetThumb) {
        try {
            return bot.execute(setStickerSetThumb);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Boolean execute(CreateNewStickerSet createNewStickerSet) {
        try {
            return bot.execute(createNewStickerSet);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public File execute(UploadStickerFile uploadStickerFile) {
        try {
            return bot.execute(uploadStickerFile);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Serializable execute(EditMessageMedia editMessageMedia) {
        try {
            return bot.execute(editMessageMedia);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }

    public Message execute(SendAnimation sendAnimation) {
        try {
            return bot.execute(sendAnimation);
        } catch (TelegramApiException e) {
            logger.error("Silent Telegram API exception", e);
            return null;
        }
    }
}
