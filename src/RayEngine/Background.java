package RayEngine;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import javax.imageio.ImageIO;

public class Background {
    
    private final BufferedImage bg;
    
    //colores del fondo
    public final Color ceiling;
    public final Color floor;
    
    public Background(Color ceiling, Color floor) {
        bg = getSolidColorImg(ceiling, floor);
        this.ceiling = ceiling;
        this.floor = floor;
    }
    
    public Background(String path) {
        bg = getBgImg(path);
  
        /*si se usa una imagen y no un color solido se obtienen colores de
        esa imagen para guardar como el techo y el suelo, para la vista 2D*/
        
        //calcula el pixel central del techo y del suelo para sacar los colores de ahi
        int ceilingX = bg.getWidth() / 2;
        int ceilingY = bg.getHeight() / 4;
        int floorX = ceilingX;
        int floorY = ceilingY * 3;
        
        ceiling = new Color(bg.getRGB(ceilingX, ceilingY));
        floor = new Color(bg.getRGB(floorX, floorY));
    }
    
    private BufferedImage getBgImg(String path) {
        BufferedImage im;
        
        try {
            
            InputStream is = getClass().getResourceAsStream(path);
            if (is == null) {
                //textura vacia
                return Sprite.getColorImg(Color.black);
            }
            im = ImageIO.read(is);
            
        } catch (IOException e) {
            
            im = Sprite.getColorImg(Color.black);
            //System.out.println("No se pudo leer la imagen: " + e);
            
        }
        
        return im;
    }
    
    
    private BufferedImage getSolidColorImg(Color ceiling, Color floor) {
        //mantiene el ancho y alto como pares
        int w = Engine.getWinWidth() % 2 == 0 ? Engine.getWinWidth() : Engine.getWinWidth()+ 1;
        int h = Engine.getWinHeight() % 2 == 0 ? Engine.getWinHeight() : Engine.getWinHeight() + 1;
        
        BufferedImage im = new BufferedImage(w, h, BufferedImage.TRANSLUCENT);
        
        Graphics2D g = im.createGraphics();
        
        g.setColor(ceiling);
        g.fillRect(0, 0, w, h / 2);
        g.setColor(floor);
        g.fillRect(0, h / 2, w, h / 2);
        g.dispose();
        
        return im;
    }
    
    public void draw(Graphics2D g, int x, int y, int w, int h) {
        g.drawImage(bg, x, y, w, h, null);
    }
}
