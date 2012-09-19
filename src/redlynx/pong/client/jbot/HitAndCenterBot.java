package redlynx.pong.client.jbot;

import redlynx.pong.client.BaseBot;
import redlynx.pong.client.Pong;
import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageParser;
import redlynx.pong.client.state.GameStatusSnapShot;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;

public class HitAndCenterBot extends JBot  
{


	public static void main(String[] args) {
		Pong.init(args, new HitAndCenterBot());
	}

	double prevCommand;
	
	

    public HitAndCenterBot() {
		prevCommand = 0;
	}

 
	
	
	
	private void act() {
		GameStatusSnapShot status = analyser.history.getStatus(0);
		if (analyser.getLastBallVel().x > 0)
			moveTo(status.conf.screenArea.y/2-status.conf.paddleDimension.y/2, 0);
		else {
			moveTo(analyser.getNextHomeCollision().pos.y, 0);
		}
	}
	

	

    @Override
    public String getDefaultName() {
        return "Hit and Center";
    }


}
