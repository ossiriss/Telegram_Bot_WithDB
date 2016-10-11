package dao;

/**
 * Created by Boris on 11-Oct-16.
 */
public class Traveler {
    private String firstName;
    private String lastName;
    private int userId;

    public Traveler(String firstName, String lastName, int userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.userId = userId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public int getUserId() {
        return userId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Traveler traveler = (Traveler) o;

        if (userId != traveler.userId) return false;
        if (firstName != null ? !firstName.equals(traveler.firstName) : traveler.firstName != null) return false;
        return lastName != null ? lastName.equals(traveler.lastName) : traveler.lastName == null;

    }

    @Override
    public int hashCode() {
        int result = firstName != null ? firstName.hashCode() : 0;
        result = 31 * result + (lastName != null ? lastName.hashCode() : 0);
        result = 31 * result + userId;
        return result;
    }
}
