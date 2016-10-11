package model;

import dao.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

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

    public static HashMap<Expense, Integer> getExpensesFromTrip(long tripId) throws DBException {
        HashMap<Expense, Integer> result = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID, expense_datetime, sum, cur, comment, userID " +
                    "from expenses where TripID = " + tripId + " " +
                    "and + deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp date = rs.getTimestamp("expense_datetime");
                double sum = rs.getDouble("sum");
                String cur = rs.getString("cur");
                String comment = rs.getString("comment");
                int userID = rs.getInt("userID");
                result.put(new Expense(sum, cur, comment, id, date.toLocalDateTime()), userID);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
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
}
