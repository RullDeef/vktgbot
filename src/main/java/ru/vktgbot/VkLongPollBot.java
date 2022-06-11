package ru.vktgbot;

import java.util.LinkedList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.longpoll.bots.LongPollBot;
import api.longpoll.bots.model.objects.additional.PhotoSize;
import api.longpoll.bots.model.objects.basic.WallPost;
import api.longpoll.bots.model.objects.media.Attachment;
import api.longpoll.bots.model.objects.media.AttachmentType;
import api.longpoll.bots.model.objects.media.Doc;
import api.longpoll.bots.model.objects.media.Photo;
import ru.vktgbot.core.WallPostEntity;

public class VkLongPollBot extends LongPollBot {
    private Logger logger;
    private String accessToken;
    private List<String> exceptionTags = List.of();
    private TgSenderBot tgBot;

    VkLongPollBot(Integer groupId, String accessToken, TgSenderBot tgBot) {
        super();
        this.accessToken = accessToken;
        this.tgBot = tgBot;
        logger = LoggerFactory.getLogger(VkLongPollBot.class);
    }

    public void setExceptionTags(List<String> newTags) {
        this.exceptionTags = newTags;
    }

    @Override
    public void onWallPostNew(WallPost wallPost) {
        logger.info("onWallPostNew: {}", wallPost);
        WallPostEntity entity = buildWallPostEntity(wallPost);

        if (entity.hasOneOfTags(exceptionTags)) {
            logger.info("post has exception tags. skipping...");
        } else {
            logger.info("prepared to send post");
            tgBot.sendPost(entity);
        }
    }

    @Override
    public String getAccessToken() {
        return accessToken;
    }

    private WallPostEntity buildWallPostEntity(WallPost wallPost) {
        String text = wallPost.getText();
        List<String> photoURLs = extractPhotoURLs(wallPost.getAttachments());
        List<String> gifURLs = extractGifURLs(wallPost.getAttachments());

        WallPost.Copyright postSource = wallPost.getCopyright();
        if (postSource != null) {
            String name = postSource.getName();
            String link = postSource.getLink();
            WallPostEntity.Copyright copyright = new WallPostEntity.Copyright(name, link);
            return new WallPostEntity(text, copyright, photoURLs, gifURLs);
        } else {
            return new WallPostEntity(text, photoURLs, gifURLs);
        }
    }

    private List<String> extractGifURLs(Iterable<Attachment> attachments) {
        logger.info("extracting gif urls from {}", attachments);

        if (attachments == null) {
            logger.info("nothing to extract here");
            return List.of();
        }

        List<String> urls = new LinkedList<>();
        for (Attachment attachment : attachments) {
            if (attachment.getType() == AttachmentType.DOCUMENT) {
                Doc doc = (Doc) attachment.getAttachmentObject();
                if ("gif".equals(doc.getExt())) {
                    logger.info("founded gif: {}", doc.getUrl());
                    urls.add(doc.getUrl());
                } else {
                    logger.warn("founded document, but it is not gif. skipping...");
                }
            }
        }

        logger.info("extracted: {}", urls);
        return urls;
    }

    private List<String> extractPhotoURLs(Iterable<Attachment> attachments) {
        logger.info("extracting photos urls from {}", attachments);

        if (attachments == null) {
            logger.info("nothing to extract here");
            return List.of();
        }
        
        List<String> urls = new LinkedList<>();
        for (Attachment attachment : attachments) {
            if (attachment.getType() == AttachmentType.PHOTO) {
                Photo photo = (Photo) attachment.getAttachmentObject();
                try {
                    String url = getBestPhotoURL(photo.getPhotoSizes());
                    urls.add(url);
                } catch (Exception e) {
                    logger.error("cannot attach photo from " + photo.toString(), e);
                }
            }
        }

        logger.info("extracted: {}", urls);
        return urls;
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
}
