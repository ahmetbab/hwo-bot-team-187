package redlynx.bots.finals.zeus3;

import java.awt.Color;
import java.util.ArrayList;

import redlynx.bots.finals.DataCollector;
import redlynx.bots.finals.dataminer.DataMinerModel;
import redlynx.bots.finals.dataminer.SauronState;
import redlynx.bots.finals.sauron.MissileDodger;
import redlynx.pong.client.Pong;
import redlynx.pong.client.PongGameBot;
import redlynx.pong.client.state.ClientGameState;
import redlynx.pong.collisionmodel.SFSauronGeneralModel;
import redlynx.pong.ui.UILine;
import redlynx.pong.ui.UIString;
import redlynx.pong.util.PongUtil;
import redlynx.pong.util.Vector2;
import redlynx.pong.util.Vector2i;
import redlynx.pong.util.Vector3;
import redlynx.pong.util.Visualisation;


public class Zeus3 extends PongGameBot {

    private String defaultName;
    private DataMinerModel dmModel;
    private final DataCollector dataCollector;

    private String decision;
    
    public Zeus3() {
        this("Zeus3");
    }

    public Zeus3(String name) {
        super();
        defaultName = name;
 
        decision = "";
        
        dataCollector = new DataCollector(new DataMinerModel(new SFSauronGeneralModel()), false);
        myModel = dataCollector.getModel();
        System.out.println("Avg SqrError in K: " + myModel.modelError());
        dataCollector.learnFromFile("miner1.txt",1);        
        System.out.println("Avg SqrError in K: " + myModel.modelError());
    }

    public static void main(String[] args) {
        Pong.init(args, new Zeus3());
    }

    private Zeus3Evaluator evaluator = new Zeus3Evaluator();
    private SauronState myState = new SauronState();
    private boolean shoutPlan = true;

    double timeLeft = 10000;

    @Override
    public void setName(String name) {
        super.setName(name);
    }
    
    @Override
    public void onGameStateUpdate(ClientGameState newStatus) {

        lines.clear();
        double ball_direction = newStatus.ball.vx;

        if(getMySide().comingTowardsMe(ball_direction)) {
        	
            // find out impact velocity and position.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());
            timeLeft = PongUtil.simulateOld(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);

            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.LEFT, timeLeft);
            double minReach = Math.max(-0.95, reach.x);
            double maxReach = Math.min(+0.95, reach.y);

            {
                // hack.. if angle is high, don't try to hit the ball with the wrong end of the paddle..
                double value = ballWorkMemory.vy / (Math.abs(ballWorkMemory.vx) + 0.000001);
                double pixelsPerTickEstimate = Math.abs((ballWorkMemory.vy)/(1000.0/21)); 
                double safelimit = (pixelsPerTickEstimate+1)/(lastKnownStatus.conf.paddleHeight/2);
                double amount = safelimit;
                
                if(value < 0.0 && minReach < -1+amount) {
                    minReach = -1+amount;
                }
                else if(value > 0.0 && maxReach > 1-amount) {
                    maxReach = +1-amount;
                }
            }

            // this is the expected y value when colliding against our paddle.
            Vector3 target = evaluator.myOffensiveEval(timeLeft, this, newStatus, PlayerSide.RIGHT, newStatus.getPedal(PlayerSide.RIGHT).y, ballWorkMemory, ballTemp, minReach, maxReach);

            // when no winning move available
            if(target.z < -10 //7*newStatus.conf.ballRadius
            		) {
            	
            	double offenceScore = target.z;
                ballWorkMemory.copy(newStatus.ball, true);
                ballWorkMemory.setVelocity(getBallVelocity());
                timeLeft = PongUtil.simulateNew(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
                target = evaluator.defensiveEval(0, 0, this, lastKnownStatus, PlayerSide.RIGHT, minReach, maxReach, ballWorkMemory);
                decision = "Defence "+target.z+" (offence "+offenceScore+")";
            }
            else {
            	decision = "Offence "+target.z;
            }
            
            double ultimateCornerShotLimit = newStatus.conf.ballRadius+4;
            boolean ultimateCornerShot = ballWorkMemory.y <  ultimateCornerShotLimit && ballWorkMemory.vy < 0 
            		|| ballWorkMemory.y > newStatus.conf.maxHeight- ultimateCornerShotLimit && ballWorkMemory.vy > 0;
            
            if (ultimateCornerShot) {
            	System.out.println("ultimate");
            	double k = ballWorkMemory.vy / ballWorkMemory.vx;
            	if (k > 0.6 && getBallVelocity() > 400) {
            		System.out.println("corner compensate");
            		double compensate = 2*newStatus.conf.ballRadius-2;
            		
            		double minVal = compensate;
            		double maxVal = newStatus.conf.maxHeight -1 - newStatus.conf.paddleHeight - compensate;
			
			        if (target.x < minVal) {
			            target.x = minVal;
			        }
			        if (target.x > maxVal) {
			            target.x = maxVal;
			        }
            	}
            }
            double movementSafetyMargin = 1;
            if (target.x <  movementSafetyMargin) {
	            target.x =  movementSafetyMargin;
	        }
	        if (target.x >= newStatus.conf.maxHeight- newStatus.conf.paddleHeight) {
	            target.x = newStatus.conf.maxHeight-1- newStatus.conf.paddleHeight-movementSafetyMargin;
	        }

            double targetPos = target.x;
            double paddleTarget = target.y;


            double requiredVelocityForMissiles = 100;

            {
                // time to target, time for missile. missile launcher reach.
                double halfPaddle = 0.5 * newStatus.conf.paddleHeight;
                double myPos = newStatus.left.y + halfPaddle;
                double distanceToTarget = Math.abs(target.x - myPos);
                double timeToTarget = distanceToTarget / getPaddleMaxVelocity();
                double timeForMissile = (timeLeft - timeToTarget) * 0.5;
                lines.add(new UILine(12, myPos - halfPaddle - timeForMissile * getPaddleMaxVelocity(), 12, myPos + halfPaddle + timeForMissile * getPaddleMaxVelocity(), Color.magenta));

                // see if should make an offensive missile shot with current plan.
                ClientGameState.Ball tmpBall = new ClientGameState.Ball();
                tmpBall.copy(ballWorkMemory, true);
                ballCollideToPaddle(target.y, tmpBall);
                double opponentTime = PongUtil.simulateNew(tmpBall, getLastKnownStatus().conf, null, null) + timeLeft;
                missileCommand.fireOffensiveMissiles(opponentTime, tmpBall, target);
                // requiredVelocityForMissiles = missileCommand.fireKillShotMissiles(timeForMissile, opponentTime, ballWorkMemory);
                Visualisation.visualizeOpponentReach(lines, this, opponentTime);
            }

            // draw stuff on the hud.
            visualiseModel(0, lastKnownStatus.getPedal(PlayerSide.LEFT).y, minReach, maxReach);
            visualisePlan(paddleTarget, Color.red);
            visualisePlan(0, Color.green);

            // check if we need to do something.
            if(myState.catching()) {
                double myPos = lastKnownStatus.getPedal(getMySide()).y;
                double distance = (targetPos - myPos);
                if(needToReact(targetPos) || reallyShouldUpdateRegardless()) {
                    changeCourse(distance);
                }
            }
        }
        else {

            // simulate twice, once there, and then back.
            ballWorkMemory.copy(newStatus.ball, true);
            ballWorkMemory.setVelocity(getBallVelocity());

            timeLeft = PongUtil.simulateNew(ballWorkMemory, lastKnownStatus.conf, lines, Color.green);
            Vector2 reach = getPaddlePossibleReturns(newStatus, ballWorkMemory, PlayerSide.RIGHT, timeLeft);

            double killshotMissileVel = missileCommand.fireKillShotMissiles(timeLeft, timeLeft, ballWorkMemory);

            if(killshotMissileVel > 1) {
                // no killshot available. do the normal stuff.
                double minReach = reach.x - 0.1;
                double maxReach = reach.y + 0.1;

                // this is the current worst case. should try to cover that?
                Vector3 target = evaluator.oppOffensiveEval(this, newStatus, PlayerSide.LEFT, newStatus.left.y + 0.5 * newStatus.conf.paddleHeight, ballWorkMemory, ballTemp, minReach, maxReach);
                double paddleTarget = target.y;

                // if no return is possible according to simulation. Then the least impossible return should be anticipated..
                if(target.z < -1000) {
                    double deltaPaddle = (ballWorkMemory.y - newStatus.right.y);
                    if(deltaPaddle > 0) {
                        // paddle is below ball. anticipate a high return.
                        paddleTarget = +1;
                    }
                    else {
                        // paddle above ball. anticipate a low return.
                        paddleTarget = -1;
                    }
                }

                // draw stuff on the hud.
                visualiseModel(lastKnownStatus.conf.maxWidth, lastKnownStatus.getPedal(PlayerSide.RIGHT).y, minReach, maxReach);
                visualisePlan(paddleTarget, Color.red);
                visualisePlan(0, Color.green);

                ballCollideToPaddle(paddleTarget, ballWorkMemory);
                double timeLeftAfter = PongUtil.simulateNew(ballWorkMemory, lastKnownStatus.conf, lines, Color.red);
                timeLeft += timeLeftAfter;

                // now we are done.
                ClientGameState.Player myPedal = lastKnownStatus.getPedal(getMySide());
                double diff_y = ballWorkMemory.y - myPedal.y;
                changeCourse(diff_y * 10000);
            }
            else {
                // killshot killshot! :D  lets have some shrooooms on the screen, mkay?
                changeCourse(killshotMissileVel * timeLeft * getPaddleMaxVelocity());
                for(int i=0; i<10; ++i) {
                    Visualisation.drawCross(lines, Color.PINK, Math.random() * lastKnownStatus.conf.maxWidth, Math.random() * lastKnownStatus.conf.maxHeight);
                    Visualisation.drawSquare(lines, Color.PINK, Math.random() * lastKnownStatus.conf.maxWidth, Math.random() * lastKnownStatus.conf.maxHeight);
                }
            }
        }

        getBallPositionHistory().drawLastCollision(lines);
        getPaddleVelocity().drawReachableArea(lines, newStatus.getPedal(getMySide()).y + newStatus.conf.paddleHeight * 0.5, timeLeft, newStatus.conf.paddleHeight);
    }

    private void visualisePlan(double paddleTarget, Color color) {
        ballTemp.copy(ballWorkMemory, true);
        ballCollideToPaddle(paddleTarget, ballTemp);
        PongUtil.simulateOld(ballTemp, lastKnownStatus.conf, lines, color);
    }

    private void visualiseModel(double x, double y, double minReach, double maxReach) {

        double targetPos = ballWorkMemory.y - lastKnownStatus.conf.paddleHeight * 0.5;
        double paddleMaxPos = lastKnownStatus.conf.maxHeight - lastKnownStatus.conf.paddleHeight;
        double paddleMinPos = 0;

        for(int i=0; i<11; ++i) {

            double pos = (i - 5) / 5.0;
            Color color = Color.green;

            if(pos < minReach || pos > maxReach) {
                color = Color.blue;
            }

            double evaluatedPaddlePos = targetPos - pos * lastKnownStatus.conf.paddleHeight * 0.5;
            if(paddleMaxPos < evaluatedPaddlePos || paddleMinPos > evaluatedPaddlePos) {
                color = Color.red;
            }

            Vector2 ballOut = myModel.guess(pos, ballWorkMemory.vx, ballWorkMemory.vy);
            ballOut.normalize().scaled(100);

            double y_point = y + pos * lastKnownStatus.conf.paddleHeight * 0.5 + lastKnownStatus.conf.paddleHeight * 0.5;
            lines.add(new UILine(new Vector2i(x, y_point), new Vector2i(x + ballOut.x, y_point + ballOut.y), color));

        }
    }

    private void changeCourse(double distance) {
        // ok seems we really have to change course.
        double idealVelocity = (distance / timeLeft / getPaddleMaxVelocity()); // this aims for the centre of current target
        if(idealVelocity * idealVelocity > 1.0) {
            if(idealVelocity > 0)
                idealVelocity = +1;
            else
                idealVelocity = -1;
        }

        idealVelocity = MissileDodger.dodge(lines, this, idealVelocity);

        if(idealVelocity != myState.velocity()) {
            requestChangeSpeed(idealVelocity);
        }
    }

    private boolean needToReact(double targetPos) {

        double myPos = lastKnownStatus.getPedal(getMySide()).y;
        double movingDistance = timeLeft * myState.velocity() * getPaddleMaxVelocity();

        double ballEndPos = ballWorkMemory.y;
        double expectedPosition = movingDistance + myPos + lastKnownStatus.conf.paddleHeight * 0.5;
        double expectedDistance = ballEndPos - expectedPosition;

        double halfPaddle = lastKnownStatus.conf.paddleHeight * 0.1;
        return expectedDistance * expectedDistance >= halfPaddle * halfPaddle || reallyShouldUpdateRegardless();
    }

    public boolean requestChangeSpeed(double v) {

        int requestedVelocity = (int)(v * 100);
        int prevVelocity = (int)(myState.velocity() * 100);
        int delta = requestedVelocity - prevVelocity;

        if(delta * delta < 5 && !reallyShouldUpdateRegardless()) {
            return false;
        }

        if(super.requestChangeSpeed(v)) {
            myState.setVelocity(v);
            return true;
        }

        return false;
    }


    @Override
    public void onGameOver(boolean won) {

        //clear collision logging info
    	dataCollector.gameOver();

        myState.setToHandling();
        myState.setVelocity(0);
        lastKnownStatus.reset();
        extrapolatedStatus.reset();
        extrapolatedTime = 0;
    }

    @Override
    public void onTick(double dt) {
    }

    @Override
    public String getDefaultName() {
        return defaultName;
    }

    @Override
    public ArrayList<UILine> getDrawLines() {
        return this.lines;
    }

    
    @Override
    public ArrayList<UIString> getDrawStrings() {
    	ArrayList<UIString> list =  super.getDrawStrings();
    	ArrayList<UIString> list2 = new ArrayList<UIString>(list);
    	list2.add(new UIString(decision, new Vector2i(0, -10),Color.green));
    	return list2;
    }
    
}
