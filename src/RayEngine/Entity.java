package RayEngine;

import java.awt.Graphics2D;

public class Entity {
    
    protected final Sprite s;
    
    protected double x;
    protected double y;
    
    private double distance; //distancia hacia el jugador
    
    //referencias
    protected Player p;
    protected Map map;
    protected Engine e;
    
    protected boolean visible = true;
    

    //constructores
    
    public Entity(Sprite s, double x, double y) {
        this.s = s;
        this.x = x;
        this.y = y;
    }
    
    public Entity(String path, double x, double y) {
        s = new Sprite(path);
        this.x = x;
        this.y = y;
    }
    
    public void addRef(Engine e, Player p, Map map) {
        this.e = e;
        this.p = p;
        this.map = map;
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    public Sprite getSprite() {
        return s;
    }
    
    //mostrar
    public void setVisible(boolean visible) {
        this.visible = visible;
    }
    
    //update y render
    
    public void update(double dt)  {
    }
    
    public void drawEntity(Graphics2D g) {
        int w = 6;
        g.fillRect((int) (x - w/2), (int) (y - w/2), w, w);
    }
    
    //metodos para el renderizado dentro del raycaster
    
    public final void updateDistance() {
        distance = Engine.distance(x, y, p.getX(), p.getY());
    }
    
    public final double getDistance() {
        return distance;
    }
}
