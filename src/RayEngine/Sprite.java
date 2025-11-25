package RayEngine;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.*;
import javax.imageio.*;

public class Sprite {
    
    private final BufferedImage sprite;
    private final BufferedImage shadedSprite;
    
    private final Color shadow;
    
    public Sprite(String path) {
        //crea el sprite con getImage, luego lo reescala al tamaño de las casillas
        sprite = Sprite.reescale(getImage(path), Engine.TILE_SIZE, Engine.TILE_SIZE);
        
        //crea el sprite sombreado
        shadow = new Color(0, 0, 0, 128); //negro a 50% de opacidad
        shadedSprite = getShadedImg(sprite);
    }
    
    //crea un sprite con un color solido
    public Sprite(Color clr) {
        //crea el sprite normal, usando el color solido
        sprite = getColorImg(clr);
        
        //crea el sprite sombreado
        shadow = new Color(0, 0, 0, 128); //negro a 50% de opacidad
        shadedSprite = getShadedImg(sprite);
    }
    
    public static final BufferedImage getImage(String path) {
        BufferedImage im;
        
        try {
            
            InputStream is = Sprite.class.getResourceAsStream(path);
            
            if (is == null) {
                //cuando no se procesa correctamente una textura crea un cuadrado magenta
                return getColorImg(Color.magenta);
            }
            
            im = ImageIO.read(is);
            //System.out.println("Imagen extraida.");
            
        } catch (IOException e) {
            
            im = getColorImg(Color.magenta);
            //System.out.println("No se pudo leer la imagen: " + e);
            
        }
        
        return im;
    }
    
    public static final BufferedImage getColorImg(Color clr) {
        BufferedImage img = new BufferedImage(Engine.TILE_SIZE, Engine.TILE_SIZE, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(clr);
        g.fillRect(0, 0, Engine.TILE_SIZE, Engine.TILE_SIZE);
        g.dispose();
        return img;
    }
    
    private BufferedImage getShadedImg(BufferedImage original) {
        int w = original.getWidth();
        int h = original.getHeight();
        
        //crea la imagen sombreada
        BufferedImage shaded = new BufferedImage(
                w, h,
                BufferedImage.TYPE_INT_ARGB
        );
        
        Graphics2D g = shaded.createGraphics();
        g.drawImage(original, 0, 0, w, h, null); //copia la original
        g.setColor(shadow);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return shaded;
    }
    
    public static BufferedImage reescale(BufferedImage src, int w, int h) {
        BufferedImage scaled = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = scaled.createGraphics();
        //g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_ANTIALIAS_OFF);
        g.drawImage(src, 0, 0, w, h, null);
        g.dispose();
        return scaled;
    }
    
    
    //DIBUJO DEL SPRITE
    
    public final void drawColumn(Graphics2D g, int column, int x, int y, int w, int h) {
        //dibuja una columna verde si se sale del limite de las texturas
        if (column < 0 || column >= Engine.TILE_SIZE) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, w, h);
            return;
        }
        
        /*las primeras dos coordenadas indican las esquinas donde se inicia y
        termina a dibujar en el componente del graphics que se le paso.
        Las ultimas dos coordenadas indican las esquinas el segmento de la
        imagen a dibujar, se extrae unicamente una columna de un pixel de ancho*/
        g.drawImage(
            sprite,
            x, y, //primera esquina de dibujo
            x + w, y + h, //segunda esquina de dibujo
            column, 0, //primera esquina del segmento
            column + 1, Engine.TILE_SIZE, //segunda esquina del segmento
            null
        );
    }
    
    public final void drawShadedColumn(Graphics2D g, int column, int x, int y, int w, int h) {
        //dibuja la misma columna verde pero sombreada
        if (column < 0 || column >= Engine.TILE_SIZE) {
            g.setColor(Color.GREEN);
            g.fillRect(x, y, w, h);
            g.setColor(shadow);
            g.fillRect(x, y, w, h);
            return;
        }
        
        g.drawImage(
            shadedSprite,
            x, y, //primera esquina de dibujo
            x + w, y + h, //segunda esquina de dibujo
            column, 0, //primera esquina del segmento
            column + 1, Engine.TILE_SIZE, //segunda esquina del segmento
            null
        );
    }
    
    //dibuja toda la imagen
    public final void drawSprite(Graphics2D g, int x, int y, int w, int h) {
        g.drawImage(sprite, x, y, w, h, null);
    }
    
    public final void drawShadedSprite(Graphics2D g, int x, int y, int w, int h) {
        g.drawImage(shadedSprite, x, y, w, h, null);
    }
    
}

