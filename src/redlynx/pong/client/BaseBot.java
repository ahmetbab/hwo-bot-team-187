package redlynx.pong.client;

import redlynx.pong.client.network.Communicator;
import redlynx.pong.client.network.PongMessageListener;
import redlynx.pong.ui.GameStateAccessorInterface;
import redlynx.pong.ui.PongVisualizer;

public interface BaseBot extends PongMessageListener {
	
	public void setName(String name);
	public void setVisualizer(PongVisualizer visualizer);
	public void setCommunicator(Communicator comm);
	
	public GameStateAccessorInterface getGameStateAccessor();
	
	public void start();

}
