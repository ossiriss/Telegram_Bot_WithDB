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
    public static void updateExpense(double numAmount, String currency, long tripID, int expenseID) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update expenses " +
                    "set sum = ?, " +
                    "cur = ? " +
                    "where ID = ? " +
                    "and TripID = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setDouble(1, numAmount);
            preparedStmt.setString(2, currency);
            preparedStmt.setInt(3, expenseID);
            preparedStmt.setLong(4, tripID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'updateExpense'");
            throw new DBException("something gone wrong in 'updateExpense'");
        }
    }

    public static void addExclude(long tripID, long userID, int expenseID) throws DBException{
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into expense2excluded(expenseUK, userID) values ((select UK from expenses where ID = ? and tripID = ?), ?);";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setInt(1, expenseID);
            preparedStmt.setLong(2, tripID);
            preparedStmt.setLong(3, userID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'Exclude'");
            throw new DBException("something gone wrong in 'Exclude'");
        }
    }

    public static Traveler getTravelerByUserID(long userId) throws Exception{
        Traveler result = null;
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select Name, Surname from users where ID_Telegram = '" + userId + "'";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                result = new Traveler(rs.getString("Name"), rs.getString("Surname"), userId);
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getUserIdByUsername'");
            throw new DBException("something gone wrong in 'getUserIdByUsername'");
        }
        if (result == null) throw  new Exception("user not found in DB");

        return result;
    }

    public static long getUserIdByUsername(String userName) throws Exception{
        Long result = null;
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID_Telegram from users where Username = '" + userName + "'";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            if (rs.next()) {
                result = rs.getLong("ID_Telegram");
            }
            rs.close();
            st.close();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'getUserIdByUsername'");
            throw new DBException("something gone wrong in 'getUserIdByUsername'");
        }
        if (result == null) throw new Exception("Mentioned username not found in DB. This user should execute any command");

        return result;
    }

    public static ArrayList<Long> getUsersList() throws DBException{
        ArrayList<Long> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID_Telegram from users";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                long id = rs.getLong("ID_Telegram");
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

    public static void addUser(long userId, String name, String surname, String uName) throws DBException{
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users(ID_Telegram, Name, Surname, Username) values(?,?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, userId);
            preparedStmt.setString(2, name);
            preparedStmt.setString(3, surname);
            preparedStmt.setString(4, uName);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addUser'");
            throw new DBException("something gone wrong in 'addUser'");
        }
    }

    public static void createTrip(long chatId, long creatorId) throws DBException{
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into trips(Telegram_chat_id, createDate, Creator_ID) values(?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, chatId);
            preparedStmt.setDate(2, new java.sql.Date(Calendar.getInstance().getTimeInMillis()));
            preparedStmt.setLong(3, creatorId);

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

    public static ArrayList<Long> getUsersInTrip(long tripId) throws DBException {
        ArrayList<Long> result = new ArrayList<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select ID_Telegram from users2trips where TripID = " + tripId + "  and deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                long id = rs.getLong("ID_Telegram");
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

    public static void addUserToTrip(long userId, long tripId) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "insert into users2trips(ID_Telegram, TripID) values(?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, userId);
            preparedStmt.setLong(2, tripId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addUserToTrip'");
            throw new DBException("something gone wrong in 'addUserToTrip'");
        }
    }

    public static HashMap<Expense, Traveler> getExpensesFromTrip(long tripId) throws DBException {
        return getExpensesFromTrip(tripId, false);
    }

    public static HashMap<Expense, Traveler> getExpensesFromTrip(long tripId, boolean withPaid) throws DBException {
        HashMap<Expense, Traveler> result = new HashMap<>();

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String paidString = withPaid ? "" : "and e.paid = 0";
            String query = "select e.ID, e.expense_datetime, e.sum, e.cur, e.comment, e.userID, u.Name, u.Surname, u2t.Merge_parent_ID, e.targetUserId, e.paid, " +
                    "e2excl.userID " +
                    "from expenses e join users u on e.userID = u.ID_Telegram join users2trips u2t on e.userID = u2t.ID_Telegram and u2t.TripID = e.TripID " +
                    "left join expense2excluded e2excl on e.uk = e2excl.expenseUK where e.TripID = " + tripId +
                    " and e.deletedFlag = 0 " + paidString +
                    " order by e.ID, e2excl.userID";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            Expense lastExp = new Expense(0);
            Traveler lastTraveler = new Traveler("dummy", "dummy", 0);
            while (rs.next()) {
                int id = rs.getInt("ID");

                if (lastExp.getId() != id){
                    if(lastExp.getId() != 0) result.put(lastExp, lastTraveler);

                    Timestamp date = rs.getTimestamp("expense_datetime");
                    double sum = rs.getDouble("sum");
                    String cur = rs.getString("cur");
                    String comment = rs.getString("comment");
                    long userID = rs.getLong("e.userID");
                    String name = rs.getString("Name");
                    String surname = rs.getString("Surname");
                    long mergeParentID = rs.getLong("Merge_parent_ID");
                    long targetUserId = rs.getLong("targetUserId");
                    lastExp = new Expense(sum, cur, comment, id, date.toLocalDateTime(), userID, targetUserId);
                    lastExp.setPaid(rs.getBoolean("paid"));
                    lastTraveler = new Traveler(name, surname, userID, mergeParentID);
                }

                long excludedUser = rs.getLong("e2excl.userID");
                if (excludedUser != 0) lastExp.getExcludedUsers().add(excludedUser);
            }

            if (lastExp.getId() != 0){
                result.put(lastExp, lastTraveler);
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

    public static void removeUserFromTrip(long userId, long tripId) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users2trips " +
                    "set deletedFlag = 1 " +
                    "where ID_Telegram = ? " +
                    "and TripID = ?";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, userId);
            preparedStmt.setLong(2, tripId);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'removeUserFromTrip'");
            throw new DBException("something gone wrong in 'removeUserFromTrip'");
        }
    }

    public static int addExpense(double numAmount, String currency, String comment, long userID, long tripID, long targetUserId) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "select IFNULL(max(ID),0) from expenses where tripID = " + tripID;
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            int result = -1;
            if (rs.next()) {
                result = rs.getInt(1) + 1;
            }

            query = "insert into expenses(sum, cur, comment, tripID, userID, targetUserId) values(?,?,?,?,?,?)";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setDouble(1, numAmount);
            preparedStmt.setString(2, currency);
            preparedStmt.setString(3, comment);
            preparedStmt.setLong(4, tripID);
            preparedStmt.setLong(5, userID);
            preparedStmt.setLong(6, targetUserId);
            preparedStmt.execute();

            return result;
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'addExpense'");
            throw new DBException("something gone wrong in 'addExpense'");
        }
    }

    public static Expense getExpenseById(int expenseID, long chatID) throws DBException {
        Expense result = null;

        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "SELECT exp.ID, exp.expense_datetime, exp.sum, exp.cur, exp.comment, exp.userID, exp.targetUserId, exp2excl.userID FROM expenses " +
                    "exp left join expense2excluded exp2excl on exp.uk = exp2excl.expenseUK" +
                    " where exp.tripID = " + chatID +
                    " and exp.id = " + expenseID +
                    " and + exp.deletedFlag = 0";
            Statement st = conn.createStatement();
            ResultSet rs = st.executeQuery(query);
            while (rs.next()) {
                if (result == null) {
                    int id = rs.getInt("exp.ID");
                    Timestamp date = rs.getTimestamp("exp.expense_datetime");
                    double sum = rs.getDouble("exp.sum");
                    String cur = rs.getString("exp.cur");
                    String comment = rs.getString("exp.comment");
                    long user = rs.getLong("exp.userID");
                    long targetUserId = rs.getLong("exp.targetUserId");
                    result = new Expense(sum, cur, comment, id, date.toLocalDateTime(), user, targetUserId);
                }
                long excludedUser = rs.getLong("exp2excl.userID");
                if (excludedUser != 0) result.getExcludedUsers().add(excludedUser);
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

    public static void updatePersonalData(long userID, String fname, String lname, String uName) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users " +
                    "set Name = ?, surname = ?, username = ? " +
                    "where ID_Telegram = ? ";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setString(1, fname);
            preparedStmt.setString(2, lname);
            preparedStmt.setString(3, uName);
            preparedStmt.setLong(4, userID);

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
                long id = rs.getLong("ID_Telegram");
                String name = rs.getString("Name");
                String surname = rs.getString("Surname");
                long mergeParentID = rs.getLong("Merge_parent_ID");
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

    public static void setMerge(long chatID, long userID, long targetUser) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update users2trips " +
                    "set Merge_parent_ID = ? " +
                    "where ID_Telegram = ? " +
                    "and TripID = ? " +
                    "and deletedFlag = 0";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, userID);
            preparedStmt.setLong(2, targetUser);
            preparedStmt.setLong(3, chatID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'setMerge'");
            throw new DBException("something gone wrong in 'setMerge'");
        }
    }

    public static void updateChatId(long oldChatID, long newChatID) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query1 = "update expenses " +
                    "set TripID = ? " +
                    "where TripID = ?";
            PreparedStatement preparedStmt1 = conn.prepareStatement(query1);
            preparedStmt1.setLong(1, newChatID);
            preparedStmt1.setLong(2, oldChatID);
            preparedStmt1.execute();

            String query2 = "update trips " +
                    "set Telegram_chat_id = ? " +
                    "where Telegram_chat_id = ?";
            PreparedStatement preparedStmt2 = conn.prepareStatement(query2);
            preparedStmt2.setLong(1, newChatID);
            preparedStmt2.setLong(2, oldChatID);
            preparedStmt2.execute();

            String query3 = "update users2trips " +
                    "set TripID = ? " +
                    "where TripID = ?";
            PreparedStatement preparedStmt3 = conn.prepareStatement(query3);
            preparedStmt3.setLong(1, newChatID);
            preparedStmt3.setLong(2, oldChatID);
            preparedStmt3.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'updateChatId'");
            throw new DBException("something gone wrong in 'updateChatId'");
        }
    }

    public static void setPaidForTrip(long chatID) throws DBException {
        try (Connection conn = DriverManager.getConnection(MyConstants.DB_url, MyConstants.DB_username, MyConstants.DB_password)) {
            String query = "update expenses " +
                    "set paid = 1 " +
                    "where TripID = ? " +
                    "and deletedFlag = 0";
            PreparedStatement preparedStmt = conn.prepareStatement(query);

            preparedStmt.setLong(1, chatID);

            preparedStmt.execute();
        } catch (SQLException e) {
            e.printStackTrace();
            System.out.println("something gone wrong in 'setPaidForTrip'");
            throw new DBException("something gone wrong in 'setPaidForTrip'");
        }
    }
}