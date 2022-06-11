package ru.vktgbot;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pengrad.telegrambot.Callback;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.model.MessageEntity;
import com.pengrad.telegrambot.model.request.InlineKeyboardButton;
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup;
import com.pengrad.telegrambot.model.request.InputMediaPhoto;
import com.pengrad.telegrambot.request.BaseRequest;
import com.pengrad.telegrambot.request.SendAnimation;
import com.pengrad.telegrambot.request.SendMediaGroup;
import com.pengrad.telegrambot.request.SendPhoto;
import com.pengrad.telegrambot.response.BaseResponse;

import ru.vktgbot.core.WallPostEntity;

public class TgSenderBot extends TelegramBot {
    private Logger logger;
    private long chatId;

    public TgSenderBot(String accessToken, long chatId) {
        super(accessToken);
        this.chatId = chatId;
        logger = LoggerFactory.getLogger(TgSenderBot.class);
    }

    public void sendPost(WallPostEntity wallPost) {
        logger.info("TgSenderBot::sendPost: wallPost={}", wallPost);

        if (wallPost.getPhotoURLs().size() == 0) {
            logger.info("nothing to send :(");
        } else if (wallPost.getPhotoURLs().size() == 1) {
            logger.info("sending single photo");

            SendPhoto request = new SendPhoto(chatId, wallPost.getPhotoURLs().get(0))
                .caption(wallPost.buildCaption());

            if (wallPost.hasCopyright()) {
                request = request.replyMarkup(createKeyboard(wallPost.getCopyright()));
            }

            execute(request, new LoggerCallback<>());
        } else {
            logger.info("sending media group");

            List<InputMediaPhoto> media = extractMediaPhotos(wallPost.getPhotoURLs());
    
            if (wallPost.hasCopyright()) {
                String caption = wallPost.buildCaption();
                WallPostEntity.Copyright copyright = wallPost.getCopyright();

                media.set(0, media.get(0).caption(caption + "\n\n" + copyright.getName())
                    .captionEntities(new MessageEntity(MessageEntity.Type.text_link,
                        caption.length() + 2, copyright.getName().length())
                            .url(copyright.getLink())));
            } else {
                media.set(0, media.get(0).caption(wallPost.buildCaption()));
            }
    
            execute(new SendMediaGroup(chatId, media.toArray(new InputMediaPhoto[0])), new LoggerCallback<>());
        }

        // sending gifs
        for (String url : wallPost.getGifURLs()) {
            logger.info("sending animation: url={}", url);

            SendAnimation request = new SendAnimation(chatId,
                FileDownloader.downloadFile(url)).caption(wallPost.buildCaption());

            if (wallPost.hasCopyright()) {
                request = request.replyMarkup(createKeyboard(wallPost.getCopyright()));
            }

            execute(request, new LoggerCallback<>());
        }
    }

    private List<InputMediaPhoto> extractMediaPhotos(List<String> photoURLs) {
        return photoURLs.parallelStream()
            .flatMap(url -> Stream.of(new InputMediaPhoto(FileDownloader.downloadFile(url))))
            .collect(Collectors.toList());
    }

    private InlineKeyboardMarkup createKeyboard(WallPostEntity.Copyright copyright) {
        return createKeyboard(copyright.getName(), copyright.getLink());
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
            if (!response.isOk()) {
                logger.info("bad response!: {}", response.description());
            }
        }

        @Override
        public void onFailure(T request, IOException e) {
            logger.error("failed to send request: " + request.toString(), e);
        }
    }
}
