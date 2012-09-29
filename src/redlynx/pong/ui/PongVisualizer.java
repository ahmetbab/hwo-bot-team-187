package redlynx.pong.ui;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

import javax.swing.JPanel;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;




public class PongVisualizer extends JPanel

{

    
	private static final long serialVersionUID = 1L;
	
	private Image imageBuffer;
    private Image renderBuffer;

    private static final int cornerx = 20;
    private static final int cornery = 40;
    private static final int border = 5;

    GameStateAccessorInterface model;
    int width;
    int height;

    public PongVisualizer(GameStateAccessorInterface model) {
        this.model = model;
        width = 800;
        height = 600;
        imageBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
        renderBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);

        this.setMinimumSize(new Dimension(width, height));
        this.setPreferredSize(new Dimension(width, height));

    }



    public synchronized void render() {

        Graphics g = renderBuffer.getGraphics();

        g.clearRect(0, 0, width, height);




        String waitingForPlayer = "Waiting for player...";
        int ptx1 = cornerx;
        int ptx2 = cornerx+500;
        int pty = 20;
        g.setColor(Color.white);

        if (model.getPlayerName(0) != null) {
            g.drawString(model.getPlayerName(0), ptx1, pty);
        }
        else {
            g.drawString(waitingForPlayer, ptx1, pty);
        }

        if (model.getPlayerName(1)!= null) {
            g.drawString(model.getPlayerName(1), ptx2, pty);
        }
        else {
            g.drawString(waitingForPlayer, ptx2, pty);
        }

        int renderStates = model.getNumberOfStatesToRender();
        for (int i = 0; i < renderStates; i++) {

            model.setRenderState(i);
            

            g.setColor(model.getRenderColor(i));

            Vector2i screenSize = model.getAreaDimensions();
            Vector2i paddleDimensions = model.getPedalDimensions();

            g.drawRect(cornerx -border,cornery -border, screenSize.x + 2 * border, screenSize.y + 2 * border);
            g.drawRect(cornerx, cornery, screenSize.x, screenSize.y);

            g.drawRect(cornerx, cornery+(int) model.getPedalY(0), paddleDimensions.x, paddleDimensions.y);
            g.drawRect(cornerx+screenSize.x - paddleDimensions.x, cornery+(int)model.getPedalY(1), paddleDimensions.x, paddleDimensions.y);


            Vector2 ballPos = model.getBallPos();
            int r = model.getBallRadius();
            g.drawRect((int)(cornerx+ballPos.x-r), (int)(cornery+ballPos.y-r), 2*r, 2*r);
            
            ArrayList<Vector2> missiles = model.getMissilePositions();
            if (missiles != null) {
            	g.setColor(Color.red);
            	for (int mis = 0; mis < missiles.size(); mis++) {
            		Vector2 pos  = missiles.get(mis);
            		g.drawLine((int)(cornerx + pos.x - 5), (int)(cornery + pos.y - 5), (int)(cornerx + pos.x + 5), (int)(cornery + pos.y + 5));
            		g.drawLine((int)(cornerx + pos.x - 5), (int)(cornery + pos.y + 5), (int)(cornerx + pos.x + 5), (int)(cornery + pos.y - 5));
            	}
            
            }
            
        }
        


        ArrayList<UILine> extraUILines = model.getExtraLines();
        if (extraUILines != null) {
        	
            for (int i = 0; i < extraUILines.size(); ++i) {
                UILine line = extraUILines.get(i);
                drawLine(line.getStart(), line.getEnd(), line.getColor());
            }
        }

        ArrayList<UIString> extraUIStrings = model.getExtraStrings();
        if (extraUIStrings != null) {
            for (int i = 0; i < extraUIStrings.size(); i++) {
                g.setColor(extraUIStrings.get(i).getColor());
                String text = extraUIStrings.get(i).getText();
                Vector2i pos = extraUIStrings.get(i).getPos();
                g.drawString(text, cornerx+pos.x, cornery+pos.y);
            }
        }




        g = imageBuffer.getGraphics();

        g.drawImage(renderBuffer,0,0,this);


        repaint();

    }

    private void drawLine(Vector2i start, Vector2i end, Color color) {
        Graphics graphics = renderBuffer.getGraphics();
        graphics.setColor(color);
        graphics.drawLine(cornerx + start.x, cornery + start.y, cornerx + end.x, cornery + end.y);
    }

    public void paint(Graphics g) {
        g.drawImage(imageBuffer,0,0,this);
    }

    public void update(Graphics g) {
        paint(g);
    }

}
