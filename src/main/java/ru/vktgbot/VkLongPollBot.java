package ru.vktgbot;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.longpoll.bots.LongPollBot;
import api.longpoll.bots.model.objects.additional.PhotoSize;
import api.longpoll.bots.model.objects.basic.WallPost;
import api.longpoll.bots.model.objects.media.Attachment;
import api.longpoll.bots.model.objects.media.AttachmentType;
import api.longpoll.bots.model.objects.media.Photo;

public class VkLongPollBot extends LongPollBot {
    private Logger logger;
    private String accessToken;
    private List<String> exceptionTags;
    private TgSenderBot tgBot;

    VkLongPollBot(Integer groupId, String accessToken, TgSenderBot tgBot) {
        super();
        this.accessToken = accessToken;
        this.tgBot = tgBot;
        logger = LoggerFactory.getLogger(VkLongPollBot.class);
    }

    public void setExceptionTags(Iterable<String> newTags) {
        this.exceptionTags = StreamSupport.stream(newTags.spliterator(), false)
                .collect(Collectors.toList());
    }

    @Override
    public void onWallPostNew(WallPost wallPost) {
        logger.info("onWallPostNew: {}", wallPost);

        String text = wallPost.getText();
        if (hasExceptionTag(text)) {
            logger.info("post has exception tags. returning...");
            return;
        }

        String caption = extractCaption(text);
        
        List<String> photoURLs = new LinkedList<>();
        if (wallPost.getAttachments() != null) {
            for (Attachment attachment : wallPost.getAttachments()) {
                if (attachment.getType() == AttachmentType.PHOTO) {
                    try {
                        Photo photo = (Photo) attachment.getAttachmentObject();
                        photoURLs.add(getBestPhotoURL(photo.getPhotoSizes()));
                    } catch (Exception e) {
                        logger.error("cannot attach photo", e);
                    }
                }
            }
        }
        
        logger.info("prepared to send post");
        WallPost.Copyright postSource = wallPost.getCopyright();
        if (postSource != null) {
            String copyright = postSource.getName();
            String link = postSource.getLink();
            tgBot.sendPost(caption, copyright, link, photoURLs);
        } else {
            tgBot.sendPost(caption, photoURLs);
        }
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    private String getBestPhotoURL(List<PhotoSize> sizes) {
        if (sizes.isEmpty()) {
            throw new IndexOutOfBoundsException("PhotoSize list empty!");
        }

        PhotoSize best = sizes.get(0);

        for (int i = 1; i < sizes.size(); i++) {
            PhotoSize currSize = sizes.get(i);
            if (currSize.getWidth() > best.getWidth()) {
                best = currSize;
            }
        }

        logger.info("founded best size: {}x{}", best.getWidth(), best.getHeight());
        logger.info("best source: {}", best.getSrc());
        return best.getSrc();
    }

    private String extractCaption(String text) {
        List<String> tags = extractTags(text);
        StringJoiner joiner = new StringJoiner(" ");
        tags.forEach((String tag) -> joiner.add(tag));
        return joiner.toString();
    }

    private List<String> extractTags(String text) {
        Pattern pattern = Pattern.compile("(#.*?)@");
        Matcher matcher = pattern.matcher(text);

        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            String tag = matcher.group(1);
            logger.info("founded tag: {}", tag);
            result.add(tag);
        }

        return result;
    }

    private boolean hasExceptionTag(String text) {
        for (String tag : exceptionTags) {
            if (text.contains(tag)) {
                logger.info("post contains exception tag! skipping");
                return true;
            }
        }
        return false;
    }
}
