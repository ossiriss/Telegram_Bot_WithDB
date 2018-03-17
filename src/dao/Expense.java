package dao;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by Boris on 26-Sep-16.
 */
public class Expense implements Comparable<Expense>{
    private double sum;
    private String currency;
    private String description;
    private LocalDateTime date;
    private int id;
    private int userID;
    private int targetUserId;

    public Expense(double sum, String currency, String description, int id, LocalDateTime date, int userID, int targetUserId) {
        this.sum = sum;
        this.currency = currency;
        this.description = description;
        this.date = date;
        this.id = id;
        this.userID = userID;
        this.targetUserId = targetUserId;
    }

    public int getTargetUserId() {
        return targetUserId;
    }

    public int getUserID() {
        return userID;
    }

    public double getSum() {
        return sum;
    }

    public int getId() {
        return id;
    }

    public String getCurrency() {
        return currency;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getDate() {
        return date;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Expense expense = (Expense) o;

        if (Double.compare(expense.sum, sum) != 0) return false;
        if (id != expense.id) return false;
        if (userID != expense.userID) return false;
        if (currency != null ? !currency.equals(expense.currency) : expense.currency != null) return false;
        if (description != null ? !description.equals(expense.description) : expense.description != null) return false;
        return date != null ? date.equals(expense.date) : expense.date == null;

    }

    @Override
    public int hashCode() {
        int result;
        long temp;
        temp = Double.doubleToLongBits(sum);
        result = (int) (temp ^ (temp >>> 32));
        result = 31 * result + (currency != null ? currency.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        result = 31 * result + (date != null ? date.hashCode() : 0);
        result = 31 * result + id;
        result = 31 * result + userID;
        return result;
    }

    @Override
    public int compareTo(Expense o) {
        int byDate = date.compareTo(o.date);
        if (byDate != 0){
            return byDate;
        } else{
            return id - o.id;
        }
    }

    @Override
    public String toString() {
        return "Expense{" +
                "sum=" + sum +
                ", currency='" + currency + '\'' +
                ", description='" + description + '\'' +
                ", date=" + date +
                ", id=" + id +
                ", userID=" + userID +
                '}';
    }

    public void setSum(double sum) {
        this.sum = sum;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
