package dao;

/**
 * Created by Boris on 11-Oct-16.
 */
public class Traveler {
    private String firstName;
    private String lastName;
    private int userId;
    private int sponsorID;
    private int weight = 1;

    public Traveler(String firstName, String lastName, int userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
    }

    public Traveler(String firstName, String lastName, int userId, int sponsorID) {
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

    public int getSponsorID() {
        return sponsorID;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public int getUserId() {
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
        return userId;
    }
}
