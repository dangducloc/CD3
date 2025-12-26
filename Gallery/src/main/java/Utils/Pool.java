package Utils;

//import Utils.DB_handler;
import Model.Img;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import java.util.ArrayList;

public class Pool {
    private static Connection dbh;

    private static Connection getConnection() throws SQLException {
        if (dbh == null || dbh.isClosed()) {
            dbh = new DB_handler().getConnection();
        }
        return dbh;
    }

    public ArrayList<Img> getAllImages() throws SQLException {
        Connection conn = getConnection();
        PreparedStatement pre_sta = conn.prepareStatement("SELECT * FROM imgs");
        ResultSet raw_data = pre_sta.executeQuery();
       //convert to ArrayList
       ArrayList<Img> images = new ArrayList<>();
         while (raw_data.next()) {
              images.add(new Img(
                raw_data.getInt("id"),
                raw_data.getString("name_by_user"),
                raw_data.getString("name_on_server")
              ));
         }
         return images;
    }

    public void insertImageWithCustomName(String customName, String nameOnServer) throws SQLException {
        Connection conn = getConnection();
        System.out.println("=== Linh ===");
        PreparedStatement pre_sta = conn.prepareStatement("INSERT INTO imgs (name_by_user, name_on_server) VALUES (?, ?)");
        pre_sta.setString(1, customName);
        pre_sta.setString(2, nameOnServer);
        pre_sta.executeUpdate();
    }

    public ArrayList<Img> searchImages(String query) throws SQLException {
        String sql = "SELECT * FROM imgs WHERE name_by_user LIKE "+"'%" + query + "%'";
        ResultSet raw_data = getConnection().prepareStatement(sql).executeQuery();

        ArrayList<Img> images = new ArrayList<>();
        while (raw_data.next()) {
            images.add(new Img(
                raw_data.getInt("id"),
                raw_data.getString("name_by_user"),
                raw_data.getString("name_on_server")
            ));
        }
        return images;
    }
}
