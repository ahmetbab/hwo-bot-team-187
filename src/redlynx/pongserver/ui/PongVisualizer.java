package redlynx.pongserver.ui;

import java.awt.Color;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.StringTokenizer;

import javax.swing.JFrame;
import javax.swing.JPanel;

import redlynx.pongserver.GameState;
import redlynx.pongserver.PongServer;



public class PongVisualizer extends JPanel
implements WindowListener {
	
	private Image imageBuffer;
	private Image renderBuffer;
	GameState model;
	int width;
	int height;
	private int tickCounter;
	
	String ballSpeed = "Ball Speed";
	String ballRadius = "Ball Radius";
	String paddleHeight = "Paddle Height";
	String paddleWidth = "Paddle Width";
	String paddleSpeed = "Paddle Speed";
	String deflectionMode = "Deflection Mode";
	String deflectionValue1 = "Deflection Value 1";
	String deflectionValue2 = "Deflection Value 2";
	
	String screenX = "Screen X";
	String screenY = "Screen Y";
	
	String inputLag = "Input Lag (TODO)";
	String outputLag = "Output Lag(TODO)";
	String messageInterval = "Message Inteval";
	String tickInterval = "Tick Inteval??";
	
	String resetButton = "Reset";
	
	String pauseButton = "Pause";
	String resumeButton = "Resume";
	
	String kick1Button = "Kick Player 1";
	String kick2Button = "Kick Player 2";
	
	String saveButton1 = "Save1";
	String loadButton1 = "Load1";
	String saveButton2 = "Save2";
	String loadButton2 = "Load2";
	String saveButton3 = "Save3";
	String loadButton3 = "Load3";
	
	
	String saveFile = "settings";
	String saveFileSuffix = ".txt";
	
	private void saveSettings(int saveId) {
		try {
			File file = new File(saveFile+saveId+saveFileSuffix);
			PrintStream out = new PrintStream(new FileOutputStream(file));
			for (int i = 0; i < dataSliders.length; i++) {
				out.println(dataSliders[i].getTitle()+":"+dataSliders[i].getValue());
			}
			out.flush();
			out.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	private void loadSettings(int saveId) {
		try {
			File file = new File(saveFile+saveId+saveFileSuffix);
			int fileLenght = (int)file.length();
			byte[] data = new byte[fileLenght];
			FileInputStream stream = new FileInputStream(file);
			stream.read(data);
			StringTokenizer st = new StringTokenizer(new String(data), "\r\n");
			while (st.hasMoreTokens()) {
				StringTokenizer pair = new StringTokenizer(st.nextToken(),":");
				String title = "";
				int value = 0;
				if(pair.hasMoreTokens()) { 
					title = pair.nextToken();
					if(pair.hasMoreTokens()) {
						String valueToken = pair.nextToken();
						try {
							value = Integer.parseInt(valueToken);
						}
						catch(NumberFormatException e) {
							System.err.println("Invalid value for "+title+": "+valueToken);
						}
					}
				}
				
				
				for (int i = 0; i < dataSliders.length; i++) {
					if (title.equals(dataSliders[i].getTitle())) {
						dataSliders[i].setValue(value);
					}
				}
				
			}
			stream.close();
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void valueChanged(String title, int value) {
		
		System.out.println(""+title+" : "+value);
		
		if (ballSpeed.equals(title)) {
			model.setBallSpeed(value/10.0f);
		}
		else if (ballRadius.equals(title)) {
			model.setBallRadius(value);
		}
		else if (paddleHeight.equals(title)) {
			model.setPaddleHeight(value);
		}
		else if (paddleWidth.equals(title)) {
			model.setPaddleWidth(value);
		}
		else if (paddleSpeed.equals(title)) {
			model.setPaddleSpeed(value/10.0f);
		}
		else if (deflectionMode.equals(title)) {
			model.setDeflectionMode(value);
		}
		else if (deflectionValue1.equals(title)) {
			model.setDeflectionValue(0, value);
		}
		else if (deflectionValue2.equals(title)) {
			model.setDeflectionValue(1, value);
		}
		else if (screenX.equals(title)) {
			model.setScreenSize(value, model.screenHeight);
		}
		else if (screenY.equals(title)) {
			model.setScreenSize( model.screenWidth, value);
			
		}
		else if (inputLag.equals(title)) {
			server.setInputLag(value);
		}
		else if (outputLag.equals(title)) {
			server.setOutputLag(value);
		}
		else if (messageInterval.equals(title)) {
			server.setMessageInterval(value);
		}	
		else if (tickInterval.equals(title)) {
			model.setTickInterval(value);
		}
		else if (resetButton.equals(title)) {
			server.resetGame();
		}
		else if (pauseButton.equals(title)) {
			pauseResumeButton.setTitle(resumeButton);
			server.setPaused(true);
		}
		else if (resumeButton.equals(title)) {
			pauseResumeButton.setTitle(pauseButton);
			server.setPaused(false);
		}
		else if (kick1Button.equals(title)) {
			server.kickPlayer(0);
		}
		else if (kick2Button.equals(title)) {
			server.kickPlayer(1);
		}
		else if (saveButton1.equals(title)) {
			saveSettings(1);
		}
		else if (loadButton1.equals(title)) {
			loadSettings(1);
		}
		else if (saveButton2.equals(title)) {
			saveSettings(2);
		}
		else if (loadButton2.equals(title)) {
			loadSettings(2);
		}
		else if (saveButton3.equals(title)) {
			saveSettings(3);
		}
		else if (loadButton3.equals(title)) {
			loadSettings(3);
		}
	}
	private SliderPanel[] dataSliders;
	ButtonPanel pauseResumeButton;
	private PongServer server;
	
	public PongVisualizer(GameState model, PongServer server) {
		this.model = model;
		this.server = server;
		width = 800;
		height = 600;
		imageBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		renderBuffer = new BufferedImage(width,height,BufferedImage.TYPE_INT_ARGB);
		
		JFrame frame = new JFrame("Pong");
	    Container content = frame.getContentPane();
	    content.setBackground(Color.black);
	    this.setMinimumSize(new Dimension(width, height));
		this.setPreferredSize(new Dimension(width, height));
		
		//frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.setSize(width,height);
		frame.addWindowListener(this);

	    GridBagLayout gb = new GridBagLayout();
	    content.setLayout(gb);
	    GridBagConstraints c = new GridBagConstraints();
	    c.gridx = 0;
	    c.gridy = 0;
	    c.weightx = 1;
	    c.weighty = 1;
	    c.fill = GridBagConstraints.BOTH;
	    c.anchor = GridBagConstraints.NORTHWEST;
	    content.add(this, c);
	    JPanel controlArea = new JPanel(new GridLayout(12, 2));
	    controlArea.setBackground(Color.black);
	    
	    dataSliders = new SliderPanel[] {
	    		new SliderPanel(paddleSpeed,1, 200, (int)(model.paddleConfig.maxSpeed*10), this),
	    		new SliderPanel(deflectionMode, 0, 0, model.deflectionMode, this),
	    	    new SliderPanel(deflectionValue1, 0, 100, model.deflectionValue[0], this),
	    	    new SliderPanel(deflectionValue2, 0, 100, model.deflectionValue[1], this),
	    	    
	    		new SliderPanel(paddleHeight, 1, 300, model.paddleConfig.height, this),
	    	    new SliderPanel(paddleWidth, 1, 300, model.paddleConfig.width, this),
	    	    new SliderPanel(ballRadius, 1, 50, model.ball.conf.radius, this),
	    	    new SliderPanel(ballSpeed, 1, 200, (int)(model.ball.conf.speed*10), this),
	    	    
	    	    
	    	    new SliderPanel(screenX, 10, 1000, model.screenWidth, this),
	    	    new SliderPanel(screenY, 10, 1000, model.screenHeight, this),
	    	    
	    	    new SliderPanel(inputLag, 0, 500, 0, this),
	    	    new SliderPanel(outputLag, 0, 500, 0, this),
	    	    
	    	    new SliderPanel(messageInterval, 0, 100, 0, this),
	    	    new SliderPanel(tickInterval, 1, 250, model.tickInterval, this)
	    };
	    
	    
	    
	    JPanel empty = new JPanel();   empty.setBackground(Color.BLACK);   controlArea.add(empty);
	    empty = new JPanel();    empty.setBackground(Color.BLACK);    controlArea.add(empty);
	    
	    controlArea.add(new ButtonPanel(resetButton, this));
	    pauseResumeButton = new ButtonPanel(pauseButton, this);
	    controlArea.add(pauseResumeButton);
	    
	    controlArea.add(new ButtonPanel(kick1Button, this));
	    controlArea.add(new ButtonPanel(kick2Button, this));
	    
	    //controlArea.add(dataSliders[0]);
	    //empty = new JPanel();    empty.setBackground(Color.BLACK);    controlArea.add(empty);
	    for (int i = 0; i < dataSliders.length; i++) {
	    	controlArea.add(dataSliders[i]);
	    }
 	    controlArea.add(new ButtonPanel(new String[] {saveButton1,saveButton2,saveButton3}, this));
	    controlArea.add(new ButtonPanel(new String[] {loadButton1,loadButton2, loadButton3}, this));
	    
	    
	    
	    
	    
		
	    c.gridx++;
	    c.weightx = 0;
	    c.fill = GridBagConstraints.REMAINDER;
	    content.add(controlArea, c);
		frame.pack();
		//frame.add(this);
		render();
		frame.show();
		
	}

	
	
	public synchronized void render() {

		Graphics g = renderBuffer.getGraphics();
		
		g.clearRect(0, 0, width, height);
		
		int cornerx = 20;
		int cornery = 40;
		int border = 5;
		tickCounter++;
		
		
		
		String waitingForPlayer = "Waiting for player...";
		int ptx1 = cornerx;
		int ptx2 = cornerx+500;
		int pty = 20;
		g.setColor(Color.white);
		
		if (model.paddle[0].name != null) {
			g.drawString(model.paddle[0].name, ptx1, pty);
		}
		else {
			g.drawString(waitingForPlayer, ptx1, pty);
		}
			
		if (model.paddle[1].name != null) {
			g.drawString(model.paddle[1].name, ptx2, pty);
		}
		else {
			g.drawString(waitingForPlayer, ptx2, pty);
		} 
		
		
		g.setColor(Color.white);
		
		
		g.drawRect(cornerx-border, cornery-border, model.screenWidth+2*border, model.screenHeight+2*border);
        g.drawRect(cornerx, cornery, model.screenWidth,  model.screenHeight);

        g.drawRect(cornerx, cornery + (int)model.paddle[0].y, model.paddleConfig.width, model.paddleConfig.height);
		g.drawRect(cornerx+model.screenWidth- model.paddleConfig.width, cornery + (int)model.paddle[1].y, model.paddleConfig.width, model.paddleConfig.height);
		
		g.drawRect((int)(cornerx+model.ball.x-model.ball.conf.radius), (int)(cornery+model.ball.y-model.ball.conf.radius), 2*model.ball.conf.radius, 2*model.ball.conf.radius);
		
		
		  
		
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
