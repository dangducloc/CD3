package Model;

import Utils.DB_handler;



public class Img {
    private int id;
    private String name_by_user;
    private String name_on_server;

    public Img(int id, String name_by_user, String name_on_server) {
        this.id = id;
        this.name_by_user = name_by_user;
        this.name_on_server = name_on_server;
    }
    public int getId() {
        return id;
    }
    public String getName_by_user() {
        return name_by_user;
    }
    public String getName_on_server() {
        return name_on_server;
    }
}
