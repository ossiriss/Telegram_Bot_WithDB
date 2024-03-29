package my_bots;

import dao.Expense;
import dao.Traveler;
import model.*;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.MessageEntity;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Boris on 26-Sep-16.
 */
public class TravelBot extends TelegramLongPollingBot {
    private static final String emo_regex = "([\\u20a0-\\u32ff\\ud83c\\udc00-\\ud83d\\udeff\\udbb9\\udce5-\\udbb9\\udcee])";

    private static String currencyToken;

    private final HashMap<Long, HashSet<Long>> paidAcceptedDict = new HashMap<>();

    @Override
    public String getBotToken() {
        return MyConstants.BOT_TOKEN;
    }

    @Override
    public void onUpdateReceived(Update update) {
        long userID = update.getMessage().getFrom().getId();
        String answerText;
        Message message = update.getMessage();
        long chatID = message.getChat().getId();
        Long oldChatId = update.getMessage().getMigrateFromChatId();
        if (oldChatId != null){
            try {
                DBHelper.updateChatId(oldChatId,chatID);
                paidAcceptedDict.put(chatID, paidAcceptedDict.getOrDefault(oldChatId, new HashSet<>()));
                sendMessage("chat ID updated", message.getChatId().toString());
            } catch (DBException e) {
                e.printStackTrace();
            }
            return;
        }
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
            //sendMessage("not a command", message.getChatId().toString());
            return;
        }
//        if (message.getFrom().getLastName() == null){
//            sendMessage("You must have Last Name in telegram to use this bot", message.getChatId().toString());
//            return;
//        }
        String senderFirstName = message.getFrom().getFirstName();
        senderFirstName = senderFirstName.replaceAll(emo_regex, "_");
        String senderLastName = message.getFrom().getLastName() == null ? " " : message.getFrom().getLastName();
        senderLastName = senderLastName.replaceAll(emo_regex, "_");
        try {
            updatePersonalData(userID, senderFirstName, senderLastName, message.getFrom().getUserName());
        } catch (DBException e) {
            e.printStackTrace();
            sendMessage("failed to update user data", message.getChatId().toString());
        }
        messageText = messageText.replace("@" + MyConstants.BOT_USERNAME, "");
        try {
            Long mentionedUserId = getMentionedUserId(message);

            if (messageText.equalsIgnoreCase(Command.HELP.toString()) || messageText.equalsIgnoreCase(Command.START.toString()))
                answerText = getHelp();
            else if (messageText.toUpperCase().matches(Command.JUMPIN.toString()))
                answerText = addTraveler(senderFirstName, senderLastName, userID, messageText, chatID, update.getMessage()
                        .getFrom().getUserName());
            else if (messageText.equalsIgnoreCase(Command.GETOUT.toString()))
                answerText = removeTraveler(userID, chatID);
            else if ((messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3}") ||
                    (messageText.toUpperCase().matches(Command.EXP.toString() + " \\d+\\.?\\d* ?[A-Z]{3} .*"))))
                answerText = expense(userID, chatID, messageText, senderFirstName);
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
                answerText = updatePersonalData(userID, senderFirstName, senderLastName, message.getFrom().getUserName());
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
            else if (messageText.toUpperCase().matches(Command.UPDATE.toString() + " \\d+ \\d+\\.?\\d* ?[A-Z]{3}"))
                answerText = update(chatID, userID, messageText);
            else if (messageText.toUpperCase().matches(Command.CALCTOTALAVERAGE.toString() + " [A-Z]{3}"))
                answerText = getTotalAverageInCurrency(chatID, messageText);
            else if (messageText.toUpperCase().matches(Command.FINALIZE.toString()))
                answerText = finalize(chatID, userID);
            else if (messageText.toUpperCase().matches(Command.UPDATETOKEN.toString() + " .+"))
                answerText = updateCurrencyToken(messageText.split(" ")[1]);
            else answerText = "Wrong command";
        } catch (Exception e) {
            e.printStackTrace();
            answerText = "Error, some command trowed exception: " + e.getMessage();
        }

        sendMessage(answerText, message.getChatId().toString());
    }

    public static String getCurrencyToken() throws IOException {
        if (currencyToken == null){
            byte[] encoded = Files.readAllBytes(Paths.get("currencyToken.txt"));
            currencyToken = new String(encoded, StandardCharsets.UTF_8);
        }

        return currencyToken;
    }

    private String updateCurrencyToken(String token) throws FileNotFoundException, UnsupportedEncodingException {
        PrintWriter writer = new PrintWriter("currencyToken.txt", "UTF-8");
        writer.println(token);
        writer.close();
        currencyToken = token;

        return "currency token updated successfully";
    }

    private String finalize(long chatID, long userID) throws Exception{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        Traveler tempTraveler = new Traveler("empty", "empty", userID);
        HashSet<Traveler> travelers = DBHelper.getTravelers(chatID);
        if (!travelers.contains(tempTraveler))
            return "Error: Traveler not found in current trip";

        HashSet<Long> acceptedUsers = paidAcceptedDict.getOrDefault(chatID, new HashSet<>());
        acceptedUsers.add(userID);

        if (acceptedUsers.size() == travelers.size()){
            DBHelper.setPaidForTrip(chatID);
            paidAcceptedDict.put(chatID, new HashSet<Long>());
            return "All expenses finalized successfully";
        }else{
            paidAcceptedDict.put(chatID, acceptedUsers);
            String reply = "Finalize confirmed, waiting for ";

            for (Traveler traveler : travelers){
                if (!acceptedUsers.contains(traveler.getUserId())){
                    reply = reply + traveler.getFirstName() + ", ";
                }
            }

            reply = reply.substring(0, reply.length() - 2);

            return  reply;
        }

    }

    private String getTotalAverageInCurrency(long chatID, String messageText) throws Exception{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        TreeMap<Expense, Traveler> map = new TreeMap<>(Collections.reverseOrder());
        map.putAll(DBHelper.getExpensesFromTrip(chatID));

        if (map.isEmpty()){
            return "Error: no expenses in current trip";
        }

        int commandLength = Command.CALCTOTALAVERAGE.toString().length() + 1;
        String currency = messageText.substring(commandLength, commandLength+3).toUpperCase();
        if (currency.equals("RUR")) currency = "RUB";

        return Calculator.getTotalAverageInCurrency(map, DBHelper.getTravelers(chatID), currency);
    }

    private String update(long chatID, long userID, String messageText) throws DBException{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        ArrayList<Long> usersInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersInTrip.contains(userID))
            return "Error: Traveler not found in current trip";

        int expenseID = -1;
        try {
            int startPosition = Command.UPDATE.toString().length() + 1;
            expenseID = Integer.parseInt(messageText.substring(startPosition, startPosition + messageText.substring(startPosition).indexOf(' ')));
        } catch (NumberFormatException e){
            return "wrong expense id";
        }

        Expense exp = DBHelper.getExpenseById(expenseID, chatID);

        if (exp == null){
            return "Error: expense not found";
        }
        if (exp.getUserID() != userID){
            return "Error: you can update only your expenses";
        }

        int startPosition = indexOf(Pattern.compile("\\d+\\.?\\d* ?[A-Za-z]{3}"), messageText);
        String amount = messageText.substring(startPosition, startPosition + indexOf(Pattern.compile("[A-Za-z]{3}"), messageText.substring(startPosition)));
        double numAmount = Double.parseDouble(amount);
        if (numAmount < 0.01) return "too small amount";
        numAmount = Math.round(numAmount*100.)/100.;
        int commandAmountLength = startPosition + amount.length();
        String currency = messageText.substring(commandAmountLength, commandAmountLength+3).toUpperCase();

        if (currency.equals("RUR")) currency = "RUB";

        DBHelper.updateExpense(numAmount, currency, chatID, expenseID);
        return "expense id " + expenseID + " was successfully updated";
    }

    private String exclude(long chatID, long userID, Long mentionedUserId, String messageText) throws DBException{
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        ArrayList<Long> usersInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersInTrip.contains(userID))
            return "Error: Traveler not found in current trip";
        if (!usersInTrip.contains(mentionedUserId))
            return "Error: target user not found in current trip";

        int expenseID = -1;
        boolean lastExpense = false;
        try {
            expenseID = Integer.parseInt(messageText.substring(messageText.lastIndexOf(' ')+1));
        } catch (NumberFormatException e){
        }

        Expense exp;
        if (expenseID > 0){
            exp = DBHelper.getExpenseById(expenseID, chatID);
        }else{
            lastExpense = true;
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
        if (lastExpense){
            return "Target user successfully excluded from last expense";
        }else {
            return "Target user successfully excluded from expense id " + expenseID;
        }
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

    private String credit(long userID, long chatID, String messageText, Long mentionedUserId) throws DBException{
        if (mentionedUserId == null) return "Wrong Command. You should mention target user";

        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        ArrayList<Long> usersInTrip = DBHelper.getUsersInTrip(chatID);

        if (!usersInTrip.contains(userID))
            return "Error: Traveler not found in current trip";
        if (!usersInTrip.contains(mentionedUserId))
            return "Error: target user not found in current trip";


        //int commandLength = Command.CALC.toString().length() + 1;
        int startPosition = indexOf(Pattern.compile("\\d+\\.?\\d* ?[A-Za-z]{3}"), messageText);
        String amount = messageText.substring(startPosition, startPosition + indexOf(Pattern.compile("[A-Za-z]{3}"), messageText.substring(startPosition)));
        double numAmount = Double.parseDouble(amount);
        if (numAmount < 0.01) return "too small amount";
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
        int newID = DBHelper.addExpense(numAmount, currency, comment, userID, chatID, mentionedUserId);

        paidAcceptedDict.put(chatID, new HashSet<Long>());

        return "expense " + newID + " confirmed: " + userName + " gave " + numAmount + " " + currency + " to " + targetUserName;
    }

    private Long getMentionedUserId(Message message) throws Exception{
        Long result = null;
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

    private String removeSponsorship(long chatID, long userID, Long targetUser) throws DBException {
        if (targetUser == null) return "Wrong command. You should mention user you want to stop sponsor";
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }
        ArrayList<Long> usersIdInTrip = DBHelper.getUsersInTrip(chatID);
        if (!usersIdInTrip.contains(userID))
            return "Error: Traveler not found in current trip";

        //int targetUser = Integer.parseInt(messageText.substring(Command.DELFUND.toString().length()+1));
        if (userID == targetUser) return "you can't fund yourself";

        if (!usersIdInTrip.contains(targetUser))
            return "Error: object of funding not found in current trip";

        DBHelper.setMerge(chatID, 0, targetUser);
        return "Funding removed successfully";
    }

    private String giveSponsorship(long chatID, long userID, Long targetUserID) throws DBException {
        if (targetUserID == null) return "Wrong command. You should mention user you want to sponsor";
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip, you need to enter trip first";
        }

        HashSet<Traveler> travelers = DBHelper.getTravelers(chatID);
        Traveler user = new Traveler("user", "user", userID);
        if (!travelers.contains(user))
            return "Error: Traveler not found in current trip";

        //int targetUser = Integer.parseInt(messageText.substring(Command.FUND.toString().length()+1));
        if (userID == targetUserID) return "you can't fund yourself";

        Traveler targetUser = new Traveler("user", "user", targetUserID);
        if (!travelers.contains(targetUser))
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

        DBHelper.setMerge(chatID, userID, targetUserID);
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

    private String updatePersonalData(long userID, String fname, String lname, String uName) throws DBException {
//        Matcher matcher = Pattern.compile(emo_regex).matcher(fname+lname);
//        while (matcher.find()) {
//            return "Retards with emoji characters inside name or surname can't use this bot!";
//        }

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

    private String removeExpense(String messageText, long userID, long chatID) throws DBException {
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
        map.putAll(DBHelper.getExpensesFromTrip(chatID, true));

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

        boolean passedPaid = false;
        Iterator entries = map.entrySet().iterator();
        while (entries.hasNext()) {
            Map.Entry<Expense, Traveler> thisEntry = (Map.Entry)entries.next();
            Expense expense = thisEntry.getKey(); Traveler traveler = thisEntry.getValue();

            if (!passedPaid && expense.isPaid()){
                answer = "\n-----------↑ Paid ↑----------" + answer;
                passedPaid = true;
            }

            if (sDate != null && !sDate.equals(expense.getDate().format(onlyDate))) {

                if (days == 1) {
                    answer = "\n" + sDate + answer;
                    return answer;
                }
                answer = "\n" + sDate + answer;
                if (days > 1) days--;
            }

            if (!expense.getExcludedUsers().isEmpty()){
                for (long excludedID : expense.getExcludedUsers()){
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

    private String expense(long userID, long chatID, String messageText, String firstName) throws DBException {
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

        int newID = DBHelper.addExpense(numAmount, currency, comment, userID, chatID, 0);

        paidAcceptedDict.put(chatID, new HashSet<Long>());
        return "expense " + newID + " confirmed: " + firstName + " paid " + numAmount + " " + currency;
    }

    private String removeTraveler(long userID, long chatID) throws DBException {
        if (!DBHelper.getTripsList().contains(chatID)){
            return "no such trip";
        }
        for (Traveler t : DBHelper.getExpensesFromTrip(chatID).values()) {
            if (t.getUserId() == userID) return "You can not leave because you have active expenses";
        }

        DBHelper.removeUserFromTrip(userID, chatID);

        return "traveler removed from current trip";
    }

    private String addTraveler(String firstName, String lastName, long userID, String messageText, long chatID, String username) throws DBException {
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
        message += "/CALCTOTALAVERAGE 'currency' - show average total spent in specific currency\n";
        message += "/UPDATENAME - update your name from your telegram account.\n";
        message += "/SHOWTRAVELERS - show travelers list\n";
        message += "/FUND 'user mention' - become sponsor for a traveler(expenses merged in '/calc')\n";
        message += "/DELFUND 'user mention' - stop sponsorship(unMerge)\n";
        message += "/SHOWFUNDED - show funded users list\n";
        message += "/EXCLUDE 'user mention' 'expense id' - exclude user from expense. Expense id - optional\n";
        message += "/UPDATE 'expense id' 'amount''currency'\n";
        message += "/FINALIZE - accept to finalize current expenses";


        return message;
    }

    private boolean sendMessage(String text, String chatId){
        String lines[] = text.split("\\r?\\n");
        StringBuilder stringBuilder = new StringBuilder("");
        ArrayList<String> messages = new ArrayList<String>();
        for (String line : lines) {
            if (stringBuilder.length() + line.length() > 4094){
                messages.add(stringBuilder.toString());
                stringBuilder.setLength(0);
            }
            stringBuilder.append(line);
            stringBuilder.append("\n");
        }
        if (stringBuilder.length() > 0){
            messages.add(stringBuilder.toString());
        }


        for (String messageText : messages){
            SendMessage answer = new SendMessage();
            answer.setChatId(chatId);
            answer.setText(messageText);

            try {
                execute(answer);
            } catch (TelegramApiException e) {
                return false;
            }
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
