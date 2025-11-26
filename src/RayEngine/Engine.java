package RayEngine;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.image.*;
import java.util.ArrayList;
import javax.swing.*;

public final class Engine extends JFrame{
    
    public static final int TILE_SIZE = 64; //cada celda medirá 64x64 unidades, y sus sprites tendrán esa cantidad de pixeles
    private static int WIN_WIDTH = 800;
    private static int WIN_HEIGHT = 600;
    
    //COMPONENTES
    public final Input in;
    private final Player p;
    private final Map map;
    private final Canvas c;
    
    //raycaster
    private final ArrayList<Entity> entities;
    private final ArrayList<Banner> banners;
    private final RayCaster raycaster;
    
    //elminiar entidades
    private final ArrayList<Entity> nextToBeRemoved;
    private final ArrayList<Entity> nextToBeAdded;
    
    //para ver loq ue sucede en la vista 2d
    private final Canvas view2d;
    private final JDialog debugScreen;
    
    //para activar o desactivar pantalla completa
    private final GraphicsDevice device;
    
    //para mostrar o no mostrar el cursor
    private final Cursor defaultCursor;
    private final Cursor hiddenCursor;
    
    
    //ATRIBUTOS
    private boolean running;
    
    private double deltaTime;
    private int targetFPS;
    private boolean paused;
    private boolean debugActive;
    
    //las dimensiones reales del frame, se mantienen incluso cuando el frame pasa
    //a pantalla completa (para regresar al tamaño correcto con toggleFullscreen)
    private int currentWidth;
    private int currentHeight;
    
    private boolean fullscreen;
    
    //para medir los fps
    private int frameAvg;
    private int frameCounter;
    private double dtSum;
    private double FPS;
    private boolean showFPSinGame;
    
    //referencia al fondo que usa el raycaster
    private Background bg;
    
    
    private static void setWinWidthAndHeight(Dimension d) {
        WIN_WIDTH = d.width;
        WIN_HEIGHT = d.height;
    }
    
    //estos metodos solo permiten conocer la variable pero no modificarla
    public static int getWinWidth() {
        return WIN_WIDTH;
    }
    
    public static int getWinHeight() {
        return WIN_HEIGHT;
    }
    
    
    public Engine(Player player, Map map) {
        
        //canvas de renderizado
        this.c = new Canvas();
        c.setBackground(new Color(0,0,0,0)); //para que soporte transparencia
        c.setPreferredSize(new Dimension(WIN_WIDTH, WIN_HEIGHT));
        c.setFocusable(true);
        
        //listeners para la leida de input
        this.in = new Input();
        in.addEngine(this);
        c.addKeyListener(in);
        c.addMouseListener(in);
        c.addMouseMotionListener(in);
        
        //canvas para la vista 2d
        this.view2d = new Canvas();
        view2d.setPreferredSize(new Dimension(map.n * TILE_SIZE, map.m * TILE_SIZE));
        debugScreen = new JDialog(this);
        debugScreen.add(view2d);
        debugScreen.pack();
        debugScreen.setResizable(false);
        
        //player
        this.p = player;
        p.addInput(in);
        p.addMap(map);
        p.addEngine(this);
        
        //mapa
        this.map = map;
        
        //lista de entidades
        entities = new ArrayList<>();
        nextToBeRemoved = new ArrayList<>();
        nextToBeAdded = new ArrayList<>();
        
        //lista de banners
        banners = new ArrayList<>();
        
        //raycaster
        bg = new Background(Color.black, Color.black);
        this.raycaster = new RayCaster(p, map, entities, banners, bg);
        
        //parámetros
        deltaTime = 0;
        targetFPS = -1;
        running = false;
        paused = false;
        debugActive = false;
        currentWidth = WIN_WIDTH;
        currentHeight = WIN_HEIGHT;
        fullscreen = false;
        
        //calculo de los fps
        frameAvg = 30;
        frameCounter = 0;
        dtSum = 0;
        FPS = 0;
        showFPSinGame = false;
        
        //para ocultar el cursor 
        device = GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()[0];
        defaultCursor = Cursor.getDefaultCursor();
        BufferedImage cursorImg = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        hiddenCursor = Toolkit.getDefaultToolkit().createCustomCursor(cursorImg, new Point(0,0), "hidden");
        
        createFrame();
    }
    
    private void createFrame() {
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        this.setFocusable(true);
        this.setResizable(false);
        this.setLocationRelativeTo(null);
        this.add(c);
        this.pack();
    }
    
    //pausar o resumir el juego
    public void togglePause() {
        paused = !paused;
        //System.out.println("Pausado: " + paused);
        
        //muestra el cursor oculto o normal segun a que estado cambió
        this.setCursor(paused ? defaultCursor : hiddenCursor);
    }
    
    public double getDeltaTime() {
        return deltaTime;
    }
    
    
    //RAYCASTER
    
    public void setFOV(int FOV) {
        raycaster.setFOV(FOV);
    }
    
    public void setRaysToCast(int rays) {
        raycaster.setRaysToCast(rays);
    }
    
    
    //BANNERS
    
    public void addBanner(Banner b) {
        //no agrega el banner si ya tenia otra instancia
        if (banners.contains(b)) return;
        
        banners.add(b);
        updateBanners(); //actualiza los zBuffer
    }
    
    public void addBanner(Banner b, int zBuffer) {
        //no agrega el banner si ya tenia otra instancia
        if (banners.contains(b)) return;
        
        //si sale de lo permitido por el arraylist lo agrega al final
        if (zBuffer > banners.size() || zBuffer < 0) {
            banners.add(b);
        } else { //si no lo agrega en la posición que especifica
            banners.add(zBuffer, b);
        }
        
        updateBanners(); //actualiza los zBuffers
    }
    
    //mueve el elemento en el zBuffer recibido al zBuffer especificado
    public void moveBanner(int zBuffer, int targetZBuffer) {
        //sale si tiene un zBuffer invalido
        if (zBuffer >= banners.size() || zBuffer < 0 ||
                targetZBuffer >= banners.size() || targetZBuffer < 0) {
            return;
        }
        
        Banner aux = banners.remove(zBuffer);
        banners.add(targetZBuffer, aux); //vuelve a añadirlo en el zBuffer esperado
        updateBanners(); //actualiza los zBuffers
    }
    
    //actualiza el zBuffer de cada banner
    public void updateBanners() {
        for (int i = 0; i < banners.size(); i++) {
            banners.get(i).setZBuffer(i);
        }
    }
    
    public int bannersSize() {
        return banners.size();
    }
    
    
    //CANVAS
    
    public Point getCanvasPos() {
        return c.getLocationOnScreen();
    }
    
    public Dimension getCanvasDimension() {
        return new Dimension(c.getWidth(), c.getHeight());
    }
    
    
    public void setTargetFPS(int fps) {
        targetFPS = fps;
    }
    
    public void setFrameAvg(int frameAvg) {
        this.frameAvg = frameAvg;
    }
    
    public void setBackground(Background bg) {
        this.bg = bg; //guarda siempre una referencia al fondo
        raycaster.setBackground(bg);
    }
    
    public void toggleShowFPS() {
        showFPSinGame = !showFPSinGame;
    }

    //modifica las dimensiones del frame principal, además sale de pantalla completa en caso de que esté activada
    public void setWindowSize(Dimension d) {
        device.setFullScreenWindow(null); //sale de pantalla completa
        currentWidth = d.width;
        currentHeight = d.height;
        Engine.setWinWidthAndHeight(d);
        c.setPreferredSize(d);
        c.setSize(d);
        this.pack();
        this.setLocationRelativeTo(null);
        c.createBufferStrategy(2); //Recrea el buffer strategy con el nuevo tamaño
        c.requestFocus();
    }
    
    //SIEMPRE RECREAR EL BUFFER STRATEGY DESPUES DE USAR
    public void undecorated(boolean u) {
        this.dispose();
        this.setUndecorated(u);
        this.setVisible(true);
    }

    //activa la pantalla completa
    public void setFullscreen() {
        fullscreen = true;
        setDebugScreenActive(false);
        
        //activa undecorated
        undecorated(true);
        
        device.setFullScreenWindow(this);
        
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        Engine.setWinWidthAndHeight(screenSize);
        c.setPreferredSize(screenSize);
        c.createBufferStrategy(2); //recrea el buffer strategy con el nuevo tamaño
    }
    
    //activa o desactiva pantalla completa
    public void toggleFullscreen() {
        fullscreen = !fullscreen;
        if (fullscreen) {
            setFullscreen();
        } else {
            //desactiva undecorated
            undecorated(false);
            setWindowSize(new Dimension(currentWidth, currentHeight));
        }
        c.requestFocus();
    }
    
    public void setDebugScreenActive(boolean active) {
        if (active && fullscreen) return; //no lo activa si está en pantalla completa
        debugActive = active;
        debugScreen.setVisible(active);
        if (active) this.toFront();
        c.requestFocus(); //vuelve a focusear el frame porque el focus se va a la vista 2d
    }
    
    public void toggleDebugScreen() {
        setDebugScreenActive(!debugActive);
        c.requestFocus(); //vuelve a focusear el frame porque el focus se va a la vista 2d
    }
    
    public void setAspectRatio(Dimension d) {
        raycaster.setAspectRatio(d.width, d.height);
    }
    
    private BufferStrategy createBuffStrat(Canvas cv) {
        cv.createBufferStrategy(2);
        return cv.getBufferStrategy();    
    }
    
    
    
    public void start() {
        running = true;
        this.setVisible(true);
        if (debugActive) debugScreen.setVisible(true);
        this.setCursor(hiddenCursor);
        c.requestFocus();
        
        /*para el renderizado utiliza un canvas en vez de hacer override de algun componente de jswing, pues usando canvas controlas exactamente cuando
        quieres que dibuje y limpie (al inicio de cada frame). Usa un bufferStrategy de 2, es decir 2 buffers. Mientras un frame se muestra, el otro frame se renderiza*/
        createBuffStrat(c);
        createBuffStrat(view2d);
        
        BufferStrategy bs;
        BufferStrategy bs2d;
        
        while(running) {
            long start = System.nanoTime();
            
            in.update();
            
            //pausa o resume el juego
            if (in.isKeyReleased(KeyEvent.VK_ESCAPE)) {
                togglePause();
                if (paused) System.out.println("JUEGO PAUSADO");
                else System.out.println("JUEGO RESUMIDO");
            }
            
            //desactiva o activa la pantalla de debug
            if (in.isKeyReleased(KeyEvent.VK_F3)) toggleDebugScreen();
        
            //activa o desactiva la pantalla completa
            if (in.isKeyReleased(KeyEvent.VK_F11)) toggleFullscreen();
            
            //FPS dentro de la vista 3D
            if (in.isKeyReleased(KeyEvent.VK_F4)) toggleShowFPS();
            
            /*llama todo esto fuera de update para que incluso con el juego pausado se pueda resumir,
            activar y desactivar la pantalla de debug o entrar y salir de pantalla completa*/
            
            updateFPS(deltaTime); //actualizar los fps
            
            
            
            //update
            if (!paused) update(deltaTime);
            
            //Render
            bs = c.getBufferStrategy();
            render(bs);
            if (debugActive) {
                bs2d = view2d.getBufferStrategy();
                render2D(bs2d);
            }
            
            
            
            long finish = System.nanoTime();
            
            //recalcular el dt
            deltaTime = (finish - start) / 1_000_000_000.0;
            deltaTime += delay(); //detiene el hilo para mantener los fps esperadoa (targetFPS) y suma el tiempo que se detuvo al dt
        }
    }
    
    
    public void stop() {
        running = false;
    }
    
    
    /*regresa el delay necesario para llegar a los targetFPS. Por ejemplo, para 60 fps cada frame dura 0.016s, pero
    si el frame dura menos que eso se calcula el tiempo que se necesita dormir el hilo para que dure lo correcto*/
    private double delay() {
        if (targetFPS <= 0) return 0;
        
        long start = System.nanoTime();
        
        double dtTarget = 1.0 / targetFPS;
        double timeLeft = dtTarget - deltaTime;
        
        if (timeLeft <= 0) return 0; //si es negativo ya paso más tiempo del esperado para llegar a los targetFPS
        
        try {
            Thread.sleep((long)(timeLeft * 1000)); 
        } catch (InterruptedException e) {
            //no hace nada pero sleep pide un try catch por si es interrumpido
        }
        
        long finish = System.nanoTime();
        return (finish - start) / 1_000_000_000.0;
    }
    
    
    private void updateFPS(double dt) {
        dtSum += dt;
        frameCounter++;
        
        if (frameCounter >= frameAvg) {
            double dtAvg = dtSum / (frameAvg * 1.0);
            FPS = 1.0 / dtAvg;
            dtSum = 0;
            frameCounter = 0;
        }
    }
    
    
    //manejo de entidades
    
    public void addEntity(Entity en) {
        en.addRef(this, p, map);
        nextToBeAdded.add(en);
    }
    
    public void removeEntity(Entity en) {
        nextToBeRemoved.add(en);
    }
    
    public ArrayList<Entity> getEntitiesInRadius(double radius) {
        ArrayList<Entity> inside = new ArrayList<>();
        
        //empieza del ultimo porque la lista está ordenada de mayor a menor
        for (int i = entities.size() - 1; i >= 0; i--) {
            Entity en = entities.get(i);
            if (en.getDistance() <= radius) inside.add(en);
            else break;
        }
        
        return inside;
    }
    
    private void updateEntitiesList() {
        for (Entity i: nextToBeRemoved) {
            try {
                entities.remove(i);
            } catch(Exception e) {}
        }
        nextToBeRemoved.clear();
        
        for (Entity i: nextToBeAdded) {
            try {
                entities.add(i);
            } catch(Exception e) {}
        }
        nextToBeAdded.clear();
    }
    
    public boolean containsEntity(Entity en) {
        return entities.contains(en);
    }
    
    
    
    //UPDATE Y RENDER
    
    private void update(double dt) {
        
        //evita que las teclas y botones que estaban activos cuando se perdio el foco se queden activadas
        if (!this.isFocused())in.allFalse();
        
        //pausa el juego cuando se pierde el focus
        if (!this.isFocused()) {
            paused = false;
            togglePause(); //para llamar a togglePause y modificar el mouse
        }
        
        //actualizar los componentes
        p.update(dt);
        updateEntities(dt);
        raycaster.update(dt);
        
        //limpia cualquier entidad que haya tenido una llamada de removeEntity
        updateEntitiesList();
    }
    
    private void updateEntities(double dt) {
        for (Entity i: entities) {
            i.update(dt);
            i.updateDistance();
        }
        
        /*una vez se actualizo la distancia de la entidad, la lista se ordena de mayor a menor distancia
        Esto permite que se dibujen primero las entidades más lejanas, y las más cercanas se dibujen por
        arriba y dar un efecto de profundidad*/
        entities.sort((a, b) -> Double.compare(b.getDistance(), a.getDistance()));
    }
    
    
    private void render(BufferStrategy bs) {
        Graphics2D g = (Graphics2D) bs.getDrawGraphics(); //obtiene el objeto para dibujar al canvas
        
        /*configuración del canvas*/
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        
        //renderizado
            raycaster.renderSimulation3D(g);
            if (showFPSinGame) drawTextBox(g, String.format("FPS: %.2f", FPS), 0, 0);
        
            
        //muestra el frame dibujar
        g.dispose();
        bs.show();
    }
    
    //renderiza la vista en 2d
    private void render2D(BufferStrategy bs) {
        Graphics2D g = (Graphics2D) bs.getDrawGraphics();
        g.clearRect(0, 0, view2d.getWidth(), view2d.getHeight());
        
        //RENDERIZADO
            g.setColor(Color.black); //para las lineas
            g.fillRect(0, 0, view2d.getWidth(), view2d.getHeight());
            
            //dibuja las casillas vacias con el color del suelo del fondo
            map.renderMap2(g, bg.floor);
            raycaster.renderView2D(g); //rayos del raycaster
            p.drawPlayer(g); //jugador
            
            //dibuja el deltatime, fps y si el juego esta pausado
            drawTextBox(g, String.format("dt: %.6f", deltaTime), 10, 10);
            drawTextBox(g, String.format("FPS: %.2f", FPS), 100, 10);
            if (paused) drawTextBox(g, "PAUSADO", view2d.getWidth() / 2, 10);
        

        g.dispose();
        bs.show();
    }
    
    
    //dibuja un texto con un fondo negro
    private void drawTextBox(Graphics2D g, String str, int x, int y) {
        FontMetrics fm = g.getFontMetrics();
        
        int fWidth = fm.stringWidth(str);
        int fHeight = fm.getHeight();
        
        int offset = fHeight / 2;
        int textX = x + offset;
        int textY = y + offset + fm.getAscent();
        
        g.setColor(Color.black);
        g.fillRect(x, y, fWidth + fHeight, fHeight * 2);
        g.setColor(Color.white);
        g.drawString(str, textX, textY);
    }
    
    
    
    //UTILIDADES
    
    //normaliza un angulo, manteniendolo dentro del rango de 0 a 2pi
    public static double normalizeAngleRad(double a) {
        a %= 2 * Math.PI;
        if (a < 0) a += 2 * Math.PI;
        return a;
    }
    
    public static double normalizeAngleDeg(double a) {
        a %= 360;
        if (a < 0) a += 360;
        return a;
    }
    
    //aplica la formula de la distancia entre dos puntos
    public static double distance(DPoint p1, DPoint p2) {
        double x1 = p1.x;
        double x2 = p2.x;
        double y1 = p1.y;
        double y2 = p2.y;
        
        double dx = x2-x1;
        double dy = y2-y1;
        
        return Math.sqrt(dx*dx + dy*dy);
    }
    
    public static double distance(double x1, double y1, double x2, double y2) {
        double dx = x2-x1;
        double dy = y2-y1;
        return Math.sqrt(dx*dx + dy*dy);
    }
 
}
