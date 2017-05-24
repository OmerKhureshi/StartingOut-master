package com.application.db.DAOImplementation;

import com.application.db.DatabaseUtil;
import com.application.db.TableNames;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static com.application.db.TableNames.ELEMENT_TABLE;
import static com.application.db.TableNames.ELEMENT_TO_CHILD_TABLE;

public class ElementToChildDAOImpl {
    // public static boolean isTableCreated = false;

    public static void createTable() {
        if (!isTableCreated()) {
            try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {
                String sql = "CREATE TABLE " + ELEMENT_TO_CHILD_TABLE + " (" +
                        "parent_id INTEGER, " +  // todo define foreign key
                        "child_id INTEGER" +  // todo define foreign key
                        ")";
                ps.execute(sql);
                System.out.println(">> Creating table " + TableNames.ELEMENT_TO_CHILD_TABLE);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    public static boolean isTableCreated() {
        // if (!isTableCreated)  // No need to call DatabaseUtil method every time. Save time this way.
        //     isTableCreated = DatabaseUtil.isTableCreated(ELEMENT_TO_CHILD_TABLE);
        // return isTableCreated;
        return DatabaseUtil.isTableCreated(ELEMENT_TO_CHILD_TABLE);
    }

    public static void insert(int elementId, int childId) {
        if (!isTableCreated())
            createTable();

        try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {
            String sql = "INSERT INTO " + ELEMENT_TO_CHILD_TABLE + " VALUES( " +
                    elementId + ", " +
                    childId +
                    ")";
            ps.execute(sql);
//            System.out.println(TableNames.ELEMENT_TO_CHILD_TABLE + ": Inserted: " + sql);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void dropTable() {
        if (isTableCreated()) {
            try (Connection c = DatabaseUtil.getConnection(); Statement ps = c.createStatement()) {
                String sql= "Drop table " + TableNames.ELEMENT_TO_CHILD_TABLE;
                System.out.println(">> Dropping table " + TableNames.ELEMENT_TO_CHILD_TABLE);
                ps.execute(sql);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

    }

    static Connection conn;
    static Statement ps;
    static String sql;
    public static ResultSet selectWhere(String where) {
        if (isTableCreated()) {
            try  {
                conn = DatabaseUtil.getConnection();
                ps = conn.createStatement();
                sql = "SELECT * FROM " + TableNames.ELEMENT_TO_CHILD_TABLE + " WHERE " + where;
                ResultSet resultSet = ps.executeQuery(sql);
                //                resultSet.next();
                //                System.out.println(resultSet.getInt("id"));
                return resultSet;
            } catch (SQLException e) {
                System.out.println("Line that threw error: " + sql);
                e.printStackTrace();
            }
        }
        throw new IllegalStateException("Table does not exist. Hence cannot fetch any rows from it.");
    }

    public static void close() {
        try {
            ps.close();
            conn.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

}
