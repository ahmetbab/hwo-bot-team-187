package redlynx.pong.util;

/**
 * Unfortunate hack to make Windows use more accurate timer interrupt period (15ms -> 1ms)  
 * 
 * https://blogs.oracle.com/dholmes/entry/inside_the_hotspot_vm_clocks
 */


public class WinTimerHack extends Thread {
	public static void fixTimerAccuracy() {
		new WinTimerHack().start();
	}
	public void run() {
		try {
			Thread.sleep(Long.MAX_VALUE);
		}catch(InterruptedException e){
		}
	}
}
