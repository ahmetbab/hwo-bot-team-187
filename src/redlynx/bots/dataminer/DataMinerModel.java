package redlynx.bots.dataminer;

import redlynx.pong.client.BaseBot;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

public class DataMinerModel implements PongModel {

    private final Vector2 out = new Vector2();
    private final BaseBot host;

    
	private static final int paddlePosAccuracy = 10;
	private static final int narrowAngleAccuracy = 3;
	private static final int wideAngleAccuracy = 3;
			;
	private static final int wideAngleRange = 3;
	
    
    float [][] deflectionData = new float[paddlePosAccuracy+1][ narrowAngleAccuracy+ wideAngleAccuracy+1]; 
    
    public DataMinerModel(BaseBot bot) {
        this.host = bot;
    }
    
    /**
     * 
     * @param pos paddle hit position,discrete values  [0-99] which compared to [-1, 1] 
     * @param angle discrete 
     */
    private void addData(int pos, int inK, float outK, float weight) {
    	
    	if (weight != 1)
    	System.out.println("pos "+pos+" inK "+inK+" outK "+outK+" old "+deflectionData[pos][inK]+
    			" error "+Math.abs(outK/deflectionData[pos][inK])+"("+(outK-deflectionData[pos][inK])+") w "+weight);
    	
    	deflectionData[pos][inK] = (1-weight)*deflectionData[pos][inK]+weight*outK;
    	
    }
    private float getData(int pos, int inK, float interpolateP, float interpolateK) {
    	//int inKi = inK+1<narrowAngleAccuracy+wideAngleAccuracy?inK+1:inK;
    	//int posi = pos+1<paddlePosAccuracy?pos+1:pos;
    	
    	int inKi = inK+1;
    	int posi = pos+1;
    	
    	float value11 = deflectionData[pos][inK];
    	float value12 = deflectionData[pos][inKi];
    	
    	float value21 = deflectionData[posi][inK];
    	float value22 = deflectionData[posi][inKi];
    	
    	
    	return 	(value11*(1-interpolateK)+value12*interpolateK)*(1-interpolateP)
    			+(value21*(1-interpolateK)+value22*interpolateK)*(interpolateP);
    	
    }
    
    private void testAgainstModel(PongModel model) {
    	System.out.println("Running tests:");
    	int paddlePosTests = 50;
    	int dirTests = 90;
    	for (int i = 0; i < paddlePosTests; i++) {
    		double pos = -1+i*(2.0/paddlePosTests);
    		
    		for (int dir = 0; dir < 4; dir++) {
    			double vx_in = (dir&1)==0?-1:1;
    			double vy_in = (dir&2)==0?-1:1;
    			
    			
    			
    			
    			for (int dirs = 0; dirs < dirTests; dirs++) {
    				double k = dirs* (wideAngleRange/(double)dirTests);
    				
    				Vector2 deflected = guess(pos, vx_in, vy_in*k);
    				Vector2 deflected2 = model.guess(pos, vx_in, vy_in*k);
    				//System.out.println(" "+deflected.x+" "+deflected.y+" "+deflected2.x+" "+deflected2.y);
    				
    				if ((deflected.x < 0 && deflected2.x > 0) || (deflected2.x < 0 && deflected.x > 0)) 
    					System.out.println("x not working"+pos+" "+vx_in+" "+(vy_in*k));
    				if ((deflected.y < 0 && deflected2.y > 0) || (deflected2.y < 0 && deflected.y > 0)) 
    					System.out.println("y not working"+pos+" "+vx_in+" "+(vy_in*k)+": "+deflected.y+" "+deflected2.y);
    			
    				double k1 = deflected.y/deflected.x;
    				double k2 = deflected2.y/deflected2.x;
    				
    				if (Math.abs(k1-k2) > 0.05)
    					System.out.println("k fail: "+pos+" "+vx_in+" "+(vy_in*k)+": "+k1+" "+k2);
    			
    			}
    			
    		}
    	}
    	System.out.println("Tests finished");
    }
    public void initialiseFromModel(PongModel model) {
    	
    	for (int paddlePos = 0; paddlePos < paddlePosAccuracy; paddlePos++) {
    		double dpos = -1 +paddlePos*(2.0/paddlePosAccuracy);
    		double vx_in = 1;
    		for (int angle = 0; angle < narrowAngleAccuracy; angle++) {
    			
    			double k = angle*(1.0/narrowAngleAccuracy);//k [0,1)
    			 
    			
    			Vector2 deflected = model.guess(dpos, vx_in, vx_in*k);
    			
    			float outK = (float) (deflected.y / Math.abs(deflected.x));
    			addData(paddlePos, angle, outK, 1);
    			
    		}
    		for (int angle = narrowAngleAccuracy; angle < narrowAngleAccuracy+wideAngleAccuracy; angle++) {
    			
    			double k = 1+(angle-narrowAngleAccuracy)* (wideAngleRange/(double)wideAngleAccuracy); 
    			 
    			Vector2 deflected = model.guess(dpos, vx_in, vx_in*k);
    			float outK = (float) (deflected.y / Math.abs(deflected.x));
    			addData(paddlePos, angle, outK, 1);
    			
    		}
    	}
    	
    	testAgainstModel(model);
    	
    }
    void initialiseFromData() {
    	
    }
    

    private void learnWithData(int pos, int k, float interpolateP, float interpolateK, float outK) {
    	
    	float mainWeight = 0.3f;
    	//getData(pos, inK, interpolateP, interpolateK)
    	int inKi = k+1;
    	int posi = pos+1;
    	
    	
    	//TODO do we need to take the old value in account here?
    	/*
    	float value11 = deflectionData[pos][k];
    	float value12 = deflectionData[pos][inKi];
    	
    	float value21 = deflectionData[posi][k];
    	float value22 = deflectionData[posi][inKi];
    	*/
    	
    	
    	//TODO should we modify the sample for each corner value
    	
    	//TODO maybe calculate the multiplier for the new sample based on the old interpolated value
    	//eg. error = 1.05 -> modify each corner value towards a value that is 5% bigger than previously
    	//float oldValue = getData(pos, k, interpolateP, interpolateK);
    	//float error = outK / oldValue; //TODO avoid div by zero

    	
    	//main data update
    	
    	addData(pos, k, outK,    mainWeight*(1-interpolateP)*(1-interpolateK));  
    	addData(pos, k+1, outK,  mainWeight*(1-interpolateP)*(interpolateK));
    	addData(pos+1, k, outK,  mainWeight*(interpolateP)*(1-interpolateK));
    	addData(pos+1, k+1, outK,mainWeight*(interpolateP)*(interpolateK));
    	
    	    	
    	//TODO add sample to surrounding elements with smaller weight
    	//not needed if deflectionsample array is kept small
    	
    }
    
    @Override
    public void learn(double pos, double vx_in, double vy_in, double vx_out, double vy_out) {
    	
     	double tpos;
    	double tvy_in;
    	double tvy_out;
    	if (vy_in < 0) {
    		tpos = -pos;
    		tvy_in = -vy_in;
    		tvy_out = -vy_out;
    	}
    	else {
    		tpos = pos;
    		tvy_in = vy_in;
    		tvy_out = vy_out;
    	}
    	
    	int discretePos = (int)(paddlePosAccuracy*(tpos+1)/2);
    	float pInterpolate = (float)((paddlePosAccuracy*(tpos+1)/2)-discretePos);
    	if (discretePos < 0) {
    		discretePos = 0;
    		pInterpolate = 0;
    	}
    	else if (discretePos >= paddlePosAccuracy) {
    		discretePos = paddlePosAccuracy-1;
    		pInterpolate = 0;
    	}
    	
    	double k = Math.abs(tvy_in / vx_in);
    	int discreteK = 0;
    	float kInterpolate =0;
    	if (k < 1) {
    		discreteK = (int) (narrowAngleAccuracy*k);
    		kInterpolate = (float)(narrowAngleAccuracy*k-discreteK);
    	}
    	else {
    		
    		discreteK = (int) (wideAngleAccuracy/(double)wideAngleRange*(k-1)+narrowAngleAccuracy);
    		kInterpolate = (float) ((wideAngleAccuracy/(double)wideAngleRange*(k-1)+narrowAngleAccuracy)-discreteK);
    		if (discreteK >= wideAngleAccuracy+narrowAngleAccuracy) {
    			discreteK = wideAngleAccuracy+narrowAngleAccuracy-1;
    			kInterpolate = 0;
    		}
    	}
    	
    	float outK = (float) (tvy_out / Math.abs(vx_out));
    	
    	learnWithData(discretePos, discreteK, pInterpolate, kInterpolate, outK);
    	
    	//float outK = getData(discretePos, discreteK, kInterpolate);
    	
    	
    	
    	
    	
    	
    	
    }

    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
    	
    	double tpos;
    	double tvy_in;
    	if (vy_in < 0) {
    		tpos = -pos;
    		tvy_in = -vy_in;
    	}
    	else {
    		tpos = pos;
    		tvy_in = vy_in;
    	}
    	
    	int discretePos = (int)(paddlePosAccuracy*(tpos+1)/2);
    	float pInterpolate = (float)((paddlePosAccuracy*(tpos+1)/2)-discretePos);
    	if (discretePos < 0) {
    		discretePos = 0;
    		pInterpolate = 0;
    	}
    	else if (discretePos >= paddlePosAccuracy) {
    		pInterpolate = 0;
    		discretePos = paddlePosAccuracy-1;
    	}
    	
    	double k = Math.abs(tvy_in / vx_in);
    	int discreteK = 0;
    	float kInterpolate =0;
    	if (k < 1) {
    		discreteK = (int) (narrowAngleAccuracy*k);
    		kInterpolate = (float)(narrowAngleAccuracy*k-discreteK);
    	}
    	else {
    		
    		discreteK = (int) (wideAngleAccuracy/(double)wideAngleRange*(k-1)+narrowAngleAccuracy);
    		kInterpolate = (float) ((wideAngleAccuracy/(double)wideAngleRange*(k-1)+narrowAngleAccuracy)-discreteK);
    		if (discreteK >= wideAngleAccuracy+narrowAngleAccuracy) {
    			discreteK = wideAngleAccuracy+narrowAngleAccuracy-1;
    			kInterpolate = 0;
    		}
    	}
    	float outK = getData(discretePos, discreteK, pInterpolate, kInterpolate);
    	if (vy_in >= 0)
    		outK = -outK;
    	if (vx_in > 0)
    		outK = -outK;
    		
    	
        out.x = -vx_in;
        out.y = +vx_in*outK;
        return out;
    }

    @Override
    public double getAngle(double vx_in, double vy_in) {return 0;}

    @Override
    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle) {
    	return guess(pos, vx_in, vy_in);
    }
    @Override
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed) {
    	 return guess(pos, vx_in, vy_in);
    }
    
    @Override
    public double modelError() {
        return 10000000;
    }
}
