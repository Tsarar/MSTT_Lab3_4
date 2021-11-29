package Model;

import java.util.ArrayList;
import java.util.HashMap;

public class Room {
    public ArrayList<String> Events = new ArrayList<>();

    public Room(HashMap<Integer, String> roomCodes, int... args) {
        for (int i: args){
            Events.add(roomCodes.get(i));
        }
    }
}
