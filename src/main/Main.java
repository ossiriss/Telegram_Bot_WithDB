package main;

import my_bots.TravelBot;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.TelegramBotsApi;

/**
 * Created by Boris on 26-Sep-16.
 */
public class Main {
    public static void main(String[] args) {

        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new TravelBot());
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }
}
