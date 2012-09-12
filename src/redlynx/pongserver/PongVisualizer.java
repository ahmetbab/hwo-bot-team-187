package redlynx.pongserver;

import java.awt.Canvas;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;



public class PongVisualizer extends Canvas implements WindowListener {
	
	private Image imageBuffer;
	private Image renderBuffer;
	GameState model;
	int width;
	int height;
	PongVisualizer(GameState model) {
		this.model = model;
		width = 800;
		height = 600;
		imageBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		renderBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		
		Frame frame = new Frame("Pong");
		
		
		//frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(width,height);
		frame.addWindowListener(this);

		frame.add(this);
		frame.show();
	}

	
	
	public void render() {

		Graphics g = renderBuffer.getGraphics();
		
		g.clearRect(0, 0, width, height);
		
		int corner = 20;
		int border = 5;
		
		g.drawRect(corner-border, corner-border, model.screenWidth+2*border, model.screenHeight+2*border);
        g.drawRect(corner, corner, model.screenWidth,  model.screenHeight);

        g.drawRect(corner, corner + (int)model.paddle[0].y, model.paddleConfig.width, model.paddleConfig.height);
		g.drawRect(corner+model.screenWidth- model.paddleConfig.width, corner + (int)model.paddle[1].y, model.paddleConfig.width, model.paddleConfig.height);
		
		g.drawRect((int)(corner+model.ball.x-model.ball.conf.radius), (int)(corner+model.ball.y-model.ball.conf.radius), 2*model.ball.conf.radius, 2*model.ball.conf.radius);
		
		
		  
		
		g = imageBuffer.getGraphics();
		
		g.drawImage(renderBuffer,0,0,this);
		
		
		repaint();
		
	}
	public void paint(Graphics g) {
		g.drawImage(imageBuffer,0,0,this);
	}

	public void update(Graphics g) {
		paint(g);
	}
	
	
	
	
	
	
	
	@Override
	public void windowActivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosed(WindowEvent arg0) {
		System.exit(-1);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowClosing(WindowEvent arg0) {
		System.exit(-1);
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeactivated(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowDeiconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowIconified(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void windowOpened(WindowEvent arg0) {
		// TODO Auto-generated method stub
		
	}
}
