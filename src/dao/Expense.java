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

    public Expense(double sum, String currency, String description, int id, LocalDateTime date) {
        this.sum = sum;
        this.currency = currency;
        this.description = description;
        this.date = date;
        this.id = id;
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

    public void setDescription(String description) {
        this.description = description;
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
        return result;
    }

    @Override
    public int compareTo(Expense o) {
        return date.compareTo(o.date);
    }
}
