package my_bots;

import model.MyConstants;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

/**
 * Created by Boris on 26-Sep-16.
 */
public class TravelBot extends TelegramLongPollingBot {
    @Override
    public String getBotToken() {
        return MyConstants.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        System.out.println(update.getMessage().getText());
    }

    @Override
    public String getBotUsername() {
        return MyConstants.BOT_USERNAME;
    }
}
