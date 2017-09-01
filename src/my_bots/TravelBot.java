package my_bots;

import dao.Expense;
import dao.Traveler;
import model.*;
import org.telegram.telegrambots.Constants;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
        if (message.getFrom().getLastName() == null){
            sendMessage("You must have Last Name in telegram to use this bot", message.getChatId().toString());
            return;
        }
        messageText = messageText.replace("@" + MyConstants.BOT_USERNAME, "");
        try {
            if (messageText.equalsIgnoreCase(Command.HELP.toString()) || messageText.equalsIgnoreCase(Command.START.toString()))
                answerText = getHelp();
            else if (messageText.toUpperCase().matches(Command.JUMPIN.toString()))
                answerText = addTraveler(message.getFrom().getFirstName(), message.getFrom().getLastName(), userID, messageText, chatID);
            else if (messageText.equalsIgnoreCase(Command.GETOUT.toString()))
                answerText = removeTraveler(userID, chatID);
            else if ((messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3}") ||
                    (messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3} .*"))))
                answerText = expense(userID, chatID, messageText, message.getFrom().getFirstName());
            else if (messageText.equalsIgnoreCase(Command.SHOWEXP.toString()) || messageText.toUpperCase().matches
                    (Command.SHOWEXP.toString() + " \\d*"))
                answerText = showExpenses(messageText, chatID);
            else if (messageText.toUpperCase().matches(Command.DELEXP.toString() + " \\d+"))
                answerText = removeExpense(messageText, userID, chatID);
            else if (messageText.toUpperCase().matches(Command.CALC.toString()))
                answerText = calculateExpenses(chatID);
            else if (messageText.toUpperCase().matches(Command.CALCTOTAL.toString()))
                answerText = calculateTotalExpenses(chatID);
            else if (messageText.toUpperCase().matches(Command.UPDATENAME.toString()))
                answerText = updatePersonalData(userID, message.getFrom().getFirstName(), message.getFrom().getLastName());
            else if (messageText.toUpperCase().matches(Command.SHOWTRAVELERS.toString()))
                answerText = showTravelers(chatID);
            else if (messageText.toUpperCase().matches(Command.FUND.toString() + " \\d+"))
                answerText = giveSponsorship(chatID, userID, messageText);
            else if (messageText.toUpperCase().matches(Command.DELFUND.toString() + " \\d+"))
                answerText = removeSponsorship(chatID, userID, messageText);
            else answerText = "Wrong command";
        } catch (Exception e) {
            e.printStackTrace();
            answerText = "Error, some command trowed exception: " + e.getMessage();
        }

        sendMessage(answerText, message.getChatId().toString());
    }

    private String removeSponsorship(long chatID, int userID, String messageText) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        ArrayList<Integer> usersIdInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersIdInTrip.contains(userID))
            return "Error: Traveler not found in current trip";

        int targetUser = Integer.parseInt(messageText.substring(Command.DELFUND.toString().length()+1));
        if (userID == targetUser) return "you can't fund yourself";

        if (!usersIdInTrip.contains(targetUser))
            return "Error: object of funding not found in current trip";

        DBHelper.setMerge(chatID, 0, targetUser);
        return "Funding removed successfully";
    }

    private String giveSponsorship(long chatID, int userID, String messageText) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        ArrayList<Integer> usersIdInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersIdInTrip.contains(userID))
            return "Error: Traveler not found in current trip";

        int targetUser = Integer.parseInt(messageText.substring(Command.FUND.toString().length()+1));
        if (userID == targetUser) return "you can't fund yourself";

        if (!usersIdInTrip.contains(targetUser))
            return "Error: object of funding not found in current trip";

        DBHelper.setMerge(chatID, userID, targetUser);
        return "Funding set successfully";
    }

    private String showTravelers(long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        String result = "Travelers list(TelegramID\tName\tSurname):\n";
        for (Traveler t : DBHelper.getTravelers(chatID)) {
            result += t.getUserId() + "\t " + t.getFirstName() + "\t" + t.getLastName() + "\n";
        }

        return result;
    }

    private String updatePersonalData(int userID, String fname, String lname) throws DBException {
        DBHelper.updatePersonalData(userID, fname, lname);
        return "Data updated successfully";
    }

    private String calculateTotalExpenses(long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
        map.putAll(DBHelper.getExpensesFromTrip(chatID));

        if (map.isEmpty()){
            return "Error: no expenses in current trip";
        }

        return Calculator.getTotalExpenses(map, DBHelper.getTravelers(chatID));
    }

    private String calculateExpenses(long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
        map.putAll(DBHelper.getExpensesFromTrip(chatID));

        if (map.isEmpty()){
            return "Error: no expenses in current trip";
        }

        return Calculator.getTotalExpensesByTraveler(map, DBHelper.getTravelers(chatID));
    }

    private String removeExpense(String messageText, int userID, long chatID) throws DBException {
        int idToRemove = Integer.parseInt(messageText.substring(Command.DELEXP.toString().length()+1));
        Expense expense = DBHelper.getExpenseById(idToRemove, chatID);
        if (expense == null)
            return "Expense with such id not found";
        if (expense.getUserID() != userID){
            return "Cannot remove expense. Not enough rights";
        }
        else DBHelper.removeExpense(expense, chatID) ;

        return "Expense id " + idToRemove + " removed";
    }

    private String showExpenses(String messageText, long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
        map.putAll(DBHelper.getExpensesFromTrip(chatID));

        if (map.isEmpty()){
            return "Error: no expenses in current trip";
        }

        int days = -1;
        String answer = "";
        if (messageText.length() > Command.SHOWEXP.toString().length()){
            days = Integer.parseInt(messageText.substring(Command.SHOWEXP.toString().length()+1));
        }

        DateTimeFormatter onlyDate = DateTimeFormatter.ofPattern("dd-MM-yyyy");
        String sDate = null;

        Iterator entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Expense, Traveler> thisEntry = (Map.Entry)entries.next();
            Expense expense = thisEntry.getKey(); Traveler traveler = thisEntry.getValue();

            if (sDate != null && !sDate.equals(expense.getDate().format(onlyDate))) {

                if (days == 1) {
                    answer = "\n" + sDate + answer;
                    return answer;
                }
                answer = "\n" + sDate + answer;
                if (days > 1) days--;
            }

            if (expense.getDescription() != null) answer = expense.getDescription() + answer;

            answer = "\n" + expense.getId() + ") " + traveler.getFirstName() + " " + traveler.getLastName().substring
                    (0,1) + ". " + expense.getSum() + " " + expense.getCurrency() + " " + answer;

            sDate = expense.getDate().format(onlyDate);
            if (!entries.hasNext()){
                answer = "\n" + sDate + answer;
            }
        }

        return answer;
    }

    private String expense(int userID, long chatID, String messageText, String firstName) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        if (!DBHelper.getUsersInTrip(chatID).contains(userID))
            return "Error: Traveler not found in current trip";

        int commandLength = Command.EXP.toString().length() + 1;
        String amount = messageText.substring(commandLength, 4 + indexOf(Pattern.compile("[A-Za-z]{3}"), messageText
                .substring(commandLength)));
        double numAmount = Double.parseDouble(amount);
        if (numAmount < 0.01) return "to small amount";
        numAmount = Math.round(numAmount*100.)/100.;
        int commandAmountLength = commandLength + amount.length();
        String currency = messageText.substring(commandAmountLength, commandAmountLength+3).toUpperCase();
        String comment = null;
        if (messageText.length() > commandAmountLength + 3)
            comment = messageText.substring(commandAmountLength + 3);

        if (currency.equals("RUR")) currency = "RUB";

        DBHelper.addExpense(numAmount, currency, comment, userID, chatID);

        return "expense confirmed: " + firstName + " paid " + numAmount + " " + currency;
    }

    private String removeTraveler(int userID, long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip";
        }
        for (Traveler t : DBHelper.getExpensesFromTrip(chatID).values()) {
            if (t.getUserId() == userID) return "You can not leave because you have active expenses";
        }

        DBHelper.removeUserFromTrip(userID, chatID);

        return "traveler removed from current trip";
    }

    private String addTraveler(String firstName, String lastName, int userID, String messageText, long chatID) throws DBException {
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
        message += "/EXP 'amount''currency' 'comment' - input expense; for example: '/EXP 10.55EUR Comment'. Comment - " +
                "optional\n";
        message += "/SHOWEXP 'days' - show current expenses. days - opitonal\n";
        message += "/DELEXP 'exp id' - remove expense\n";
        message += "/CALC - show debts for each\n";
        message += "/CALCTOTAL - show total debts for each\n";
        message += "/UPDATENAME - update your name from your telegramm account.\n";
        message += "/SHOWTRAVELERS - show travelers list with their telegramID\n";
        message += "/FUND 'telegramID' - become sponsor for a traveler(expenses merged in '/calc')\n";
        message += "/DELFUND 'telegramID' - stop sponsorship(unMerge)\n";

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


    private static int indexOf(Pattern pattern, String s) {
        Matcher matcher = pattern.matcher(s);
        return matcher.find() ? matcher.start() : -1;
    }

    @Override
    public String getBotUsername() {
        return MyConstants.BOT_USERNAME;
    }
}
