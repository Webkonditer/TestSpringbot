package com.mytelegrambots.testspringbot.service;

import com.mytelegrambots.testspringbot.config.BotConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.digester.ArrayStack;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    private static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilites.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:" +
            "Type /start to see a welcome message\n\n" +
            "Type /start to see data stored about yourself...\n\n";
    final BotConfig config;

    public TelegramBot(BotConfig config) {

        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get greeting message"));
        listOfCommands.add(new BotCommand("/mydata", "get your data stored"));
        listOfCommands.add(new BotCommand("/deletedata", "delete my data"));
        listOfCommands.add(new BotCommand("/help", "info how use this bot"));
        listOfCommands.add(new BotCommand("/settings", "set your preferences"));
        try {
            this.execute(new SetMyCommands(listOfCommands, new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            log.error("Error setting bot`s command list: " + e.getMessage());
        }
    }

    @Override
    public String getBotUsername() {
        return config.getBotName();
    }

    @Override
    public String getBotToken() {
        return config.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()){
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            switch (messageText) {
                case "/start":
                    try {
                        startCommandReceived(chatId, update.getMessage().getChat().getFirstName());
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;

                case "/help":

                    try {
                        sendMessage(chatId, HELP_TEXT);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                default:
                    try {
                        sendMessage(chatId, "Sorry, comand was not recognised");
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
            }
        }
    }

    private void startCommandReceived(long chatId, String firstName) throws TelegramApiException {
        String answer = "Hi, " + firstName + ", nice to meet you!";
        log.info("Replaed to user " + firstName);
        sendMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) throws TelegramApiException{
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        try{
            execute(message);
        } catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }
}
