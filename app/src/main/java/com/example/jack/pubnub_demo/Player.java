package com.example.jack.pubnub_demo;

import com.pubnub.api.PubNub;
import com.pubnub.api.models.consumer.PNStatus;

/**
 * Created by Jack on 3/15/18.
 */

public class Player {

    //
    String username;
    String opponent;
    PubNub pn;
    boolean myTurn;


    /**
     * Constructor for player objects.
     * @param username Username of the player
     * @param opponent Player's opponent
     * @param pn The PubNub thru which the data is being sent.
     * @param myTurn If it is this player's turn.
     */
    public Player(String username, String opponent, PubNub pn, boolean myTurn){
        this.username = username;
        this.opponent = opponent;
        this.pn = pn;
        this.myTurn = myTurn;
    }

    /**
     * Returns if it is this player's turn.
     * @return This player's turn.
     */
    public boolean isMyTurn(){
        return myTurn;
    }
}
