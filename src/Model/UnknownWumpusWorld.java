package Model;

import java.util.Hashtable;
import java.util.Set;

public class UnknownWumpusWorld {
    private Hashtable<Position, UnknownRoom> worldGrid;
    private boolean isWumpusAlive;
    private int wumpusRoomCount;
    private Position wumpusCoordinates;

    public UnknownWumpusWorld() {
        worldGrid = new Hashtable<>();
        isWumpusAlive = true;
        wumpusRoomCount = 0;
    }

    public Position getWumpusCoordinates() {
        int xWampusCoord = 0;
        int yWampusCoord = 0;

        Set<Position> keys = worldGrid.keySet();
        for (Position roomPosition : keys) {
            UnknownRoom room = worldGrid.get(roomPosition);
            if (room.getWumpus() == NavigationConstants.ROOM_STATUS_POSSIBLE) {
                xWampusCoord += roomPosition.getX();
                yWampusCoord += roomPosition.getY();
            }
        }

        xWampusCoord /= wumpusRoomCount;
        yWampusCoord /= wumpusRoomCount;

        this.wumpusCoordinates = new Position(xWampusCoord, yWampusCoord);

        return this.wumpusCoordinates;
    }

    public Hashtable<Position, UnknownRoom> getWorldGrid() {
        return worldGrid;
    }

    public boolean isWumpusAlive() {
        return isWumpusAlive;
    }

    public void setWumpusAlive(boolean wumpusAlive) {
        isWumpusAlive = wumpusAlive;
    }

    public int getWumpusRoomCount() {
        return wumpusRoomCount;
    }

    public void setWumpusRoomCount(int wumpusRoomCount) {
        this.wumpusRoomCount = wumpusRoomCount;
    }
}
