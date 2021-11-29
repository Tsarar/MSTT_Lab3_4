package Model;

public class UnknownRoom {
    private int exist;
    private int stench;
    private int breeze;
    private int pit;
    private int wumpus;
    private int ok;
    private int gold;
    private int noWay;

    public UnknownRoom() {
        this.exist = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.stench = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.breeze = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.pit = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.wumpus = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.ok = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.gold = NavigationConstants.ROOM_STATUS_NO_STATUS;
        this.noWay = NavigationConstants.ROOM_STATUS_NO_STATUS;
    }

    public void addEvent(String event_name) {
        switch (event_name) {
            case NavigationConstants.WUMPUS:
                this.setWumpus(NavigationConstants.ROOM_STATUS_TRUE);
                break;
            case NavigationConstants.PIT:
                this.setPit(NavigationConstants.ROOM_STATUS_TRUE);
                break;
            case NavigationConstants.BREEZE:
                this.setBreeze(NavigationConstants.ROOM_STATUS_TRUE);
                break;
            case NavigationConstants.STENCH:
                this.setStench(NavigationConstants.ROOM_STATUS_TRUE);
                break;
            case NavigationConstants.GOLD:
                this.setGold(NavigationConstants.ROOM_STATUS_TRUE);
                break;
            case NavigationConstants.START:
            case NavigationConstants.SCREAM:
            case NavigationConstants.BUMP:
                break;
        }
    }

    public int getExist() {
        return exist;
    }

    public void setExist(int exist) {
        this.exist = exist;
    }

    public int getStench() {
        return stench;
    }

    public void setStench(int stench) {
        this.stench = stench;
    }

    public int getBreeze() {
        return breeze;
    }

    public void setBreeze(int breeze) {
        this.breeze = breeze;
    }

    public int getPit() {
        return pit;
    }

    public void setPit(int pit) {
        this.pit = pit;
    }

    public int getWumpus() {
        return wumpus;
    }

    public void setWumpus(int wumpus) {
        this.wumpus = wumpus;
    }

    public int getOk() {
        return ok;
    }

    public void setOk(int ok) {
        this.ok = ok;
    }

    public int getGold() {
        return gold;
    }

    public void setGold(int gold) {
        this.gold = gold;
    }

    public int getNoWay() {
        return noWay;
    }

    public void setNoWay(int noWay) {
        this.noWay = noWay;
    }
}
