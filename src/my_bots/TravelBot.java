package my_bots;

import model.Command;
import model.DBHelper;
import model.MyConstants;
import org.telegram.telegrambots.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
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
        int userID = update.getMessage().getFrom().getId();
        String answerText;
        Message message = update.getMessage();
        long chatID = message.getChat().getId();
        String messageText = message.getText();
        //debugging
        System.out.println(messageText);
        System.out.println(chatID);
        //System.out.println(userID);
        //end debugging

        if (messageText==null) return;
        if (messageText.substring(0,1).equals("/"))
            messageText = messageText.substring(1);
        else {
            sendMessage("not a command", message.getChatId().toString());
            return;
        }
        /*if (message.getFrom().getLastName() == null){
            sendMessage("You must have Last Name in telegram to use this bot", message.getChatId().toString());
            return;
        }*/
        try {
            if (messageText.equalsIgnoreCase(Command.HELP.toString()) || messageText.equalsIgnoreCase(Command.START.toString()))
                answerText = getHelp();
            else if (messageText.toUpperCase().matches(Command.JUMPIN.toString()))
                answerText = addTraveler(message.getFrom().getFirstName(), message.getFrom().getLastName(), userID, messageText, chatID);
            else if (messageText.equalsIgnoreCase(Command.GETOUT.toString()))
                answerText = removeTraveler(userID, chatID);
            /*else if ((messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3}") ||
                    (messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3} .*"))))
                answerText = expense(userID, messageText, message.getFrom().getFirstName());
            else if (messageText.equalsIgnoreCase(Command.SHOWEXP.toString()) || messageText.toUpperCase().matches
                    (Command.SHOWEXP.toString() + " \\d*"))
                answerText = showExpenses(messageText);
            else if (messageText.toUpperCase().matches(Command.DELEXP.toString() + " \\d+"))
                answerText = removeExpense(messageText, userID);
            else if (messageText.toUpperCase().matches(Command.CALC.toString()))
                answerText = calculateExpenses(messageText, userID);
            else if (messageText.toUpperCase().matches(Command.CALCTOTAL.toString()))
                answerText = calculateTotalExpenses(messageText, userID);
            else if (messageText.toUpperCase().matches(Command.ENDTRIP.toString() + " \\S+ \\S+"))
                answerText = endTrip(messageText, userID);*/
            else answerText = "Wrong command";
        } catch (Exception e) {
            answerText = "Error, some command trowed exception" + " " + e.getMessage();
            e.printStackTrace();
        }
        
        sendMessage(answerText, message.getChatId().toString());
    }

    private String removeTraveler(int userID, long chatID) {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip";
        }
        if (!DBHelper.getExpensesForUser(userID, chatID).isEmpty()){
            return "You can not leave because you have active expenses";
        }
        else {
            DBHelper.removeUserFromTrip(userID, chatID);
        }

        return "traveler removed from current trip";
    }

    private String addTraveler(String firstName, String lastName, int userID, String messageText, long chatID) {
        if (!DBHelper.getUsersList().contains(userID)){
            DBHelper.addUser(userID, firstName, lastName);
        }
        if (!DBHelper.getTripsList().contains(chatID)){
            DBHelper.createTrip(chatID, userID);
        }
        if (DBHelper.getUsersInTrip(chatID).contains(userID)){
            return "Traveler already present in current trip";
        }
        else {
            DBHelper.addUserToTrip(userID, chatID);
        }

        return "Traveler " + firstName + " " + lastName + " added";
    }

    private String getHelp() {
        String message = "List of commands:\n";
        message += "/JUMPIN - jump in selected trip\n";
        message += "/GETOUT - get out of current trip\n";
        message += "/EXP 'amount''currency' 'comment' - input expense; for example: /EXP 10.55EUR Comment. Comment - " +
                "optional\n";
        message += "/SHOWEXP 'days' - show current expenses. days - opitonal\n";
        message += "/DELEXP 'exp id' - remove expense\n";
        message += "/CALC - show debts for each\n";
        message += "/CALCTOTAL - show total debts for each\n";
        message += "/ENDTRIP 'Trip Name' 'Password' - Terminate trip. Only creator can do it.\n";

        return message;
    }

    private boolean sendMessage(String text, String chatId){
        SendMessage answer = new SendMessage();
        answer.setChatId(chatId);
        answer.setText(text);

        try {
            sendMessage(answer);
        } catch (TelegramApiException e) {
            return false;
        }

        return true;
    }

    @Override
    public String getBotUsername() {
        return MyConstants.BOT_USERNAME;
    }
}
