package my_bots;

import dao.Expense;
import dao.Traveler;
import model.*;
import org.telegram.telegrambots.Constants;
import org.telegram.telegrambots.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.api.objects.ChatMember;
import org.telegram.telegrambots.api.objects.MessageEntity;
import org.telegram.telegrambots.exceptions.TelegramApiException;
import org.telegram.telegrambots.api.methods.send.SendMessage;
import org.telegram.telegrambots.api.objects.Message;
import org.telegram.telegrambots.api.objects.Update;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Boris on 26-Sep-16.
 */
public class TravelBot extends TelegramLongPollingBot {
    private static final String emo_regex = "([\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee])";

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
        try {
            updatePersonalData(userID, message.getFrom().getFirstName(), message.getFrom().getLastName(), message.getFrom().getUserName());
        } catch (DBException e) {
            e.printStackTrace();
            sendMessage("failed to update user data", message.getChatId().toString());
        }
        messageText = messageText.replace("@" + MyConstants.BOT_USERNAME, "");
        try {
            Integer mentionedUserId = getMentionedUserId(message);

            if (messageText.equalsIgnoreCase(Command.HELP.toString()) || messageText.equalsIgnoreCase(Command.START.toString()))
                answerText = getHelp();
            else if (messageText.toUpperCase().matches(Command.JUMPIN.toString()))
                answerText = addTraveler(message.getFrom().getFirstName(), message.getFrom().getLastName(), userID, messageText, chatID, update.getMessage()
                        .getFrom().getUserName());
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
            else if (messageText.toUpperCase().matches(Command.CALC.toString() + " [A-Z]{3}"))
                answerText = calculateExpensesInCurrency(chatID, messageText);
            else if (messageText.toUpperCase().matches(Command.CALCTOTAL.toString()))
                answerText = calculateTotalExpenses(chatID);
            else if (messageText.toUpperCase().matches(Command.UPDATENAME.toString()))
                answerText = updatePersonalData(userID, message.getFrom().getFirstName(), message.getFrom().getLastName(), message.getFrom().getUserName());
            else if (messageText.toUpperCase().matches(Command.SHOWTRAVELERS.toString()))
                answerText = showTravelers(chatID);
            else if (messageText.toUpperCase().matches(Command.FUND.toString() + " .+"))
                answerText = giveSponsorship(chatID, userID, mentionedUserId);
            else if (messageText.toUpperCase().matches(Command.DELFUND.toString() + " .+"))
                answerText = removeSponsorship(chatID, userID, mentionedUserId);
            else if ((messageText.toUpperCase().matches(Command.CREDIT.toString() + ".+ \\d+\\.?\\d* ?[A-Z]{3}") ||
                    (messageText.toUpperCase().matches(Command.CREDIT.toString() + ".+ \\d+\\.?\\d* ?[A-Z]{3} .*"))))
                answerText = credit(userID, chatID, messageText, mentionedUserId);
            else if (messageText.toUpperCase().matches(Command.SHOWFUNDED.toString()))
                answerText = showFunded(chatID);
            else if (messageText.toUpperCase().matches(Command.EXCLUDE.toString() + " .+( \\d+|$)"))
                answerText = exclude(chatID, userID, mentionedUserId, messageText);
            else answerText = "Wrong command";
        } catch (Exception e) {
            e.printStackTrace();
            answerText = "Error, some command trowed exception: " + e.getMessage();
        }

        sendMessage(answerText, message.getChatId().toString());
    }

    private String exclude(long chatID, int userID, Integer mentionedUserId, String messageText) throws DBException{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        ArrayList<Integer> usersInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersInTrip.contains(userID))
            return "Error: Traveler not found in current trip";
        if (!usersInTrip.contains(mentionedUserId))
            return "Error: target user not found in current trip";

        int expenseID = -1;
        try {
            expenseID = Integer.parseInt(messageText.substring(messageText.lastIndexOf(' ')+1));
        } catch (NumberFormatException e){

        }

        Expense exp;
        if (expenseID > 0){
            exp = DBHelper.getExpenseById(expenseID, chatID);
        }else{
            TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
            map.putAll(DBHelper.getExpensesFromTrip(chatID));
            exp = map.firstKey();
            expenseID = exp.getId();
        }

        if (exp == null){
            return "Error: expense not found";
        }
        if (exp.getTargetUserId() > 0){
            return "Error: can't exclude from 1to1 credits";
        }
        if (exp.getUserID() != userID){
            return "Error: you can exclude users only from your expenses";
        }
        if (exp.getExcludedUsers().contains(mentionedUserId)){
            return "Error: target user already excluded from this expense";
        }

        DBHelper.addExclude(chatID, mentionedUserId, expenseID);
        return "Target user successfully excluded";
    }

    private String showFunded(long chatID) throws DBException{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        String result = "Funding list(Sponsor -> Target):\n";
        int startLength = result.length();
        for (Traveler t1 : DBHelper.getTravelers(chatID)) {
            if (t1.getSponsorID() != 0) {
                for (Traveler t2 : DBHelper.getTravelers(chatID)) {
                    if (t1.getSponsorID() == t2.getUserId()) {
                        result += t2.getFirstName() + " " + t2.getLastName().substring(0, 1) + " -> " + t1.getFirstName() + " " + t1.getLastName().substring(0,
                                1) +
                        "\n";
                        break;
                    }
                }
            }
        }

        if(startLength == result.length()) return "there is no funded users in this trip";

        return result;
    }

    private String credit(int userID, long chatID, String messageText, Integer mentionedUserId) throws DBException{
        if (mentionedUserId == null) return "Wrong Command. You should mention target user";

        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        ArrayList<Integer> usersInTrip = DBHelper.getUsersInTrip(chatID);

        if (!usersInTrip.contains(userID))
            return "Error: Traveler not found in current trip";
        if (!usersInTrip.contains(mentionedUserId))
            return "Error: target user not found in current trip";


        //int commandLength = Command.CALC.toString().length() + 1;
        int startPosition = indexOf(Pattern.compile("\\d+\\.?\\d* ?[A-Za-z]{3}"), messageText);
        String amount = messageText.substring(startPosition, startPosition + indexOf(Pattern.compile("[A-Za-z]{3}"), messageText.substring(startPosition)));
        double numAmount = Double.parseDouble(amount);
        if (numAmount < 0.01) return "to small amount";
        numAmount = Math.round(numAmount*100.)/100.;
        int commandAmountLength = startPosition + amount.length();
        String currency = messageText.substring(commandAmountLength, commandAmountLength+3).toUpperCase();
        String comment = null;
        if (messageText.length() > commandAmountLength + 3)
            comment = messageText.substring(commandAmountLength + 3);

        if (currency.equals("RUR")) currency = "RUB";

        HashSet<Traveler> travelers = DBHelper.getTravelers(chatID);
        String userName = "user";
        String targetUserName = "user";
        for (Traveler t : travelers) {
            if (t.getUserId() == userID) userName = t.getFirstName();
            if (t.getUserId() == mentionedUserId) targetUserName = t.getFirstName();
        }
        DBHelper.addExpense(numAmount, currency, comment, userID, chatID, mentionedUserId);


        return "expense confirmed: " + userName + " gave " + numAmount + " " + currency + " to " + targetUserName;
    }

    private Integer getMentionedUserId(Message message) throws Exception{
        Integer result = null;
        for (MessageEntity messageEntity : message.getEntities()) {
            if (messageEntity.getType().contains("mention")){
                if (result != null) throw new Exception("command can contain maximum one user mention");
                if (messageEntity.getUser() != null){
                    result =  messageEntity.getUser().getId();
                } else{
                    String userName = message.getText().substring(messageEntity.getOffset() + 1, messageEntity.getOffset() + messageEntity.getLength());
                    result = DBHelper.getUserIdByUsername(userName);
                }
            }
        }
        return result;
    }

    private String removeSponsorship(long chatID, int userID, Integer targetUser) throws DBException {
        if (targetUser == null) return "Wrong command. You should mention user you want to stop sponsor";
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        ArrayList<Integer> usersIdInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersIdInTrip.contains(userID))
            return "Error: Traveler not found in current trip";

        //int targetUser = Integer.parseInt(messageText.substring(Command.DELFUND.toString().length()+1));
        if (userID == targetUser) return "you can't fund yourself";

        if (!usersIdInTrip.contains(targetUser))
            return "Error: object of funding not found in current trip";

        DBHelper.setMerge(chatID, 0, targetUser);
        return "Funding removed successfully";
    }

    private String giveSponsorship(long chatID, int userID, Integer targetUser) throws DBException {
        if (targetUser == null) return "Wrong command. You should mention user you want to sponsor";
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        HashSet<Traveler> travelers = DBHelper.getTravelers(chatID);
        Traveler user = new Traveler("user", "user", userID);
        if (!travelers.contains(user))
            return "Error: Traveler not found in current trip";

        //int targetUser = Integer.parseInt(messageText.substring(Command.FUND.toString().length()+1));
        if (userID == targetUser) return "you can't fund yourself";

        if (!travelers.contains(user))
            return "Error: object of funding not found in current trip";

        for (Traveler t : travelers) {
            if (t.equals(user)){
                if (t.getSponsorID() != 0){
                    return "Error: you already funded by other person";
                } else{
                    break;
                }
            }
        }

        DBHelper.setMerge(chatID, userID, targetUser);
        return "Funding set successfully";
    }

    private String showTravelers(long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        String result = "Travelers list(Name\tSurname):\n";
        for (Traveler t : DBHelper.getTravelers(chatID)) {
            result += t.getFirstName() + "\t" + t.getLastName() + "\n";
        }

        return result;
    }

    private String updatePersonalData(int userID, String fname, String lname, String uName) throws DBException {
        Matcher matcher = Pattern.compile(emo_regex).matcher(fname+lname);
        while (matcher.find()) {
            return "Retards with emoji characters inside name or surname can't use this bot!";
        }

        DBHelper.updatePersonalData(userID, fname, lname, uName);
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

    private String calculateExpensesInCurrency(long chatID, String messageText) throws Exception {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
        map.putAll(DBHelper.getExpensesFromTrip(chatID));

        if (map.isEmpty()){
            return "Error: no expenses in current trip";
        }

        int commandLength = Command.CALC.toString().length() + 1;
        String currency = messageText.substring(commandLength, commandLength+3).toUpperCase();
        if (currency.equals("RUR")) currency = "RUB";

        return Calculator.getTotalExpensesByTravelerInCurrency(map, DBHelper.getTravelers(chatID), currency);
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

    private String showExpenses(String messageText, long chatID) throws Exception {
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

            if (!expense.getExcludedUsers().isEmpty()){
                for (int excludedID : expense.getExcludedUsers()){
                    Traveler excludedTraveler = DBHelper.getTravelerByUserID(excludedID);
                    answer = excludedTraveler.getFirstName() + " " + excludedTraveler.getLastName().substring(0,1) + ".\t" + answer;
                }
                answer = "\nexcluded: " + answer;
            }

            if (expense.getDescription() != null) answer = expense.getDescription() + answer;

            if (expense.getTargetUserId() != 0) {
                Traveler target = DBHelper.getTravelerByUserID(expense.getTargetUserId());
                answer = "\n" + expense.getId() + ") " + traveler.getFirstName() + " " + traveler.getLastName().substring
                        (0, 1) + ". -> " + target.getFirstName() + " " + target.getLastName().substring
                        (0, 1) + ". " + expense.getSum() + " " + expense.getCurrency() + " " + answer;
            } else{
                answer = "\n" + expense.getId() + ") " + traveler.getFirstName() + " " + traveler.getLastName().substring
                        (0, 1) + ". " + expense.getSum() + " " + expense.getCurrency() + " " + answer;
            }

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

        DBHelper.addExpense(numAmount, currency, comment, userID, chatID, 0);

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

    private String addTraveler(String firstName, String lastName, int userID, String messageText, long chatID, String username) throws DBException {
        if (!DBHelper.getUsersList().contains(userID)){
            DBHelper.addUser(userID, firstName, lastName, username);
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
        message += "/CREDIT 'user mention' 'amount''currency' 'comment' - input direct expense to someone. for example: '/CREDIT @user 10.55EUR Comment' " +
                "Comment - optional\n";
        message += "/SHOWEXP 'days' - show current expenses. days - opitonal\n";
        message += "/DELEXP 'exp id' - remove expense\n";
        message += "/CALC - show debts for each\n";
        message += "/CALC 'currency' - show debts for each in specific currency\n";
        message += "/CALCTOTAL - show total debts for each\n";
        message += "/UPDATENAME - update your name from your telegram account.\n";
        message += "/SHOWTRAVELERS - show travelers list\n";
        message += "/FUND 'user mention' - become sponsor for a traveler(expenses merged in '/calc')\n";
        message += "/DELFUND 'user mention' - stop sponsorship(unMerge)\n";
        message += "/SHOWFUNDED - show funded users list\n";
        message += "/EXCLUDE 'user mention' 'expense id' - exclude user from expense\n";

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
