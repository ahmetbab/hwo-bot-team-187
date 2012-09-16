package redlynx.pong.client.state;

import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;

public class GameStatusSnapShot {

	public static class Player {
		public double y = -1;
		public String name;
	}

	public static class Conf {
		public Vector2i screenArea;
		public Vector2i paddleDimension;

		public Conf() {
			screenArea = new Vector2i();
			paddleDimension = new Vector2i();
		}

		public int ballRadius;
		public int tickInterval;
	}

	public long time;
	public final Player left;
	public final Player right;
	public final Vector2 ball;
	public final Conf conf;

	public GameStatusSnapShot() {
		left = new Player();
		right = new Player();
		ball = new Vector2();
		conf = new Conf();
	}

	public String toString() {
		return "time: "+time+"\n"
		+"player1: y: "+left.y+" name "+left.name+"\n"
		+"player2: y: "+right.y+" name "+right.name+"\n"
		+"ball: x: "+ ball.x+" y: "+ ball.y+"\n"
		+"conf: area: ("+conf.screenArea.x+", "+conf.screenArea.y+") paddle ("+conf.paddleDimension.x+", "+conf.paddleDimension.y+") ball radius "+conf.ballRadius+" tickInterval: "+conf.tickInterval+"\n";
	}
}
