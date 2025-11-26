package RayEngine;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;

public final class RayCaster {
    
    private final Map map;
    private final Player p;
    private final ArrayList<Entity> entities;
    private final ArrayList <Banner> banners;
    
    private int simWidth;
    private int simHeight;
    
    private DPoint playerPos;
    private double angle; //angulo del jugador en radianes
    
    private int FOV; //field of view, que tantos grados puede ver el jugador
    
    private int raysToCast;
    private Ray[] rays;
    private double ratio; //división del aspect ratio
    
    private Background bg;
    
    
    //aspect ratio o relacion de aspecto, que indica la proporción que el renderizado mantendrá
    private Dimension aspectRatio;
    
    public RayCaster(Player p, Map map, ArrayList<Entity> entities, ArrayList<Banner> banners, Background bg) {
        this.p = p;
        this.map = map;
        this.entities = entities;
        this.banners = banners;
        this.bg = bg;
        this.FOV = 60;
        
        //establece la relación de aspecto
        setAspectRatio(4, 3);
        
        /*establece la cantidad de rayos, además del ancho y alto de la
        simulación y la imagen donde se dibujará cada frame antes del escalado*/
        setRaysToCast(200);
        
        updatePlayerInfo();
        rays = new Ray[raysToCast];
    }
    
    
    //EN GRADOS
    public void setFOV(int FOV) {
        //mantiene el fov como numero par, para mantener todo centrado al jugador siempre
        if (FOV % 2 == 1) FOV++;
        
        /*caso especial, si el angulo es 360 lo mantiene sin normalizar. No afecta en nada realmente porque normalmente no se va a usar
        un fov de 360, pero un angulo de 360 es igual a 0, por lo que si intentas castear rayos que cobran todos los angulos posibles 
        estableciendo el fov a 360, en realidad quedarias con un aumento de 0, y todos los rayos tendrian exactamente el mismo angulo*/
        if (FOV == 360) {
            this.FOV = FOV;
            return;
        }
        
        FOV = (int) Engine.normalizeAngleDeg(FOV);
        this.FOV = FOV;
    }
    
    public void setRaysToCast(int raysToCast) {
        //if (raysToCast % 2 == 1) raysToCast++; //igual con los rayos
        this.raysToCast = raysToCast;
        rays = new Ray[raysToCast];
        
        /*cada rayo va a dibujar una sola columna de pixeles de la vista en 3D, por lo que el ancho en pixeles será el mismo
        que el número de rayos trazados. se calcula el alto de la simulación con una regla de 3, considerando el aspect ratio*/
        simWidth = raysToCast;
        simHeight = (simWidth * aspectRatio.height) / aspectRatio.width;
    }
    
    public final void setAspectRatio(int w, int h) {
        aspectRatio = new Dimension(w, h);
        ratio = aspectRatio.width / (1.0 * aspectRatio.height);
    }
    
    public void setBackground(Background bg) {
        this.bg = bg;
    }
    
    
    public void update(double dt) {
        updatePlayerInfo();
        castRays();
    }
    
    private void updatePlayerInfo() {
        playerPos = p.getPlayerPos();
        angle = p.getRadAngle();
    }
    
    
    private void castRays() {
        //incremento del angulo cada que se castea un nuevo rayo
        double angleIncrement = Math.toRadians(FOV / (raysToCast * 1.0));
        double rayAngle = angle - angleIncrement * (raysToCast / 2);
        
        for (int i = 0; i < raysToCast; i++) {
            rays[i] = new Ray(Engine.normalizeAngleRad(rayAngle), playerPos, map);
            rayAngle += angleIncrement;
        }
    }

    
    //METODOS PARA EL RENDERIZADO 3D
    
    public void renderSimulation3D_2(Graphics2D g) {    
        //escalas para acomodar al tamaño de la ventana
        double widthScale = 1.0 * Engine.getWinWidth() / simWidth;
        double heightScale = 1.0 * Engine.getWinHeight() / simHeight;
        AffineTransform old = g.getTransform();
        g.scale(widthScale, heightScale);

        renderWalls(g, simWidth, simHeight);
        renderEntities(g, simWidth, simHeight);
        
        g.setTransform(old); //regresa a la escala original
    }
    
    public void renderSimulation3D(Graphics2D g) {    
        //escalas para acomodar al tamaño de la ventana
        double widthScale = 1.0 * Engine.getWinWidth() / simWidth;
        double heightScale = 1.0 * Engine.getWinHeight() / simHeight;
        double scale = widthScale < heightScale ? widthScale : heightScale;
        
        int winW = Engine.getWinWidth();
        int winH = Engine.getWinHeight();

        int drawW = (int) (simWidth * scale);
        int drawH = (int) (simHeight * scale);

        //offset para mantener el dibujo centrado
        int xOffset = (winW - drawW) / 2;
        int yOffset = (winH - drawH) / 2;
        
        //escala y centra las coordenadas
        AffineTransform old = g.getTransform();
        g.translate(xOffset, yOffset);
        g.scale(scale, scale);
        
        //dibujar paredes, sprites y banners
        renderWalls(g, simWidth, simHeight);
        renderEntities(g, simWidth, simHeight);
        renderBanners(g, simWidth, simHeight);
        
        g.setTransform(old); //regresa a la escala original
        
        
        /*dibuja las lineas negras a los lados o arriba y abajo para cubrir las
        partes que quedan fuera de la simulacion y del centrado*/
        drawBlackStripes(g, xOffset, yOffset, scale);
    }
    
    private void drawBlackStripes(Graphics2D g, int xo, int yo, double scale) {
        g.setColor(Color.black);
        
        //a los lados
        g.fillRect(0, 0, xo, yo);
        g.fillRect((int) (xo + scale * simWidth), (int) (yo + scale * simHeight), xo, yo);
        
        //arriba y abajo
        g.fillRect(0, 0, Engine.getWinWidth(), yo);
        g.fillRect(0, (int) (yo + scale * simHeight), Engine.getWinWidth(), yo);
    }
    
    //renderiza las paredes
    private void renderWalls(Graphics2D g, int simWidth, int simHeight) {
        bg.draw(g, 0, 0, simWidth, simHeight);
        
        for (int i = 0; i < raysToCast; i++) {
            
            if (rays[i].hit == null) continue;
            double rayLength = rays[i].length;
            
            /*Se da un efecto de ojo de pez porque las columnas se hacen más pequeñas entra más largo sea el rayo y
            los rayos más cercanos a las orillas son más largos. por eso se obtiene la diferencia de angulos del jugador
            y del rayo, multiplicando la longitud por el coseno de este angulo se descompone en su componente horizontal
            lo que arregla el efecto*/
            double da = angle - rays[i].angle;
            da = Engine.normalizeAngleRad(da);
            rayLength *= Math.cos(da);
            
            //obtiene el alto de la columna que dibujará el rayo actual
            int rayHeight = getRayHeight(rayLength, simHeight);
            
            //calcula la posición en y que se debe de dibuja la columna para mantenerla centrada
            int offset = (simHeight - rayHeight) / 2;
            
            //g.setColor(Color.MAGENTA);
            
            //que columna de pixeles del sprite se va a dibujar
            int column;
            if (rays[i].isVertical) column = (int) (rays[i].hit.y % Engine.TILE_SIZE);
            else column = (int) (rays[i].hit.x % Engine.TILE_SIZE);
            
            
            /*para evitar que las texturas se dibujen invertidas checa si el rayo está mirando a la izquierda en intersecciones
            verticales o hacia abajo en intersecciones horizontales (cosa que indica inverted) y si el rayo si está invertido lo
            corrige tomando las texturas de derecha a izquierda y no de izquierda a derecha como lo haria column normalmente*/
            if (rays[i].inverted) column = Engine.TILE_SIZE - column - 1;
            
            //obtiene el sprite de la pared que el rayo golpeó para dibujarlo
            Sprite wallSpr = map.getBehaviorSprite(rays[i].tileValue);
            
            //si la intersección es vertical dibuja la pared normal, si es horizontal la dibuja sombreada
            if (rays[i].isVertical) {
                wallSpr.drawColumn(g, column, i, offset, 1, rayHeight);
            } else {
                wallSpr.drawShadedColumn(g, column, i, offset, 1, rayHeight);
            }
        }
    }
    
    private int getRayHeight(double rayLength, int simHeight) {
        //calcula el alto de cada columna columna de un rayo, obteniendo la inversa de su longitud y multiplicandola por el alto de la simulacion
        double rayHeightDouble = (Engine.TILE_SIZE / rayLength * simHeight);
        
        /*la verdad es que no se muy bien por qué, pero las paredes se dibujan deformadas según el aspect ratio de
        la pantalla. Con un aspect ratio de 1:1 las paredes se ven correctamente, pero con uno de 4:3 o 16:9 las
        paredes tendrán un ancho y alto que corresponde a ese aspect ratio. Al final logré corregirlo obteniendo ratio,
        que es la división entre el ancho y alto del aspect ratio, y luego multiplicando el alto de la columna que se
        va a dibujar con ratio. Funciona, aunque no se si sea la forma correcta de hacerlo*/
        rayHeightDouble *= ratio; 
        
        int rayHeight = (int) Math.round(rayHeightDouble);
        if (rayHeight % 2 == 1) rayHeight++; //mantiene los numeros pares
        return rayHeight;
    }
    
    
    private void renderEntities(Graphics2D g, int simWidth, int simHeight) {
        double fovRad = Math.toRadians(FOV);
        double halfFovTan = Math.tan(fovRad / 2.0);

        for (Entity i: entities) {
            if (!i.visible) continue;
            /*dx y dy son las posiciones de la entidad en un plano cartesiano donde el jugador es el origen*/
            double dx = i.getX() - p.getX();
            double dy = -(i.getY() - p.getY());
            double pa = -p.getRadAngle();
            
            /*para conocer la posición en pantalla que tendrá la entidad, el eje x se tiene que alinear al
            angulo del jugador. De esta forma una entidad con x = 0 estará directamente enfrente del jugador,
            x negativo estará a la izquierda y x positivo a la derecha. Para lograr esto se calcula que angulo
            sumar a pa para que quede con un angulo de 90, y de esta forma siempre esté alineado como se necesita*/
            double rotation = (Math.PI / 2) - pa;
            rotation = Engine.normalizeAngleRad(rotation);
            DPoint transform = rotateView(dx, dy, rotation);
            
            //si está detras del jugador no lo dibuja
            if (transform.y < 0) continue;  
            
            //calcula la posicion en x (screenX) en la simulación 3d

            /*t indicará donde deberá estar el personaje en un rango de -1 a 1, siendo -1 el inicio de la pantalla y 1 el final.*/
            double t = transform.x / transform.y;
            
            /*t no es más que la tangente de angulo que hay entre el eje y (que ya está alineado al jugador) y el vector de la
            entidad. Pero esta tangente puede tomar cualquier valor dependiendo de que tan lejos esté del eje y, entonces se
            normaliza usando halfOfTan, la tangente del angulo desde el eje y hasta el inicio o final del campo de vista. De
            esta forma las entidades siempre se mantendrán dentro del rango del fov*/
            t = t / halfFovTan;
            
            /*finalmente se modifica el rango para que vaya de 0 a 1 y se multiplica
            por el tamaño de la vista 3d para obtener la posición final de x*/
            int screenX = (int)((t + 1.0) * 0.5 * simWidth);
            
            int size = this.getRayHeight(transform.y, simHeight); //ancho y alto de un sprite
            int offset = (simHeight - size) / 2;
            
            int start = screenX - size / 2;
            for (int j = start; j < start + size; j++) {
                //no dibuja la columna si está detras de una pared
                if (j < 0 || j >= rays.length) continue;
                if (i.getDistance() > rays[j].length) continue;
                
                int pos = j - start;
                int column = pos * Engine.TILE_SIZE / size;
                i.getSprite().drawColumn(g, column, j, offset, 1, size);
            } 
        }
    }
    
    /*rota un vector al angulo recibido. Por ejemplo si recibe un vector con un
    angulo de 60 y recibe a = 30, regresa un vector con un angulo de 90*/
    private DPoint rotateView(double x, double y, double a) {
        double cos = Math.cos(a), sin = Math.sin(a);
        
        double rx = x * cos - y * sin;
        double ry = x * sin + y * cos;
        
        return new DPoint(rx, ry);
    }
    
    ///BANNERS
    private void renderBanners(Graphics2D g, int simWidth, int simHeight) {
        for (Banner i: banners) {
            i.draw(g, simWidth, simHeight);
        }
    }
    
    
    //RENDERIZAR  VISTA 2D
    public void renderView2D(Graphics2D g) {
        g.setColor(Color.yellow);
        for (Ray i: rays) {
            i.drawRay(g);
        }
        
        g.setColor(new Color(0, 255, 255));
        for (Entity i: entities) {
            i.drawEntity(g);
        }
    }
    
}




final class Ray {
    
    public final double angle;
    private final double px, py; //posicion del personaje en x y y
    private final int m, n; //orden de la matriz del mapa
    
    
    //true si hay intersección antes de salir del mapa en cualquiera de los dos casos
    private boolean foundHorizontalIntersection = false;
    private boolean foundVerticalIntersection = false;

    private DPoint intersection;
    
    //DATOS DEL RAYO
    
    public final double length;
    public final boolean isVertical;
    public final boolean isHorizontal;
    
    public final DPoint hit;
    
    public final int tileValue;
    
    public final boolean inverted;
    
    
    //recibe el angulo en radianes
    public Ray(double angle, DPoint pos, Map map) {
        this.angle = angle;
        px = pos.x;
        py = pos.y;

        m = map.m;
        n = map.n;
        
        length = cast(map);
        isVertical = foundVerticalIntersection;
        isHorizontal = foundHorizontalIntersection;
        hit = intersection;
        
        /*si el angulo mira hacia abajo o hacia la izquierda las texturas se van a dibujar invertidas
        por lo que es necesario saber si un angulo va a dibujar sus texturas asi para invertirlo*/
        boolean lookingDown = isHorizontal && (angle > 0 && angle < Math.PI);
        boolean lookingLeft = isVertical && (angle > Math.PI / 2 && angle < 3 * Math.PI / 2);
        inverted = lookingDown || lookingLeft;
        
        tileValue = hit != null ? map.getWallValue(hit.x, hit.y) : 0;
    }
    
    private double cast(Map map) {
        DPoint pos = new DPoint(px, py);
        
        //obtiene los puntos de intersección vertical y horizontal
        DPoint horizontalHit = horizontalHit(map);
        DPoint verticalHit = verticalHit(map);
        
        double hLength;
        double vLength;
        
        //si se dio una interseccion horizontal calcula el valor del rayo horizontal, si no le da un valor muy alto
        hLength = foundHorizontalIntersection && horizontalHit != null ? Engine.distance(pos, horizontalHit) : Double.POSITIVE_INFINITY;
        
        //igual con la intersección vertical
        vLength = foundVerticalIntersection && verticalHit != null ? Engine.distance(pos, verticalHit) : Double.POSITIVE_INFINITY;
        
        //primero verifica si si encontró el punto, si no regresa el valor más alto posible
        /*ya que está regresando infinito, al hacer el calculo de la longitud de la columna de ese rayo siempre dará 0,
        pues se estaria dividiendo entre infinito. Asi nunca se dibujaria la pared y no daria ningun conflicto*/
        if (hLength == Double.POSITIVE_INFINITY && vLength == Double.POSITIVE_INFINITY) {
            intersection = null;
            return Double.POSITIVE_INFINITY;
        }
        
        //asigna la longitud más corta de entr los dos, además de guardar la posición
        if (hLength < vLength) {
            intersection = horizontalHit;
            foundVerticalIntersection = false;
            foundHorizontalIntersection = true;
            return hLength;
        } else {
            intersection = verticalHit;
            foundHorizontalIntersection = false;
            foundVerticalIntersection = true;
            return vLength;
        }
    }
    
    
    //ENCUENTRA TODAS LAS INTERSECCIONES HORIZONTALES
    private DPoint horizontalHit(Map map) {
        boolean facingDown = angle > 0 && angle < Math.PI;
        boolean facingUp = angle > Math.PI && angle < 2 * Math.PI;
        
        boolean foundWall = false;
        
        //se calcula la primera intersección pues el personaje no está alineado a las casillas
        double firstX;
        double firstY;
        
        //calcula la primera intersección en y, redondeando al valor del tamaño de la casilla
        /*ya que se buscan las intersecciones horizontales, la posicion en y de cada intersección siempre se encontrará
        alineada a las orillas de las casillas, por lo que para encontrar la primer intersección solo es necesario tomar
        la posición en y del jugador y dependiendo de si está mirando arriba o abajo, llevarla a la siguiente casilla
        hacia arriba o hacia abajo*/
        if (facingUp) {
            firstY = Math.floor(py / Engine.TILE_SIZE) * Engine.TILE_SIZE - 0.0001;
        } else if(facingDown) {
            firstY = Math.floor(py / Engine.TILE_SIZE) * Engine.TILE_SIZE + Engine.TILE_SIZE;
        } else { //cuando está mirando directamente a la izquierda o a la derecha no podrá encontrar jamás una intersección horizontal
            return null;
        }
        
        //ahora aplicar la formula para encontrar la primer intersección en x
        firstX = (firstY - py) / Math.tan(angle) + px;
        
        //ahora calcula nuevos valores para x y y para cada interseccion horizontal, iniciando desde la primer intersección
        double nextX = firstX;
        double nextY = firstY;
        
        //incrementos para encontrar cada nueva intersección
        double incrementX = 0;
        double incrementY = 0;
        
        //los incrementos en y serán el tilesize, para mantenerlo siempre alineado a las casillas del mapa
        if (facingUp) incrementY = - Engine.TILE_SIZE;
        else if (facingDown) incrementY = Engine.TILE_SIZE;
        
        incrementX = incrementY / Math.tan(angle);
        
        //empieza el loop de revisión, solo para de revisar cuando sale del mapa
        while(nextX >= 0 && nextX <= n * Engine.TILE_SIZE
                && nextY >= 0 && nextY <= m * Engine.TILE_SIZE ) {
            
            //si encuentra una pared en un incremento lo marca y deja de buscar
            if (map.insideOfWall(nextX, nextY)) {
                foundWall = true;
                break;
            } else { //si no la encuentra sigue aumentando
                nextX += incrementX;
                nextY += incrementY;
            }
            
        }
        
        //si encontró la pared regresa la posición en donde la encontró
        if (foundWall) {
            foundHorizontalIntersection = true;
            return new DPoint(nextX, nextY);
        }
        
        //si no la encontró regresa null
        return null;
    }
    
    
    
    //ENCUENTRA TODAS LAS INTERSECCIONES VERTICALES
    private DPoint verticalHit(Map map) {     
        boolean facingLeft = angle > Math.PI / 2 && angle < 3 * Math.PI / 2;
        boolean facingRight = angle > 3 * Math.PI / 2 || angle < Math.PI / 2;
        
        boolean foundWall = false;
        
        //se calcula la primera intersección pues el personaje no está alineado a las casillas
        double firstX;
        double firstY;
        
        //calcula la primera intersección en x, redondeando al valor del tamaño de la casilla
        /*de la misma forma que en las intersecciones horizontales, la posicion en x de la intersección se alinea a las
        casillas, por lo que lleva la posicion en x a un valor que se alinee con las casillas*/
        if (facingRight) {
            firstX = Math.floor(px / Engine.TILE_SIZE) * Engine.TILE_SIZE + Engine.TILE_SIZE;
        } else if(facingLeft) {
            firstX = Math.floor(px / Engine.TILE_SIZE) * Engine.TILE_SIZE - 0.0001;
        } else { //cuando está mirando directamente hacia arriba o abajo es imposible encontrar una interseccion vertical
            return null;
        }
        
        //ahora aplicar la formula para encontrar la primer intersección en y
        firstY = py + (firstX - px) * Math.tan(angle);
        
        //ahora calcula nuevos valores para x y y para cada interseccion vertical, iniciando desde la primer intersección
        double nextX = firstX;
        double nextY = firstY;
        
        //incrementos para encontrar cada nueva intersección
        double incrementX = 0;
        double incrementY = 0;
        
        if (facingRight) incrementX = Engine.TILE_SIZE;
        else if (facingLeft) incrementX = - Engine.TILE_SIZE;
        
        incrementY = incrementX * Math.tan(angle);
        
        //empieza el loop de revisión, solo para de revisar cuando sale del mapa
        while(nextX >= 0 && nextX <= n * Engine.TILE_SIZE && nextY >= 0 && nextY <= m * Engine.TILE_SIZE ) {
            
            //si encuentra una pared en un incremento lo marca y deja de buscar
            if (map.insideOfWall(nextX, nextY)) {
                foundWall = true;
                break;
            } else { //si no la encuentra sigue aumentando
                nextX += incrementX;
                nextY += incrementY;
            }
            
        }
        
        //si encontró la pared regresa la posición en donde la encontró
        if (foundWall) {
            foundVerticalIntersection = true;
            return new DPoint(nextX, nextY);
        }
        
        //si no la encontró regresa null
        return null;
    }
    
    
 
    public void drawRay(Graphics2D g) {
        if (intersection != null) {
            g.drawLine((int) px, (int) py, (int) intersection.x, (int) intersection.y);
        }
    }
}