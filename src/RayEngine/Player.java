package RayEngine;

import java.awt.*;
import java.awt.event.*;

/**
 *
 * @author trili
 */
public final class Player {
    
    protected int v; //velocidad del jugador
    protected double x;
    protected double y;
    
    //radio de la hitbox del jugador, para calcular colisiones
    protected int hitboxRadius;
    
    protected int lastMouseX;
    protected double angle; //angulo de la vista del jugador EN RADIANES
    protected double sensitivity;
    
    protected Input in;
    protected Map map;
    protected Engine e;
    
    protected Robot mouseController;
    
    //la posición de la casilla del mapa en el que está el jugador, se recalcula cada frame
    protected Position tile;
    
    public Player(int v, int x, int y) {
        this.v = v;
        this.x = x;
        this.y = y;
        this.hitboxRadius = 15;
        
        this.sensitivity = 0.3;
        
        try {
            mouseController = new Robot();
        } catch (AWTException ex) {
            mouseController = null;
        }
        
        this.angle = 0;
    }
    
    public DPoint getPlayerPos() {
        return new DPoint(x, y);
    }
    
    public double getX() {
        return x;
    }
    
    public double getY() {
        return y;
    }
    
    
    //para guardar una referencia de la clase Input de engine y acceder a los eventos de mouse y teclado del frame de engine
    public void addInput(Input i) {
        this.in = i;
        this.lastMouseX = in.getMouseX();
        mouseController.mouseMove(Engine.getWinWidth() / 2, Engine.getWinHeight() / 2); //mueve el mouse al centro
    }
    
    public void addMap(Map map) {
        this.map = map;
    }
    
    public void addEngine(Engine e) {
        this.e = e;
    }
    
    
    //regresa el angulo en grados
    public double getAngle() {
        return Math.toDegrees(angle);
    }
    
    
   //angulo real en radianes
    public double getRadAngle() {
        return angle;
    }
    
    //recibe el angulo y lo guarda convertido a radianes
    public void setAngle(double newAngle) {
        angle = Math.toRadians(newAngle);
        //angle = Engine.normalizeAngleRad(angle);
        normalizeAngle();
    }
    
    public void addAngle(double newAngle) {
        angle += Math.toRadians(newAngle);
        //angle = Engine.normalizeAngleRad(angle);
        normalizeAngle();
    }
    
    public void normalizeAngle() {
        angle = angle % (2 * Math.PI);
        if (angle < 0) angle += 2 * Math.PI;
    }
    
    public void setSensitibity(double sensitivity) {
        this.sensitivity = sensitivity;
    }
    
    
    public void update(double dt) {
        //si aun no se agrea la clase de input o el map solo no actualiza
        if (in == null || map == null) return;
        
        updateAngle();
        updateMovement(dt); //actualiza el movimiento del personaje
        //System.out.println(Math.toDegrees(angle));
    }
    
    
    protected void updateMovement(double dt) {
        double moveX = 0;
        double moveY = 0;

        //normaliza la velocidad con el dt
        double speed = v * dt;
        
        double cos = Math.cos(angle);
        double sin = Math.sin(angle);

        /*descompone el movimiento diagonal descrito por el angulo (angle) en sus componentes en x y en y, usando seno y coseno. De esta forma
        se obtiene el aumento para cada eje para llegar a la posición deseada individualmente. Ya que seno y coseno solo proporcionan valores
        entre -1 y 1, para obtener el incremento total los componentes se multiplican por la velocidad normalizada*/
        
        //adelante y atrás
        if (in.isKeyDown(KeyEvent.VK_W)) {
            moveX += cos * speed;
            moveY += sin * speed;
        }
        if (in.isKeyDown(KeyEvent.VK_S)) {
            moveX -= cos * speed;
            moveY -= sin * speed;
        }

        //izquierda y derecha
        if (in.isKeyDown(KeyEvent.VK_A)) {
            moveX += sin * speed;
            moveY -= cos * speed;
        }
        if (in.isKeyDown(KeyEvent.VK_D)) {
            moveX -= sin * speed;
            moveY += cos * speed;
        }

        //la nueva posición que tomará el personaje
        double newX = x + moveX;
        double newY = y + moveY;

        
        //si con newX no colisiona con pared lo asigna al personaje
        if (!hasCollision(newX, y, hitboxRadius)) x = newX;
        
        //si con newY no colisiona con pared lo asigna al personaje
        if (!hasCollision(x, newY, hitboxRadius)) y = newY;
        
        /*no revisa las colisiones con ambas posiciones nuevas (newX y newY) porque si lo hiciera, aunque solo un eje tenga colision, ninguna de las dos posiciones se actualizaria.
        Por eso checa ambas colisiones separadas, para que si encuentra una colision en un eje, solo se bloquee ese y puedas seguir moviendote en el otro eje*/

        tile = Map.getTile(x, y);
        //System.out.println(tile);
    }
    
    //revisa si una posicion colisiona con una pared en el radio especificado
    protected boolean hasCollision(double px, double py, int radius) {
        //las 8 posibles direcciones en las que podria detectarse una colision
        int[][] directions = {
            {-radius, -radius}, {0, -radius},
            {radius, -radius}, {radius, 0},
            {radius, radius}, {0, radius},
            {-radius, radius}, {-radius, 0}
        };
        
        for (int[] i: directions) {
            if (map.insideOfWall(px + i[0], py + i[1])) {
                return true;
            }
        }
        return false;
    }
    
    
    protected void updateAngle() {
        Point canvas = e.getCanvasPos(); //posicion del canvas en la pantalla
        Dimension dim = e.getCanvasDimension(); //tamaño del canvas
        
        //calcula donde está el centro del canvas
        int centerX = canvas.x + (int) dim.getWidth() / 2;
        int centerY = canvas.y + (int) dim.getHeight() / 2;
        
        //calcula cuanto avanzó el mouse desde el frame anterior y lo transforma a un angulo que suma al angulo actual
        double offset = in.getMouseX() - (int) dim.getWidth() / 2;
        offset *= sensitivity;
        addAngle(offset);
        
        //System.out.println(Math.toDegrees(angle));
        //regresa el mouse al centro
        mouseController.mouseMove(centerX, centerY);
    }
    
    
    
    /*METODOS DE DIBUJO*/
    
    protected void drawPlayer(Graphics2D g) {
        int pWidth = 10;
        
        //dibujar el personaje de prueba
        g.setColor(Color.red);
        DPoint pPos = getPlayerPos();
        g.fillRect((int) (pPos.x - pWidth/2), (int) (pPos.y - pWidth/2), pWidth, pWidth); //dibuja el personaje centrado
        drawLine(g);
    }
    
    //dibuja la linea que indica hacia donde apunta el jugador
    protected void drawLine(Graphics2D g) {
        int x1 = (int) x;
        int x2 = (int) (x + Math.cos(angle) * 20);
        
        int y1 = (int) y;
        int y2 = (int) (y + Math.sin(angle) * 20);
        
        //dibujar la linea
        Stroke old = g.getStroke();
        g.setStroke(new BasicStroke(2));
        g.drawLine(x1, y1, x2, y2);
        g.setStroke(old);
    }
}
