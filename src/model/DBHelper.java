package model;

import dao.Expense;
import dao.Traveler;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;

/**
 * Created by Boris on 26-Sep-16.
 */
public class DBHelper {
    public static void main(String[] args) throws Exception {

    }

    public static ArrayList<Integer> getUsersList() throws DBException{
        ArrayList<Integer> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID_Telegram from users";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID_Telegram");
                result.add(id);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getUsersList'");
            throw new DBException("something gone wrong in 'getUsersList'");
        }

        return result;
    }

    public static void addUser(int userId, String name, String surname) throws DBException{
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users(ID_Telegram, Name, Surname) values(?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userId);
            preparedStmt.setString(2, name);
            preparedStmt.setString(3, surname);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addUser'");
            throw new DBException("something gone wrong in 'addUser'");
        }
    }

    public static void createTrip(long chatId, int creatorId) throws DBException{
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into trips(Telegram_chat_id, createDate, Creator_ID) values(?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, chatId);
            preparedStmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            preparedStmt.setInt(3, creatorId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'CreateTrip'");
            throw new DBException("something gone wrong in 'CreateTrip'");
        }
    }

    public static ArrayList<Long> getTripsList() throws DBException {
        ArrayList<Long> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select Telegram_chat_id from trips where deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                long id = rs.getLong("Telegram_chat_id");
                result.add(id);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getTripsList'");
            throw new DBException("something gone wrong in 'getTripsList'");
        }

        return result;
    }

    public static ArrayList<Integer> getUsersInTrip(long tripId) throws DBException {
        ArrayList<Integer> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID_Telegram from users2trips where TripID = " + tripId + "  and deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID_Telegram");
                result.add(id);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getUsersInTrip'");
            throw new DBException("something gone wrong in 'getUsersInTrip'");
        }

        return result;
    }

    public static void addUserToTrip(int userId, long tripId) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users2trips(ID_Telegram, TripID) values(?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userId);
            preparedStmt.setLong(2, tripId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addUserToTrip'");
            throw new DBException("something gone wrong in 'addUserToTrip'");
        }
    }

    public static HashMap<Expense, Traveler> getExpensesFromTrip(long tripId) throws DBException {
        HashMap<Expense, Traveler> result = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select e.ID, e.expense_datetime, e.sum, e.cur, e.comment, e.userID, u.Name, u.Surname, u2t.Merge_parent_ID " +
                    "from expenses e, users u, users2trips u2t where e.TripID = " + tripId +
                    " and e.userID = u.ID_Telegram" +
                    " and e.deletedFlag = 0" +
                    " and e.userID = u2t.ID_Telegram" +
                    " and u2t.TripID = e.TripID";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp date = rs.getTimestamp("expense_datetime");
                double sum = rs.getDouble("sum");
                String cur = rs.getString("cur");
                String comment = rs.getString("comment");
                int userID = rs.getInt("userID");
                String name = rs.getString("Name");
                String surname = rs.getString("Surname");
                int mergeParentID = rs.getInt("Merge_parent_ID");

                result.put(new Expense(sum, cur, comment, id, date.toLocalDateTime(), userID), new Traveler(name, surname, userID, mergeParentID));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getExpensesForUser'");
            throw new DBException("something gone wrong in 'getExpensesForUser'");
        }

        return result;
    }

    public static void removeUserFromTrip(int userId, long tripId) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users2trips " +
                    "set deletedFlag = 1 " +
                    "where ID_Telegram = ? " +
                    "and TripID = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userId);
            preparedStmt.setLong(2, tripId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'removeUserFromTrip'");
            throw new DBException("something gone wrong in 'removeUserFromTrip'");
        }
    }

    public static void addExpense(double numAmount, String currency, String comment, int userID, long tripID) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into expenses(sum, cur, comment, tripID, userID) values(?,?,?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setDouble(1, numAmount);
            preparedStmt.setString(2, currency);
            preparedStmt.setString(3, comment);
            preparedStmt.setLong(4, tripID);
            preparedStmt.setInt(5, userID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addExpense'");
            throw new DBException("something gone wrong in 'addExpense'");
        }
    }

    public static Expense getExpenseById(int userID, long chatID) throws DBException {
        Expense result = null;

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "SELECT ID, expense_datetime, sum, cur, comment, userID FROM travelbase.expenses" +
                    " where tripID = " + chatID +
                    " and id = " + userID +
                    " and + deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp date = rs.getTimestamp("expense_datetime");
                double sum = rs.getDouble("sum");
                String cur = rs.getString("cur");
                String comment = rs.getString("comment");
                int user = rs.getInt("userID");
                result = new Expense(sum, cur, comment, id, date.toLocalDateTime(), user);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getExpenseById'");
            throw new DBException("something gone wrong in 'getExpenseById'");
        }

        return result;
    }

    public static void removeExpense(Expense expense, long chatID) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update expenses " +
                    "set deletedFlag = 1 " +
                    "where ID = ? " +
                    "and TripID = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, expense.getId());
            preparedStmt.setLong(2, chatID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'removeExpense'");
            throw new DBException("something gone wrong in 'removeExpense'");
        }
    }

    public static void updatePersonalData(int userID, String fname, String lname) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users " +
                    "set Name = ?, surname = ? " +
                    "where ID_Telegram = ? ";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setString(1, fname);
            preparedStmt.setString(2, lname);
            preparedStmt.setInt(3, userID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'updatePersonalData'");
            throw new DBException("something gone wrong in 'updatePersonalData'");
        }
    }

    public static HashSet<Traveler> getTravelers(long chatID) throws DBException {
        HashSet<Traveler> result = new HashSet<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select u.ID_Telegram, u.Name, u.Surname, u2t.Merge_parent_ID from users2trips u2t, users u" +
                    " where u2t.TripID = " + chatID +
                    " and u2t.deletedFlag = 0" +
                    " and u2t.ID_Telegram = u.ID_Telegram";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID_Telegram");
                String name = rs.getString("Name");
                String surname = rs.getString("Surname");
                int mergeParentID = rs.getInt("Merge_parent_ID");
                result.add(new Traveler(name, surname, id, mergeParentID));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getTravelers'");
            throw new DBException("something gone wrong in 'getTravelers'");
        }

        return result;
    }

    public static void setMerge(long chatID, int userID, int targetUser) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users2trips " +
                    "set Merge_parent_ID = ? " +
                    "where ID_Telegram = ? " +
                    "and TripID = ? " +
                    "and deletedFlag = 0";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userID);
            preparedStmt.setInt(2, targetUser);
            preparedStmt.setLong(3, chatID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'updatePersonalData'");
            throw new DBException("something gone wrong in 'updatePersonalData'");
        }
    }
}