package RayEngine;

import java.awt.event.*;

/*IMPORTANTE: importar KeyEvent y MouseEvent para poder acceder a las constantes de las teclas que los metodos necesitan*/

public class Input implements KeyListener, MouseListener, MouseMotionListener {
    
    private Engine e;

    //teclado
    private final boolean[] keys = new boolean[256];
    private final boolean[] keysWasPressed = new boolean [256];
    private final boolean[] keysReleased = new boolean[256];

    //mouse
    private final boolean[] mouseButtons = new boolean[5];
    private final boolean[] mouseButtonsWasPressed = new boolean[5];
    private final boolean[] mouseButtonsReleased = new boolean[5];
    private int mouseX = 0;
    private int mouseY = 0;
    
    
    //marca todo como false, cuando la ventana pierde el focus las teclas presionadas quedan bloqueadas y se pierde control del persoanje, por eso es necesario reiniciar los arrays cuando eso pase.
    public void allFalse() {
        for (int i = 0; i < keys.length; i++) keys[i] = false;
        for (int i = 0; i < mouseButtons.length; i++) mouseButtons[i] = false;
    }
    
    public void addEngine(Engine e) {
        this.e = e;
    }


    /*METODOS PARA EL TECLADO*/
    
    //consulta el array modificado por los eventos para concer si actualmente la tecla recibida está presionada
    public boolean isKeyDown(int keyCode) {
        return keys[keyCode];
    }
    
    public boolean isKeyReleased(int keyCode) {
        return keysReleased[keyCode];
    }
    
    //en el array de teclas, marca cada tecla que ha sido presionada como true
    @Override
    public void keyPressed(KeyEvent ev) {
        int key = ev.getKeyCode();
        if (key < keys.length && key >= 0) {
            keys[key] = true;
            keysWasPressed[key] = true;
        }
    }

    //lo mismo que keyPressed pero marca las teclas liberadas como false
    @Override
    public void keyReleased(KeyEvent ev) {
        int key = ev.getKeyCode();
        if (key < keys.length && key >= 0) keys[key] = false;
    }

    @Override public void keyTyped(KeyEvent e) {}
    
    
    /*METODOS PARA EL MOUSE*/
    
    public boolean isMouseDown(int button) {
        return mouseButtons[button];
    }
    
    public boolean isMouseReleased(int button) {
        return mouseButtonsReleased[button];
    }

    public int getMouseX() {
        return mouseX;
    }
    
    public int getMouseY() {
        return mouseY;
    }

    @Override
    public void mousePressed(MouseEvent e) {
        if (e.getButton() < mouseButtons.length && e.getButton() >= 0) mouseButtons[e.getButton()] = true;
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        int button = e.getButton();
        if (button < mouseButtons.length && button >= 0) mouseButtons[button] = false;
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
    }

    //metofod innecesarios
    @Override public void mouseClicked(MouseEvent e) {}
    @Override public void mouseEntered(MouseEvent e) {}
    @Override public void mouseExited(MouseEvent e) {}
    
    
    
    //update de las teclas liueradas
    public void clearReleased() {
        for (int i = 0; i < keysReleased.length; i++) {
            keysReleased[i] = false;
        }
        
        for (int i = 0; i < mouseButtonsReleased.length; i++) {
            mouseButtonsReleased[i] = false;
        }
    }
    
    
    
    //ACTUALIZACION DE LOS METODOS RELEASED
    
    public void update() {
        updateKeys();
        updateMouseButtons();
    }
    
    private void updateKeys() {
        for (int i = 0; i < keys.length; i++) {
            
            if (keys[i]) {
                
                //si está presionado actualmente no puede ser liberado
                keysReleased[i] = false;
                keysWasPressed[i] = true;
                
            } else {
                
                //si no está presionado
                if (keysWasPressed[i]) {
                    keysReleased[i] = true;
                    keysWasPressed[i] = false;
                } else {
                    keysReleased[i] = false;
                }
                
            }
            
        }
    }
    
    private void updateMouseButtons() {
        for (int i = 0; i < mouseButtons.length; i++) {
            
            if (mouseButtons[i]) {
                
                //si está presionado actualmente no puede ser liberado
                mouseButtonsReleased[i] = false;
                mouseButtonsWasPressed[i] = true;
                
            } else {
                
                //si no está presionado
                if (mouseButtonsWasPressed[i]) {
                    mouseButtonsReleased[i] = true;
                    mouseButtonsWasPressed[i] = false;
                } else {
                    mouseButtonsReleased[i] = false;
                }
                
            }
            
        }
    }
}
