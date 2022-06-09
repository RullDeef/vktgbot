package ru.vktgbot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.BaseResponse;

public class TgSenderBot extends TelegramBot {
    private Logger logger;
    private long chatId;

    public TgSenderBot(String accessToken, long chatId) {
        super(accessToken);
        this.chatId = chatId;
        logger = LoggerFactory.getLogger(TgSenderBot.class);
    }

    public void sendPost(String caption, Iterable<String> photoURLs) {
        logger.info("TgSenderBot::sendPost: caption={}", caption);

        InputMediaPhoto[] mediaArray = extractMediaPhotos(photoURLs);
        mediaArray[0] = mediaArray[0].caption(caption);

        execute(new SendMediaGroup(chatId, mediaArray), new LoggerCallback<>());
    }

    public void sendPost(String caption, String copyright, String link, Iterable<String> photoURLs) {
        logger.info("TgSenderBot::sendPost: caption={} copyright={} link={}", caption, copyright, link);

        InputMediaPhoto[] mediaArray = extractMediaPhotos(photoURLs);
        
        if (mediaArray.length == 1) {
            InlineKeyboardMarkup keyboard = createKeyboard(copyright, link);
            
            logger.info("sending single photo");
            execute(new SendPhoto(chatId, photoURLs.iterator().next())
                .replyMarkup(keyboard).caption(caption), new LoggerCallback<>());
        } else {
            mediaArray[0] = mediaArray[0].caption(caption + "\n\n" + copyright).captionEntities(
                new MessageEntity(MessageEntity.Type.text_link,
                    caption.length() + 2, copyright.length()).url(link)
            );
    
            logger.info("sending media group");
            execute(new SendMediaGroup(chatId, mediaArray), new LoggerCallback<>());
        }
    }

    private InputMediaPhoto[] extractMediaPhotos(Iterable<String> photoURLs) {
        List<InputMediaPhoto> media = new ArrayList<>();
        photoURLs.forEach(url -> media.add(new InputMediaPhoto(url)));
        InputMediaPhoto[] mediaArray = new InputMediaPhoto[media.size()];
        media.toArray(mediaArray);

        return mediaArray;
    }

    private InlineKeyboardMarkup createKeyboard(String copyright, String link) {
        return new InlineKeyboardMarkup(
                new InlineKeyboardButton(copyright).url(link));
    }

    private static class LoggerCallback<T extends BaseRequest<T, R>, R extends BaseResponse> implements Callback<T, R> {
        Logger logger = LoggerFactory.getLogger(LoggerCallback.class);

        @Override
        public void onResponse(T request, R response) {
            logger.info("sended successfully: request={}, response={}", request, response);
        }

        @Override
        public void onFailure(T request, IOException e) {
            logger.error("failed to send request: " + request.toString(), e);
        }
    }
}
