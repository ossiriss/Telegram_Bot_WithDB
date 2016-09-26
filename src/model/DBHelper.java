package model;

import dao.Expense;

import java.sql.*;
import java.util.ArrayList;
import java.util.Calendar;

/**
 * Created by Boris on 26-Sep-16.
 */
public class DBHelper {
    public static void main(String[] args) throws SQLException {

    }

    public static ArrayList<Integer> getUsersList(){
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
            System.out.println("something goes wrong in 'getUsersList'");
        }

        return result;
    }

    public static void addUser(int userId, String name, String surname){
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users(ID_Telegram, Name, Surname) values(?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userId);
            preparedStmt.setString(2, name);
            preparedStmt.setString(3, surname);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something goes wrong in 'addUser'");
        }
    }

    public static void createTrip(long chatId, int creatorId){
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into trips(Telegram_chat_id, createDate, Creator_ID) values(?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, chatId);
            preparedStmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            preparedStmt.setInt(3, creatorId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something goes wrong in 'CreateTrip'");
        }
    }

    public static ArrayList<Long> getTripsList(){
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
            System.out.println("something goes wrong in 'getTripsList'");
        }

        return result;
    }

    public static ArrayList<Integer> getUsersInTrip(long tripId){
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
            System.out.println("something goes wrong in 'getUsersInTrip'");
        }

        return result;
    }

    public static void addUserToTrip(int userId, long tripId){
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users2trips(ID_Telegram, TripID) values(?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, userId);
            preparedStmt.setLong(2, tripId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something goes wrong in 'addUserToTrip'");
        }
    }

    public static ArrayList<Expense> getExpensesForUser(int userId, long tripId){
        ArrayList<Expense> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID, expense_datetime, sum, cur, comment  " +
                    "from expenses where TripID = " + tripId + " " +
                    "and userID = " + userId + " " +
                    "and + deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                int id = rs.getInt("ID");
                Timestamp date = rs.getTimestamp("expense_datetime");
                double sum = rs.getDouble("sum");
                String cur = rs.getString("cur");
                String comment = rs.getString("comment");
                result.add(new Expense(sum, cur, comment, id, date.toLocalDateTime()));
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            System.out.println("something goes wrong in 'getExpensesForUser'");
        }

        return result;
    }

    public static void removeUserFromTrip(int userId, long tripId){
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
            System.out.println("something goes wrong in 'removeUserFromTrip'");
        }
    }

}
