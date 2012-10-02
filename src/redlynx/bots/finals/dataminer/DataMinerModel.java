package redlynx.bots.finals.dataminer;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

public class DataMinerModel implements PongModel {

    private final Vector2 out = new Vector2();
    
	private static final int paddlePosAccuracy = 10;
	private static final int narrowAngleAccuracy = 10;
	private static final int wideAngleAccuracy = 20;
	private static final int wideAngleRange = 3;
	
	private PongModel model;
    
    float [][] deflectionData = new float[paddlePosAccuracy+1][ narrowAngleAccuracy+ wideAngleAccuracy+1]; 
    
    public DataMinerModel(PongModel referenceModel) {
        this.model = referenceModel;
    }
    
    /**
     *
     * @param pos paddle hit position,discrete values  [0-99] which compared to [-1, 1]
     */
    private void addData(int pos, int inK, float outK, float weight) {

        float newValue = (1-weight)*deflectionData[pos][inK]+weight*outK;
    	if (weight != 1) {
    		//System.out.println("pos "+pos+" inK "+inK+" outK "+outK+" old "+deflectionData[pos][inK]+
    		//		" error "+Math.abs(outK/deflectionData[pos][inK])+"("+(outK-deflectionData[pos][inK])+") w "+weight);
    		if (Math.abs(newValue-deflectionData[pos][inK]) > 0.1) {
    			// System.out.println("BIG ERROR!!!" + (newValue-deflectionData[pos][inK]));
    			// System.out.println("pos "+pos+" inK "+inK+" outK "+outK+" old "+deflectionData[pos][inK]+
                // " error "+Math.abs(outK/deflectionData[pos][inK])+"("+(outK-deflectionData[pos][inK])+") w "+weight);
    			return;
    		}
    	}
    	
    		
    	
    	deflectionData[pos][inK] = newValue;
    	
    }
    
    /**
     *  Bilinear interpolated outK based on paddle hit position and inK 
     * @param pos
     * @param inK
     * @param interpolateP
     * @param interpolateK
     * @return estimated outK
     */
    private float getDataBilinear(int pos, int inK, float interpolateP, float interpolateK) {
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
    
    private float spline(float t, float p0, float p1, float p2, float p3) {
		float x1 = t*((2-t)*t-1);
		float x2 = t*t*(3*t-5)+2;
		float x3 = t*((4-3*t)*t+1);
		float x4 = (t-1)*t*t;
		return 0.5f*(p0*x1+p1*x2+p2*x3+p3*x4);
    }
    
    private float getDataBicubic(int pos, int inK, float interpolateP, float interpolateK) {
    	//int inKi = inK+1<narrowAngleAccuracy+wideAngleAccuracy?inK+1:inK;
    	//int posi = pos+1<paddlePosAccuracy?pos+1:pos;
    	
    	int inKm = inK-1;
    	int posm = pos-1;
    	if (inKm < 0) inKm = 0;
    	if (posm < 0) posm = 0;
    		
    	
    	
    	int inKi = inK+1;
    	int posi = pos+1;
    	if (inKi >= narrowAngleAccuracy+wideAngleAccuracy)
    		inKi = narrowAngleAccuracy+wideAngleAccuracy-1;
    	if (posi >= paddlePosAccuracy)
    		posi = paddlePosAccuracy-1;
    	
    	
    	int inK2 = inK+2;
    	int pos2 = pos+2;
    	if (inK2 >= narrowAngleAccuracy+wideAngleAccuracy)
    		inK2 = narrowAngleAccuracy+wideAngleAccuracy-1;
    	if (pos2 >= paddlePosAccuracy)
    		pos2 = paddlePosAccuracy-1;
    		
    	
    	float p00 = deflectionData[posm][inKm];
    	float p01 = deflectionData[posm][inK];
    	float p02 = deflectionData[posm][inKi];
    	float p03 = deflectionData[posm][inK2];
    	
    	float p10 = deflectionData[pos][inKm];
    	float p11 = deflectionData[pos][inK];
    	float p12 = deflectionData[pos][inKi];
    	float p13 = deflectionData[pos][inK2];
    	
    	float p20 = deflectionData[posi][inKm];
    	float p21 = deflectionData[posi][inK];
    	float p22 = deflectionData[posi][inKi];
    	float p23 = deflectionData[posi][inK2];
    	
    	float p30 = deflectionData[pos2][inKm];
    	float p31 = deflectionData[pos2][inK];
    	float p32 = deflectionData[pos2][inKi];
    	float p33 = deflectionData[pos2][inK2];

    	return spline(interpolateP, 
    			spline(interpolateK, p00,p01,p02,p03),
    			spline(interpolateK, p10,p11,p12,p13),
    			spline(interpolateK, p20,p21,p22,p23),
    			spline(interpolateK, p30,p31,p32,p33));   	
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
    public void initialise() {
    	initialiseFromModel(model);
    }
    public void initialiseFromModel(PongModel model) {
    	
    	for (int paddlePos = 0; paddlePos < paddlePosAccuracy+1; paddlePos++) {
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
    	
    	//testAgainstModel(model);
    	
    }


    public void learnFromData(String file, int times) {
    	
    	for (int i = 0; i < times; i++) {
	    	 try {
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line = null;
				while((line = reader.readLine()) != null) {
					StringTokenizer st = new StringTokenizer(line, "\n\r\t");
					double pos, xin,yin,xout,yout;
					pos = Double.parseDouble(st.nextToken());
					xin = Double.parseDouble(st.nextToken());
					yin = Double.parseDouble(st.nextToken());
					xout = Double.parseDouble(st.nextToken());
					yout = Double.parseDouble(st.nextToken());
					learn(pos, xin, yin, xout, yout);
				}
				reader.close();
				
			} catch (FileNotFoundException e) {
				System.out.println("Data file not found");
			}catch(Exception e) {
				System.out.println("Malformed data");
			}
	    	 smooth();
    	}	
    	    	
    }
    
    void initialiseFromData() {
    	
    }
    

    private void learnWithData(int pos, int k, float interpolateP, float interpolateK, float outK) {
    	
    	float mainWeight = 0.03f;
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
    		
    		//out of paddle hit (big ball radius), cannot learn
    		return;
    	}
    	else if (discretePos >= paddlePosAccuracy) {
    		discretePos = paddlePosAccuracy-1;
    		pInterpolate = 0;
    		//out of paddle hit (big ball radius), cannot learn
    		return;
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
    			
    			//too steep angle to learn
    			return;
    		}
    	}
    	
    	float outK = (float) (tvy_out / Math.abs(vx_out));
    	
    	learnWithData(discretePos, discreteK, pInterpolate, kInterpolate, outK);
    }

    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
    	
    	double k = Math.abs(vy_in / vx_in);
    	if (k < 0.2)
    		return model.guess(pos, vx_in, vy_in);
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
    	float outK = getDataBicubic(discretePos, discreteK, pInterpolate, kInterpolate);
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
    public void smooth() {
    	for (int i = 0; i < paddlePosAccuracy; i++) {
    		float prev; 
    		float current= deflectionData[i][0];
    		float next= deflectionData[i][1];
    		for (int k = 2; k < narrowAngleAccuracy+wideAngleAccuracy; k++) {
    			
    			prev = current;
    			current = next;
    			next =deflectionData[i][k];
    			
    			if (prev < current) {
    				deflectionData[i][k-1] = current = (prev+next)/2;
    				  
    			}
    		}
    	}
    	
    	for (int k = 0; k < narrowAngleAccuracy+wideAngleAccuracy; k++) {
			
    		
    		float prev; 
    		float current= deflectionData[0][k];
    		float next= deflectionData[1][k];
			
			for (int i = 2; i < paddlePosAccuracy; i++) {
				prev = current;
				current = next;
				next =deflectionData[i][k];
				if (prev > current) {
					deflectionData[i-1][k] = current = (prev+next)/2;
					  
				}
			}
		}
    }

    public void optimizeModel(int passes) {
    	double error = modelError();
    	double prevError;
    	double change = 0.1;
    	int p = 0;
    	do {
    		
    		prevError = error;
    		for (int i = 0; i < paddlePosAccuracy; i++) {
    			for (int k = 0; k < narrowAngleAccuracy+wideAngleAccuracy; k++) {
	    		
    				deflectionData[i][k] += change;
    				double error2 = modelError();
    				if (error2 < error) {
    					error = error2;
    					continue;
	    			}
	    			deflectionData[i][k] -= 2*change;
	    			error2 = modelError();
	    			if (error2 < error) {
	    				error = error2;
	    				continue;
	    			}
	    			deflectionData[i][k] += change;
	    		}
	    	}
    		smooth();
    		error = modelError();
	    	System.out.println("Error after pass: "+error+" c "+change );
	    	if (Math.abs(prevError -error) < 0.0001)
	    		change *= 0.5;
	    	p++;
    	}while (prevError != error && change > 0.005 && p < passes);
    }
    
    @Override
    public double modelError() {
    	
    	 double sqrErrorSum = 0;
	        int numSamples = 1;
    	
    	 try {
 			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream("miner1.txt")));
 			String line = null;
 			
 			while((line = reader.readLine()) != null) {
 				StringTokenizer st = new StringTokenizer(line, "\n\r\t");
 				double pos, inx,iny,outx,outy;
 				pos = Double.parseDouble(st.nextToken());
 				inx = Double.parseDouble(st.nextToken());
 				iny = Double.parseDouble(st.nextToken());
 				outx = Double.parseDouble(st.nextToken());
 				outy = Double.parseDouble(st.nextToken());
 			   Vector2 out = guessGivenSpeed(pos, inx, iny, Math.sqrt(inx*inx + iny*iny));

               double expected = out.y / out.x;
               double real = outy / outx;
               double error = expected - real;
               sqrErrorSum += error * error;
               ++numSamples;
 			}
 			reader.close();
 			
 		} catch (FileNotFoundException e) {
 			 System.out.println("model eval failed.");
 			System.out.println("Data file not found");
 		}catch(Exception e) {
 			 System.out.println("model eval failed.");
 			System.out.println("Malformed data");
 		}
    	
   

  

        return sqrErrorSum / numSamples;
    }
}
