package dao;

import java.time.LocalDateTime;
import java.util.Date;

/**
 * Created by Boris on 26-Sep-16.
 */
public class Expense {
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
}
