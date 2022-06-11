package ru.vktgbot.core;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WallPostEntity {
    private static Logger logger = LoggerFactory.getLogger(WallPostEntity.class);

    private List<String> tags;
    
    private List<String> photoURLs;
    private List<String> gifURLs;

    private Copyright copyright;

    public WallPostEntity(String text, List<String> photoURLs, List<String> gifURLs) {
        this.tags = extractTags(text);

        this.photoURLs = photoURLs;
        this.gifURLs = gifURLs;
    }

    public WallPostEntity(String text, Copyright copyright, List<String> photoURLs, List<String> gifURLs) {
        this.tags = extractTags(text);

        this.photoURLs = photoURLs;
        this.gifURLs = gifURLs;
        this.copyright = copyright;
    }

    public String buildCaption() {
        StringJoiner joiner = new StringJoiner(" ");
        tags.forEach((String tag) -> joiner.add(tag));
        return joiner.toString();
    }

    public List<String> getTags() {
        return tags;
    }

    public boolean hasOneOfTags(List<String> otherTags) {
        for (String tag : otherTags) {
            if (tags.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public List<String> getPhotoURLs() {
        return photoURLs;
    }

    public List<String> getGifURLs() {
        return gifURLs;
    }

    public boolean hasCopyright() {
        return copyright != null;
    }

    public Copyright getCopyright() {
        if (!hasCopyright()) {
            logger.error("tried to get null copyright", new NullPointerException());
        }
        return copyright;
    }

    @Override
    public String toString() {
        return "WallPostEntity{tags=" + tags + ",photoUrls=" + photoURLs + ",gifUrls=" + gifURLs + ",copyright="
            + (copyright == null ? "null" : copyright.toString()) + "}";
    }

    private static List<String> extractTags(String text) {
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

    public static class Copyright {
        private String name;
        private String link;

        public Copyright(String name, String link) {
            this.name = name;
            this.link = link;
        }

        public String getName() {
            return name;
        }

        public String getLink() {
            return link;
        }

        @Override
        public String toString() {
            return "Copyright{name=" + name + ",link=" + link + "}";
        }
    }
}
