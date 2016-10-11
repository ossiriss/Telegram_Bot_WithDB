package model;

import dao.Expense;
import dao.Traveler;

import java.util.*;

/**
 * Created by Boris on 11-Oct-16.
 */
public class Calculator {

    public static String getTotalExpensesByTraveler(TreeMap<Expense, Traveler> expenses) {
        Set<Traveler> travelers = new HashSet<>();
        for (Map.Entry<Expense, Traveler> entry: expenses.entrySet()) {
            travelers.add(entry.getValue());
        }

        String result = "";
        ArrayList<String> resultList = new ArrayList<>();
        HashSet<String> currency = new HashSet<String>();
        for (Expense e:expenses.keySet() ) {
            currency.add(e.getCurrency());
        }

        for (String cur : currency) {
            LinkedHashMap<Traveler, Double> perTraveler = new LinkedHashMap<>();
            ArrayList<String> clients = new ArrayList<String>();
            ArrayList<Double> balance = new ArrayList<Double>();
            double totalForEach = 0.;
            for (Traveler t : travelers) {
                perTraveler.put(t, 0.);
            }
            for (Map.Entry<Expense, Traveler> entry: expenses.entrySet()) {
                Expense e = entry.getKey();
                Traveler t = entry.getValue();
                if (e.getCurrency().equals(cur)){
                    perTraveler.put(t, e.getSum() + perTraveler.get(t));
                    totalForEach += e.getSum();
                }
            }
            totalForEach = totalForEach/travelers.size();
            for (Map.Entry<Traveler, Double> entry: perTraveler.entrySet()) {
                Traveler t = entry.getKey();
                double d = entry.getValue();
                perTraveler.put(t, d-totalForEach);
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
}