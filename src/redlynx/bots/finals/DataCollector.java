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

    public DataCollector(DataMinerModel model) {
        this.model = model;
        this.model.initialise();
        logging = System.out;
    }

    public void prepareDataCollect(Vector3 target, ClientGameState.Ball ballCollision) {
        logged = false;
        dataCollectCollisionPoint = target.y;
        dataCollectVelocityIn.x = ballCollision.vx;
        dataCollectVelocityIn.y = ballCollision.vy;
        dataCollectHitPos.x = ballCollision.x;
        dataCollectHitPos.y = ballCollision.y;
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

                logging.println("" + dataCollectCollisionPoint + "\t" + dataCollectVelocityIn.x + "\t" + dataCollectVelocityIn.y + "\t" + dataCollectVelocityOut.x + "\t" + dataCollectVelocityOut.y);

                model.learn(dataCollectCollisionPoint,
                        dataCollectVelocityIn.x, dataCollectVelocityIn.y,
                        dataCollectVelocityOut.x, dataCollectVelocityOut.y);
            }
            else {
                System.out.println("ignore border collision");
            }
        }
        else {
            System.out.println("Ignore too fast ball");
        }
    }

    public PongModel getModel() {
        return model;
    }

    public void gameOver() {
        logged = true;
    }

    public void learnFromFile(String s) {
        try {
            File logFile = new File(s);
            model.learnFromData(s);
            logging = new PrintStream(new BufferedOutputStream(new FileOutputStream(logFile, true)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}