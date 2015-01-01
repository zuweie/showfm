package com.example.zuweie.showfm;

/**
 * Created by zuweie on 12/31/14.
 */
public class ClientID {
    static int id_counter = 0;
    private int myid = 0;

    public ClientID(){
        myid = id_counter++;
    }

   public int getClientID() {
        return myid;
    }
}
