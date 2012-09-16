package redlynx.pong.client.jbot;

import java.lang.reflect.Array;

import redlynx.pong.client.state.GameStatusSnapShot;

public class HistoryBuffer {
	private static final int historySize = 128; 
		
	private GameStatusSnapShot[] history;
	private int historyPointer;
	private int validHistory;

	public HistoryBuffer() {
		history = new GameStatusSnapShot[historySize];
		reset();
	}
	void reset() {
		historyPointer = -1;
		validHistory = 0; 
	}
	public int getHistorySize() {
		return validHistory;
	}
	
	public GameStatusSnapShot getStatus(int historyIndex) {
		assert(historyIndex < validHistory );
		//System.out.println("ix "+((historyPointer+historyIndex) % historySize)+"\n "+history[(historyPointer+historyIndex) % historySize]);
		return history[(historySize +historyPointer-historyIndex) % historySize]; 
	}
	
	public void addSnapShot(GameStatusSnapShot status) {
		historyPointer = (historyPointer+1) % historySize;
		history[historyPointer] =  status;
		validHistory++;
	}
	
}
