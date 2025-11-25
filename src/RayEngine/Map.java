package RayEngine;

import java.awt.Color;
import java.awt.Graphics2D;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;


public class Map {
    
    public final int[][] map;
    
    public final int m;
    public final int n;
    
    private final HashMap<Integer, Sprite> behaviors;
    
    public Map(String path) {
        map = openMapFile(path);
        m = map.length;
        n = map[0].length;
        
        behaviors = new HashMap<>();
        this.addTileBehavior(1, new Sprite(Color.white));
        
        printMap();
    }
    
    public Map(int[][] grid) {
        int gridN = grid[0].length;
        
        for (int[] i: grid) {
            if (i.length != gridN) {
                System.out.println("Mapa invalido, lineas de diferentes longitudes.");
                System.exit(0);
            }
        }
        
        map = grid;
        m = map.length;
        n = map[0].length;
        this.behaviors = new HashMap<>();
        this.addTileBehavior(1, new Sprite(Color.white));
        
        printMap();
    }
    
    private void printMap() {System.out.println("imprimiendo mapa");
        for (int[] i: map) {
            for (int j: i) {
                System.out.printf("%d ", j);
            }
            System.out.println();
        }
    }
    
    protected final int[][] openMapFile(String path) {
        int[][] result = new int[1][1];
        //intenta abrir el archivo del mapa
        try{
            InputStream in = getClass().getResourceAsStream(path);
            
            if (in == null) {
                System.out.println("No se encontró el archivo de mapa: " + path);
                System.exit(0);
            }
            
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            
            result = readMapFile(reader);
            
            //readMapFile regresa matrices de 1x1 cuando hay casos invalidos, por lo que termina el programa si el mapa no es valido.
            if (result.length == 1 && result[0].length == 1) {
                System.out.println("Mapa invalido.");
                System.exit(0);
            }
        }
        catch(Exception e) {
            System.out.println("No se encontró el archivo de mapa.\nError: " + e);
            System.exit(0);
        }
        
        //convertir el archivo a un mapa
        return result;
    }
    
    protected final int[][] readMapFile(BufferedReader reader) {
        //lee la primera linea
        String line;
        try {
            line = reader.readLine();
        } catch (IOException ex) {
            return new int[1][1];
        }
        if (line == null) return new int[1][1];
        
        int n1 = line.split(" ").length;//columnas
        ArrayList<String> lines = new ArrayList<>();
        
        //continua leyendo el resto, si alguna linea tiene un no de elementos que la primera regresa
        while(line != null) {
            if (line.split(" ").length != n1) {
                System.out.println("Mapa invalido, ineas de diferentes longitudes.");
                return new int[1][1];
            }
            
            lines.add(line);
            try {
                line = reader.readLine();
            } catch (IOException ex) {
                return new int[1][1];
            }
        }
        
        
        //ahora con la lista empieza a convertirla a ints
        int[][] result = new int[lines.size()][n1];
        
        for (int i = 0; i < result.length; i++) {
            String[] lineStr = lines.get(i).split("\\s+");
            
            for (int j = 0; j < lineStr.length; j++) {
                try {
                    result[i][j] = Integer.parseInt(lineStr[j]);
                } catch(NumberFormatException E) {
                    return new int[1][1];
                }
            }
        }
        
        return result;
    }
    
    
    //le indica al mapa como comportarse cuando una pared
    public final void addTileBehavior(int tileValue, String sprPath) {
        if (tileValue == 0) return; //no se le puede agregar comportamiento a las casillas en 0
        behaviors.put(tileValue, new Sprite(sprPath));
    }
    
    public final void addTileBehavior(int tileValue, Sprite spr) {
        if (tileValue == 0) return; //no se le puede agregar comportamiento a las casillas en 0
        behaviors.put(tileValue, spr);
    }
    
    //regersa una referencia al sprite del tipo de casilla indicada, si no tiene regresa null
    public final Sprite getBehaviorSprite(int key) {
        if (behaviors.containsKey(key)) {
            return behaviors.get(key);
        }
        return null;
    }
    
    public boolean isBehaviorDefined(int key) {
        return behaviors.containsKey(key);
    }
    
    
    //METODOS DE DIBUJO
    
    public void renderMap(Graphics2D g) {
    int[][] data = map; // matriz del mapa

        for (int i = 0; i < this.m; i++) {
            for (int j = 0; j < this.n; j++) {
                int value = data[i][j];
                
                //blanco para paredes, negro para espacio vacio
                if (this.isWall(i, j)) {
                    g.setColor(Color.white);
                } else {
                    g.setColor(Color.black);
                }

                //dibuja el cuadrado de la casilla un poco más pequeño y centrado para que el color de fondo de la apariencia de un contorno
                g.fillRect(
                    j * Engine.TILE_SIZE + 2, //x
                    i * Engine.TILE_SIZE + 2, //y
                    Engine.TILE_SIZE - 2,
                    Engine.TILE_SIZE -2
                );
                
                //dibuja el mapa sin el contorno
                /*g.fillRect(
                    j * TILE_SIZE, //x
                    i * TILE_SIZE, //y
                    TILE_SIZE,
                    TILE_SIZE
                );*/
            }
        }
    }
    
    public void renderMap2(Graphics2D g, Color floorColor) {
    int[][] data = map; // matriz del mapa

        for (int i = 0; i < this.m; i++) {
            for (int j = 0; j < this.n; j++) {
                int value = data[i][j];
                
                //casillas vacias
                if (!this.isWall(i, j)) {
                    g.setColor(floorColor);
                    g.fillRect(
                        j * Engine.TILE_SIZE + 2, //x
                        i * Engine.TILE_SIZE + 2, //y
                        Engine.TILE_SIZE - 2,
                        Engine.TILE_SIZE -2
                    );
                    
                    continue;
                }

                Sprite spr = behaviors.get(value);
                
                //dibuja textura de la pared
                spr.drawSprite(g,
                        j * Engine.TILE_SIZE + 2,
                        i * Engine.TILE_SIZE + 2,
                        Engine.TILE_SIZE - 2,
                        Engine.TILE_SIZE -2
                );
            }
        }
    }
    
    //regesa un objeto position indicando la casilla del mapa dentro de la que las coordenadas recibidas se encuentran
    public static Position getTile(double x, double y) {
        int tileX = (int) Math.floor(x / Engine.TILE_SIZE);
        int tileY = (int) Math.floor(y / Engine.TILE_SIZE);
        //return new Position((int)(y / Engine.TILE_SIZE), (int)(x / Engine.TILE_SIZE));
        return new Position(tileY, tileX);
    }
    
    //checa si las posiciones recibidas quedan dentro de una pared
    public boolean insideOfWall(double x, double y) {
        Position p = getTile(x, y);
        if (p.m >= 0 && p.n >= 0 && p.m < m && p.n < n) {
            return isBehaviorDefined(map[p.m][p.n]);
            //return map[p.m][p.n] == 1;
        }
        return false;
    }
    
    public boolean isWall(int m, int n) {
        return behaviors.containsKey(map[m][n]);
    }
    
    public int getWallValue(double x, double y) {
        Position p = getTile(x, y);
        return map[p.m][p.n];
    }
}