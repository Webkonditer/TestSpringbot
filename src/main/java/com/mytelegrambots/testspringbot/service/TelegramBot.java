package com.mytelegrambots.testspringbot.service;

import com.mytelegrambots.testspringbot.config.BotConfig;
import com.mytelegrambots.testspringbot.model.Ads;
import com.mytelegrambots.testspringbot.model.AdsRepository;
import com.mytelegrambots.testspringbot.model.User;
import com.mytelegrambots.testspringbot.model.UserRepository;
import com.vdurmont.emoji.EmojiParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.tomcat.util.digester.ArrayStack;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScope;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TelegramBot extends TelegramLongPollingBot {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AdsRepository adsRepository;

    private static final String HELP_TEXT = "This bot is created to demonstrate Spring capabilites.\n\n" +
            "You can execute commands from the main menu on the left or by typing a command:\n\n" +
            "Type /start to see a welcome message\n\n" +
            "Type /mydata to see data stored about yourself...\n\n";

    static final String YES_BUTTON = "YES_BUTTON";
    static final String NO_BUTTON = "NO_BUTTON";

    final BotConfig config;

    public TelegramBot(BotConfig config) {

        this.config = config;
        List<BotCommand> listOfCommands = new ArrayList<>();
        listOfCommands.add(new BotCommand("/start", "get greeting message"));
        listOfCommands.add(new BotCommand("/register", "register in telegram bot"));
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

            if(messageText.contains("/send") && chatId == config.getOwnerId()){
                var textToSend = EmojiParser.parseToUnicode(messageText.substring(messageText.indexOf(" ")));
                var users = userRepository.findAll();
                for(User user: users){
                    try {
                        sendMessage(user.getChatId(), textToSend);
                    } catch (TelegramApiException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else {

                switch (messageText) {
                    case "/start":
                        try {
                            registerUser(update.getMessage());
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

                    case "/register":

                        register(chatId);

                        break;

                    default:
                        try {
                            sendMessage(chatId, "Sorry, command was not recognised");
                        } catch (TelegramApiException e) {
                            throw new RuntimeException(e);
                        }
                }
            }
        } else if (update.hasCallbackQuery()){//Если пришел не текст, а нажата кнопка.
            String callBackData = update.getCallbackQuery().getData();
            long messageId = update.getCallbackQuery().getMessage().getMessageId();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            if (callBackData.equals(YES_BUTTON)){

                String text = "You pressed " + YES_BUTTON;
                executeEditMessageText(text, chatId, messageId);

            } else if(callBackData.equals(NO_BUTTON)){

                String text = "You pressed " + NO_BUTTON;
                executeEditMessageText(text, chatId, messageId);

            }
        }
    }

    private void register(long chatId) {

        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText("Do you really want to register?");

        //Создание кнопок внутри сообщения
        InlineKeyboardMarkup markupInline = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rowsInline = new ArrayList<>();
        List<InlineKeyboardButton> rowInline = new ArrayList<>();
        var yesButton = new InlineKeyboardButton();

        yesButton.setText("Yes");
        yesButton.setCallbackData(YES_BUTTON);

        var noButton = new InlineKeyboardButton();

        noButton.setText("No");
        noButton.setCallbackData(NO_BUTTON);

        rowInline.add(yesButton);
        rowInline.add(noButton);

        rowsInline.add(rowInline);

        markupInline.setKeyboard(rowsInline);
        message.setReplyMarkup(markupInline);

        executeMessage(message);

    }

    private void registerUser(Message msg) {
        if (userRepository.findById(msg.getChatId()).isEmpty()) {
            var chatId = msg.getChatId();
            var chat = msg.getChat();

            User user = new User();

            user.setChatId(chatId);
            user.setFirstName(chat.getFirstName());
            user.setLastName(chat.getLastName());
            user.setUserName(chat.getUserName());
            user.setRegisteredAt(new Timestamp(System.currentTimeMillis()));

            userRepository.save(user);
            log.info("user saved: " + user);
        }
    }

    private void startCommandReceived(long chatId, String firstName) throws TelegramApiException {
        String answer = EmojiParser.parseToUnicode("Hi, " + firstName + ", nice to meet you!" + ":blush:");
        log.info("Replaed to user " + firstName);
        sendStartMessage(chatId, answer);
    }

    private void sendMessage(long chatId, String textToSend) throws TelegramApiException{
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);
        executeMessage(message);
    }

    private void sendStartMessage(long chatId, String textToSend) throws TelegramApiException{
        SendMessage message = new SendMessage();
        message.setChatId(String.valueOf(chatId));
        message.setText(textToSend);

        //Добавление клавиатуры к каждому собщению.
        ReplyKeyboardMarkup keyboardMarkup = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow row = new KeyboardRow();

        row.add("weather");//Добавление кнопки
        row.add("get random joke");

        keyboardRows.add(row);//Добавление строки кнопок

        row = new KeyboardRow();

        row.add("register");
        row.add("check my data");
        row.add("delete my data");

        keyboardRows.add(row);

        keyboardMarkup.setKeyboard(keyboardRows);//Формирование клавиатуры

        message.setReplyMarkup(keyboardMarkup);//Добавление клавиатуры к сообщению
        //----------------------------------------

        executeMessage(message);
    }

    private void executeEditMessageText(String text, long chatId, long messageId){
        EditMessageText message = new EditMessageText();
        message.setChatId(String.valueOf(chatId));
        message.setText(text);
        message.setMessageId((int)messageId);

        try{
            execute(message);
        } catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    private void executeMessage(SendMessage message){
        try{
            execute(message);
        } catch (TelegramApiException e){
            log.error("Error occurred: " + e.getMessage());
        }
    }

    //Запуск по расписанию
    @Scheduled(cron = "${cron.scheduler}")
    private void sendAds() throws TelegramApiException {

        var ads = adsRepository.findAll();
        var users = userRepository.findAll();

        for (User user: users){
            for (Ads ad: ads){
                sendMessage(user.getChatId(), ad.getAd());
            }
        }
    }
}
