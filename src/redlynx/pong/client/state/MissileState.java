package redlynx.pong.client.state;

import redlynx.pong.util.Vector2;

public class MissileState {
	public Vector2 pos;
	public Vector2 vel;
	public long launchtime;
	public String code;
	
	public MissileState(Vector2 pos, Vector2 vel, long launchtime, String code) {
		this.pos = pos;
		this.vel = vel;
		this.launchtime = launchtime;
		this.code = code;
	}
}
