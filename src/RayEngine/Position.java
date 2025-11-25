package RayEngine;

public class Position {
    public final int m;
    public final int n;
    
    public Position(int m, int n) {
        this.m = m;
        this.n = n;
    }
    
    @Override
    public String toString() {
        return m + " " + n;
    }
}
