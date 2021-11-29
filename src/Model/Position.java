package Model;

public class Position {
    private int x;
    private int y;

    public Position() {
        this.x = 0;
        this.y = 0;
    }

    public Position(int x, int y) {
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean equals(Object obj) {
        Position position = (Position) obj;
        return this.x == position.getX() &&
               this.y == position.getY();
    }

    @Override
    public int hashCode() {
        int hash = 1;
        final int prime = 17;

        hash = prime * hash + x;
        hash = prime * hash + y;

        return hash;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }
}