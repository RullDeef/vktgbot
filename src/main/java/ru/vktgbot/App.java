package ru.vktgbot;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import api.longpoll.bots.exceptions.VkApiException;

public class App {
    static final String TG_TOKEN = System.getenv("TG_TOKEN");
    static final long TG_CHAT_ID = Long.parseLong(System.getenv("TG_CHAT_ID"));

    static final String VK_API_TOKEN = System.getenv("VK_API_TOKEN");
    static final Integer VK_GROUP_ID = Integer.valueOf(System.getenv("VK_GROUP_ID"));

    static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        TgSenderBot tgBot = new TgSenderBot(TG_TOKEN, TG_CHAT_ID);
        VkLongPollBot vkBot = new VkLongPollBot(VK_GROUP_ID, VK_API_TOKEN, tgBot);

        vkBot.setExceptionTags(List.of("#Comics", "#Video", "#AD"));

        try
        {
            vkBot.startPolling();
        } catch (VkApiException exception) {
            logger.error("App.main", exception);
        }
    }
}
