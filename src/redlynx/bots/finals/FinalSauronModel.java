package redlynx.bots.finals;

import redlynx.pong.client.PongGameBot;
import redlynx.pong.collisionmodel.PongModel;
import redlynx.pong.util.Vector2;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class FinalSauronModel implements PongModel {

    private final Vector2 out = new Vector2();
    private final PongGameBot host;


    private double[] values = {0.419, 0.3234, 0.08, 0.1};

    /*
    private double constantWeight = 0.349;
    private double highWeight = -0.044;
    */

    public FinalSauronModel(PongGameBot bot) {
        this.host = bot;
        System.out.println(modelError());
    }

    @Override
    public void learn(double pos, double vx_in, double vy_in, double vx_out, double vy_out) {
    }

    @Override
    public Vector2 guess(double pos, double vx_in, double vy_in) {
        return guessGivenSpeed(pos, vx_in, vy_in, host.getBallVelocity());
    }

    @Override
    public double getAngle(double vx_in, double vy_in) {return 0;}

    @Override
    public Vector2 guessGivenAngle(double pos, double vx_in, double vy_in, double angle) {
        return guess(pos, vx_in, vy_in);
    }

    @Override
    public Vector2 guessGivenSpeed(double pos, double vx_in, double vy_in, double speed) {
        double inAngle = Math.asin(Math.abs(vy_in) / speed);

        /*
        out.x = -vx_in;
        out.y = +vy_in;
        int sign = vx_in > 0 ? -1 : +1;
        if(vy_in > 0 && pos > 0) {
            out.rotate(sign * inAngle * pos * values[0]);
        }
        else if(vy_in > 0 && pos < 0) {
            out.rotate(sign * inAngle * pos * values[1]);
        }
        else if(vy_in < 0 && pos > 0) {
            out.rotate(sign * inAngle * pos * values[1]);
        }
        else {
            out.rotate(sign * inAngle * pos * values[0]);
        }
        */

        double k = vy_in / vx_in;

        out.x = -vx_in;
        out.y = +vy_in + pos * speed * (values[0] + (inAngle * values[1]) + (k * values[2]));
        return out;
    }

    public void tweak() {
        double error = modelError();
        boolean improved = true;

        double delta = 1;

        for(int k=0; k<15; ++k) {
            improved = true;
            while (improved) {

                improved = false;
                double tmpError;

                for(int i=0; i<values.length; ++i) {
                    values[i] += delta;
                    tmpError = modelError();
                    if(tmpError < error) {
                        error = tmpError;
                        improved = true;
                        continue;
                    }
                    values[i] -= 2 *delta;
                    tmpError = modelError();
                    if(tmpError < error) {
                        error = tmpError;
                        improved = true;
                        continue;
                    }
                    values[i] += delta;
                }
            }

            System.out.println("Halved delta!");
            delta *= 0.5;
        }

        for(int i=0; i<values.length; ++i) {
            System.out.println("optimal value " + i + ": " + values[i]);
        }

        System.out.println("AvgSqr error in K: " + error);
    }

    @Override
    public double modelError() {
        File file = new File("pongdata.txt");

        double errorSum = 0;
        double sqrErrorSum = 0;
        int numSamples = 1;

        try {
            FileInputStream fis = new FileInputStream(file);
            Scanner scanner = new Scanner(fis);
            while(scanner.hasNext()) {
                double pos = scanner.nextDouble();
                double inx = scanner.nextDouble();
                double iny = scanner.nextDouble();
                double outx = scanner.nextDouble();
                double outy = scanner.nextDouble();

                guessGivenSpeed(pos, inx, iny, Math.sqrt(inx*inx + iny*iny));

                double expected = out.y / out.x;
                double real = outy / outx;
                double error = Math.abs(expected - real);
                sqrErrorSum += error * error;
                errorSum += error;
                ++numSamples;
            }
        } catch (FileNotFoundException e) {
            System.out.println("model eval failed.");
        }

        System.out.println("Model linear error: " + (errorSum / numSamples) + ", sqrError: " + (sqrErrorSum / numSamples));
        return errorSum / numSamples;
    }
}
