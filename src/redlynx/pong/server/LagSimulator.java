package redlynx.pong.server;

import java.util.Comparator;
import java.util.PriorityQueue;

public class LagSimulator extends Thread{

	private int inputLag;
	private int outputLag;
	
	enum MessageDir {
		Send,
		Receive
	}
	private class QueueItem {
		 
		public ServerPlayer player;
		public String message;
		public long timeToSend;
		public MessageDir messageDir;
		
		public QueueItem(ServerPlayer player, String message, MessageDir messageDir, long time) {
			this.player = player;
			this.message = message;
			this.timeToSend = time;
			this.messageDir = messageDir;
		}
	}
	
	private PriorityQueue<QueueItem> queue;
	public LagSimulator() {
		queue = new PriorityQueue<QueueItem>(200, new Comparator<QueueItem>() {
			@Override
			public int compare(QueueItem a, QueueItem b) {
				return (a.timeToSend<b.timeToSend?-1:1);
			}
			
		});
	}
	
	public void setInputLag(int lag) {
		inputLag = lag;
	}
	public void setOutputLag(int lag) {
		outputLag = lag;
	}
	
	public void run() {
		try {
			while(true) {
				
				synchronized (this) {
					long time = System.nanoTime();
					//if (!queue.isEmpty())
					//	System.out.println("peek "+queue.peek().timeToSend +" time "+time+" diff "+(queue.peek().timeToSend - time));
					while (!queue.isEmpty() && ((queue.peek().timeToSend - time) <= 0)) {
						
						QueueItem item = queue.remove();
						switch(item.messageDir) {
						case Send:
							item.player.sendMessage(item.message);	
							break;
						case Receive:
							item.player.messageReceived(item.message);	
							break;
						}	
					}
				}
				
				Thread.sleep(1);
			}
		}catch(InterruptedException e) {}
	}

	public synchronized void receive(ServerPlayer serverPlayer, String msg) {
		if (inputLag == 0)
				serverPlayer.messageReceived(msg);
		else
			queue.add(new QueueItem(serverPlayer, msg, MessageDir.Receive, System.nanoTime()+inputLag*1000000));
		
	}

	public synchronized void send(ServerPlayer serverPlayer, String msg) {
		if (outputLag == 0)
			serverPlayer.sendMessage(msg);
		else {
			//System.out.println("send laggy: "+System.currentTimeMillis()+" + "+outputLag);
			queue.add(new QueueItem(serverPlayer, msg, MessageDir.Send, System.nanoTime()+outputLag*1000000));
		}

		
	}
	
}
