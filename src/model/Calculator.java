package model;

import dao.Expense;
import dao.Traveler;

import java.io.IOException;
import java.util.*;

/**
 * Created by Boris on 11-Oct-16.
 */
public class Calculator {

    private static Traveler getTravelerByID(Set<Traveler> travelers, int id){
        Traveler sponsor = new Traveler("sponsor", "sponsor", id);
        for (Traveler trav:travelers ) {
            if (trav.equals(sponsor)){
                return trav;
            }
        }
        return sponsor;
    }

    public static String getTotalExpensesByTravelerInCurrency(TreeMap<Expense, Traveler> expenses, Set<Traveler> travelers, String currency) throws Exception{
        //TreeMap<Expense, Traveler> expensesInCurrency = new TreeMap<Expense, Traveler>();

        for(Map.Entry<Expense,Traveler> entry : expenses.entrySet()) {
            Expense expense = entry.getKey();
            Traveler traveler = entry.getValue();

            if (!expense.getCurrency().equals(currency)){
                expense.setSum(HttpHelper.convertCurrency(expense.getSum(), expense.getCurrency(), currency));
                expense.setCurrency(currency);
            }
        }

        return getTotalExpensesByTraveler(expenses, travelers);
    }

    public static String getTotalExpensesByTraveler(TreeMap<Expense, Traveler> expenses, Set<Traveler> travelers) {

        for (Traveler trav:travelers ) {
            for (Traveler t:travelers ) {
                if (trav.getSponsorID() == t.getUserId()){
                    t.increaseWeight();
                }
            }
        }

        String result = "";
        ArrayList<String> resultList = new ArrayList<String>();
        HashSet<String> currency = new HashSet<String>();
        for (Expense e:expenses.keySet() ) {
            currency.add(e.getCurrency());
        }

        for (String cur : currency) {
            LinkedHashMap<Traveler, Double> perTraveler = new LinkedHashMap<Traveler, Double>();
            ArrayList<String> clients = new ArrayList<String>();
            ArrayList<Double> balance = new ArrayList<Double>();
            double totalForEach = 0.;
            for (Traveler t : travelers) {
                if (t.getSponsorID() == 0)
                    perTraveler.put(t, 0.);
            }
            for (Map.Entry<Expense, Traveler> entry: expenses.entrySet()) {
                Expense e = entry.getKey();
                Traveler t = entry.getValue();
                if (e.getCurrency().equals(cur)){
                    if (t.getSponsorID()!=0){
                        t = getTravelerByID(travelers, t.getSponsorID());
                    }
                    perTraveler.put(t, e.getSum() + perTraveler.get(t));
                    if (e.getTargetUserId() != 0) {
                        Traveler trav = getTravelerByID(travelers, e.getTargetUserId());
                        if (trav.getSponsorID() != 0) trav = getTravelerByID(travelers, trav.getSponsorID());
                        perTraveler.put(trav, perTraveler.get(trav) - e.getSum());
                    }else if(!e.getExcludedUsers().isEmpty()){ //calculate for expenses with excluded travelers
                        int travelersForExpense = travelers.size() - e.getExcludedUsers().size();
                        for (Traveler trav : travelers) {
                            if (!e.getExcludedUsers().contains(trav.getUserId())){
                                if (trav.getSponsorID() != 0) trav = getTravelerByID(travelers, trav.getSponsorID());
                                perTraveler.put(trav, perTraveler.get(trav) - e.getSum()/travelersForExpense);
                            }
                        }
                    }else {
                        totalForEach += e.getSum();
                    }
                }
            }
            totalForEach = totalForEach/travelers.size();
            for (Map.Entry<Traveler, Double> entry: perTraveler.entrySet()) {
                Traveler t = entry.getKey();
                double d = entry.getValue();
                perTraveler.put(t, d-totalForEach*t.getWeight());
            }
            orderByValue(perTraveler, Comparator.<Double>naturalOrder().reversed());

            for (Map.Entry<Traveler, Double> entry: perTraveler.entrySet()) {
                Traveler t = entry.getKey();
                double d = entry.getValue();
                clients.add(t.getFirstName() + " " + t.getLastName().substring(0,1) + ".");
                balance.add(d);
            }
            int debtorPos = 0;
            int creditorPos = balance.size()-1;
            while (debtorPos < creditorPos){
                String curDebtorName = clients.get(debtorPos);
                double curDebtorBalance = balance.get(debtorPos);
                String curCreditorName = clients.get(creditorPos);
                double curCreditorBalance = balance.get(creditorPos);

                if (Math.abs(curDebtorBalance) < 0.01 || Math.abs(curCreditorBalance) < 0.01) break;

                double balanceSum = curDebtorBalance + curCreditorBalance;

                if (Math.abs(balanceSum) <= 0.01)
                {
                    resultList.add( curCreditorName +  " -> " + curDebtorName + " " + Math.round(curDebtorBalance*100.)
                            /100. + " " + cur + "\n");
                    //  берем следующего дебитора
                    debtorPos++;
                    //  берем следующего кредитора
                    creditorPos--;
                }
                //  если суммы кредитора не хватает, чтобы выплатить долг дебитору
                else if (balanceSum > 0.01)
                {
                    resultList.add( curCreditorName +  " -> " + curDebtorName + " " + -Math.round(curCreditorBalance*100.)
                            /100. + " " + cur + "\n");
                    //  выплачиваем что есть
                    balance.set(debtorPos, balance.get(debtorPos) + curCreditorBalance);
                    //  берем следующего кредитора
                    creditorPos--;
                }

                //  если сумма кредитора выше, чем долг дебитора
                else
                {
                    resultList.add( curCreditorName +  " -> " + curDebtorName + " " + Math.round(curDebtorBalance*100.)
                            /100. + " " + cur + "\n");
                    //  выплачиваем весь долг дебитора
                    balance.set(creditorPos, balance.get(creditorPos) + curDebtorBalance);
                    //  берем следующего дебитора
                    debtorPos++;
                }
            }

        }
        Collections.sort(resultList);
        for (String s : resultList) {
            result += s;
        }

        return result;
    }

    private static <K, V> void orderByValue(
            LinkedHashMap<K, V> m, Comparator<? super V> c) {
        List<Map.Entry<K, V>> entries = new ArrayList<>(m.entrySet());
        m.clear();
        entries.stream()
                .sorted(Comparator.comparing(Map.Entry::getValue, c))
                .forEachOrdered(e -> m.put(e.getKey(), e.getValue()));
    }

    public static String getTotalExpenses(TreeMap<Expense, Traveler> expenses, Set<Traveler> travelers) {

        String result = "Total spent: ";
        HashMap<String, Double> total = new HashMap<String, Double>();
        for (Expense e : expenses.keySet()) {
            if (e.getTargetUserId() != 0 || !e.getExcludedUsers().isEmpty()){
                continue;
            }
            if (total.containsKey(e.getCurrency()))
                total.put(e.getCurrency(), e.getSum() + total.get(e.getCurrency()));
            else
                total.put(e.getCurrency(), e.getSum());
        }
        for (Map.Entry<String, Double> entry: total.entrySet()) {
            result += Math.round(entry.getValue()*100.)/100. + entry.getKey() + ", ";
            entry.setValue(entry.getValue()/travelers.size());   //we live here "total for each"
        }
        result = result.replaceAll(", $", "");
        result += "\n";


        for (Traveler t : travelers) {
            String sTotal = t.getFirstName() + " " + t.getLastName()  + " must pay: ";
            HashMap<String, Double> totalForTraveler = new HashMap<String, Double>();
            for (String cur : total.keySet()) {
                totalForTraveler.put(cur, 0.);   //filling all currencies with zeros
            }
            for (Map.Entry<Expense, Traveler> entry: expenses.entrySet()) {   //fill map with total spends for traveler
                Traveler traveler = entry.getValue();
                Expense expense = entry.getKey();
                if (t.equals(traveler)){
                    if (!totalForTraveler.containsKey(expense.getCurrency())) totalForTraveler.put(expense.getCurrency(), 0.);
                    totalForTraveler.put(expense.getCurrency(),  totalForTraveler.get(expense.getCurrency()) + expense.getSum());
                } else if (t.getUserId() == expense.getTargetUserId()){
                    if (!totalForTraveler.containsKey(expense.getCurrency())) totalForTraveler.put(expense.getCurrency(), 0.);
                    totalForTraveler.put(expense.getCurrency(),  totalForTraveler.get(expense.getCurrency()) - expense.getSum());
                } else if (!expense.getExcludedUsers().isEmpty() && !expense.getExcludedUsers().contains(t.getUserId())){
                    if (!totalForTraveler.containsKey(expense.getCurrency())) totalForTraveler.put(expense.getCurrency(), 0.);
                    double debt = expense.getSum() / (travelers.size() - expense.getExcludedUsers().size());
                    totalForTraveler.put(expense.getCurrency(),  totalForTraveler.get(expense.getCurrency()) - Math.round(debt*100.)/100.);
                }
            }
            for (Map.Entry<String, Double> entry: totalForTraveler.entrySet()) {
                String cur = entry.getKey();
                double amount = entry.getValue();
                double totalAmount = total.get(cur) != null ? total.get(cur) : 0.;
                double sum = Math.round((totalAmount - amount)*100.)/100.;
                if (Math.abs(sum) > 0.01)
                    sTotal += sum + cur + ", ";
            }
            sTotal = sTotal.replaceAll(", $", "");
            result += sTotal + "\n";
        }

        return result;
    }

    public static String getTotalAverageInCurrency(TreeMap<Expense, Traveler> expenses, Set<Traveler> travelers, String currency) throws Exception{
        String result = "Average spent for person: ";

        for(Map.Entry<Expense,Traveler> entry : expenses.entrySet()) {
            Expense expense = entry.getKey();
            Traveler traveler = entry.getValue();

            if (!expense.getCurrency().equals(currency)){
                expense.setSum(HttpHelper.convertCurrency(expense.getSum(), expense.getCurrency(), currency));
                expense.setCurrency(currency);
            }
        }

        HashMap<String, Double> total = new HashMap<String, Double>();
        for (Expense e : expenses.keySet()) {
            if (e.getTargetUserId() != 0 || !e.getExcludedUsers().isEmpty()){
                continue;
            }
            if (total.containsKey(e.getCurrency()))
                total.put(e.getCurrency(), e.getSum() + total.get(e.getCurrency()));
            else
                total.put(e.getCurrency(), e.getSum());
        }
        for (Map.Entry<String, Double> entry: total.entrySet()) {
            result += Math.round(entry.getValue()/travelers.size()*100.)/100. + entry.getKey() + ", ";
            entry.setValue(entry.getValue()/travelers.size());
        }
        result = result.replaceAll(", $", "");
        result += "\n";

        return result;
    }
}
