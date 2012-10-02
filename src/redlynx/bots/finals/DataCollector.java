package redlynx.bots.finals;

import redlynx.bots.dataminer.DataMinerModel;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector3;

import java.io.*;

public class DataCollector {

    private final DataMinerModel model;
    private Vector2 dataCollectVelocityIn = new Vector2();
    private Vector2 dataCollectHitPos = new Vector2();
    private Vector2 dataCollectVelocityOut = new Vector2();
    private double dataCollectCollisionPoint;
    private PrintStream logging;
    private boolean logged = true;

    private boolean learn = false;
    private boolean doLogging;
    
    public DataCollector(DataMinerModel model, boolean doLogging) {
        this.model = model;
        this.model.initialise();
        logging = System.out;
        this.doLogging = doLogging;
    }

    public void prepareDataCollect(Vector3 target, ClientGameState.Ball ballCollision) {
    	
    	//if (!logged && ballCollision.getSpeed() > 200) {
    	//	if (Math.abs(dataCollectHitPos.y - ballCollision.y) > 1) {
    	//		System.out.println("Collision estimate Changed by: "+(dataCollectHitPos.y - ballCollision.y)+" ry "+((dataCollectHitPos.y - ballCollision.y)/ballCollision.vy));
    	//	}
    	//}
    	
        
        dataCollectCollisionPoint = target.y;
        dataCollectVelocityIn.x = ballCollision.vx;
        dataCollectVelocityIn.y = ballCollision.vy;
        dataCollectHitPos.x = ballCollision.x;
        dataCollectHitPos.y = ballCollision.y;
        
        logged = false;
    }

    public void setCollisionPoint(double y) {
        dataCollectCollisionPoint = y;
    }

    public boolean isLogged() {
        return logged;
    }

    public void updateModel(ClientGameState newStatus, PongGameBot bot) {
        logged = true;
        dataCollectVelocityOut.x = newStatus.ball.vx;
        dataCollectVelocityOut.y = newStatus.ball.vy;

        if(bot.getBallPositionHistory() != null && bot.getBallPositionHistory().getLastCollisionPoint() != null && bot.getBallPositionHistory().getLastCollisionPoint().x > bot.lastKnownStatus.conf.paddleWidth + bot.lastKnownStatus.conf.ballRadius + 5) {
            dataCollectVelocityOut.y *= -1;
        }

        double paddleCollisionPos = dataCollectHitPos.y - bot.lastKnownStatus.conf.paddleHeight / 2 * (dataCollectCollisionPoint + 1);

        if (bot.getBallVelocity() < 500) {

            //Do not accept collision points that are too close to borders, too much error in calculations
            if (paddleCollisionPos > bot.lastKnownStatus.conf.paddleHeight / 2 &&
                    paddleCollisionPos < bot.lastKnownStatus.conf.maxHeight - (bot.lastKnownStatus.conf.paddleHeight * 3) / 2) {

            	
            	if (doLogging) {
	                logging.println("" + dataCollectCollisionPoint + "\t" + dataCollectVelocityIn.x + "\t" + dataCollectVelocityIn.y + "\t" + dataCollectVelocityOut.x + "\t" + dataCollectVelocityOut.y);
	                logging.flush();
            	}
                if (learn)
                	model.learn(dataCollectCollisionPoint,
                			dataCollectVelocityIn.x, dataCollectVelocityIn.y,
                			dataCollectVelocityOut.x, dataCollectVelocityOut.y);
            }
            else {
                // ignore border collision
            }
        }
        else {
            // Ignore too fast ball
        }
    }

    public PongModel getModel() {
        return model;
    }

    public void gameOver() {
        logged = true;
    }

    public void learnFromFile(String s, int times) {
        try {
            File logFile = new File(s);
            model.learnFromData(s, times);
            logging =  new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public void optimizeModel(int passes) {
    	model.optimizeModel(passes);
    	
    }
}