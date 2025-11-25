package RayEngine;

import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

public final class Banner {
    
    private BufferedImage banner;
    private int zBuffer;
    
    /*maneja las coordenadas y dimensiones no con pixeles, si no con proporciones de la
    pantalla. Por ejemplo un banner dibujado en (0.5, 0.5) siempre se dibujará en la
    mitad de la pantalla, sin importar que tamaño tenga en el momento que se llamó*/
    private double x, y;
    private double w, h;
    
    public Banner(String path, double x, double y, double w, double h) {
        //usa el mismo metodo para leer imagenes de Sprite, pero sin cambiar la escala
        banner = Sprite.getImage(path);
        
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
    
    public void setZBuffer(int zBuffer) {
        this.zBuffer = zBuffer;
    }
    
    public int getZBuffer() {
        return zBuffer;
    }
    
    //COORDENADAS
    
    public double getX() {
        return x;
    }
    public  double getY() {
        return y;
    }
    
    public void setX(double x) {
        this.x = x;
    }
    public void setY(double y) {
        this.y = y;
    }
    
    
    //DIMENSIONES
    
    public double getW() {
        return w;
    }
    public  double getH() {
        return h;
    }
    
    public void setW(double w) {
        this.w = w;
    }
    public void setH(double h) {
        this.h = h;
    }
    
    
    //BANNER
    
    public void setBanner(String path) {
        banner = Sprite.getImage(path);
    }
    
    
    public void draw(Graphics2D g, int winW, int winH) {
        //coordenadas reales en la simulación
        int sx = (int) (x * winW);
        int sy = (int) (y * winH);
        
        //dimensiones reales en la simulación
        int sw = (int) (w * winW);
        int sh = (int) (h * winH);
        
        g.drawImage(banner, sx, sy, sw, sh, null);
    }
}
