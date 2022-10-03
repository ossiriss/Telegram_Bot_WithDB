package dao;

/**
 * Created by Boris on 11-Oct-16.
 */
public class Traveler {
    private String firstName;
    private String lastName;
    private long userId;
    private long sponsorID;
    private int weight = 1;

    public Traveler(String firstName, String lastName, long userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
    }

    public Traveler(String firstName, String lastName, long userId, long sponsorID) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
        this.sponsorID = sponsorID;
    }

    public int getWeight() {
        return weight;
    }

    public int increaseWeight(){
        return ++weight;
    }

    public long getSponsorID() {
        return sponsorID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public long getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Traveler traveler = (Traveler) o;

        return userId == traveler.userId;

    }

    @Override
    public int hashCode() {
        return Long.hashCode(userId);
    }
}
