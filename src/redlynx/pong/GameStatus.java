package redlynx.pong;


public class GameStatus {
	public class Player {
		double y;
		String name;
	}
	public class Position {
		double x;
		double y;
	}
	protected class Conf {
		int maxWidth;
		int maxHeight;
		int paddleHeight;
		int paddleWidth;
		int ballRadius;
		int tickInterval;
	}
	GameStatus() {
		left = new Player();
		right = new Player();
		ballPos = new Position();
		conf = new Conf();
	}
	
	public long time;
	public Player left;
	public Player right;
	public Position ballPos;
	public Conf conf;
		
	public String toString() {
		return "time: "+time+"\n"
		+"player1: y: "+left.y+" name "+left.name+"\n"
		+"player2: y: "+right.y+" name "+right.name+"\n"
		+"ball: x: "+ballPos.x+" y: "+ballPos.y+"\n"
		+"conf: area: ("+conf.maxWidth+", "+conf.maxHeight+") paddle ("+conf.paddleWidth+", "+conf.paddleHeight+") ball radius "+conf.ballRadius+" tickInterval: "+conf.tickInterval+"\n";
	}
	
}
