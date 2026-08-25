package Azigram;

import java.util.Arrays;

import PamUtils.complex.ComplexArray;
import fftManager.FFTDataUnit;
import fftManager.NonMagnitudeSpectrogramData;

/**
 * Just a dirty hack for testing. This AzigramDataUnit is just an FFTDataUnit, 
 * But override the getMagnitude() function to run the demux and Azigram
 * routines.
 * @author brian_mil
 *
 */
public class AzigramDataUnit extends FFTDataUnit implements NonMagnitudeSpectrogramData {

	ComplexArray P;
	double[] vx, vy, f, directionalData;
	private double[] directionalMagnitude;
	private double[] excessAboveBackground;
	
	public AzigramDataUnit(long timeMilliseconds, int channelBitmap, long startSample, long duration,
			ComplexArray fftData, int fftSlice) {
		super(timeMilliseconds, channelBitmap, startSample, duration, fftData, fftSlice);
	}

	@Override
	public double[] getMagnitudeData() {
		double[] magSqData = super.getMagnitudeData();
		
		return Arrays.copyOf(magSqData, getP().length());
	
	}
	
	public ComplexArray getP() {
		return P;
	}

	public void setP(ComplexArray p) {
		P = p;
	}

	public double[] getVx() {
		return vx;
	}

	public void setVx(double[] vx) {
		this.vx = vx;
	}

	public double[] getVy() {
		return vy;
	}

	public void setVy(double[] vy) {
		this.vy = vy;
	}

	public double[] getF() {
		return f;
	}

	public void setF(double[] f) {
		this.f = f;
	}

	public double[] getDirectionalAngle() {
		return directionalData;
	}

	public void setDirectionalAngle(double[] mu) {
		directionalData = mu;
	}
	
	@Override
	public double[] getSpectrogramData() {
		return getDirectionalAngle();
	}
	
	/**
	 * How far (in dB) each cell's magnitude sits above the tracked per-bin
	 * noise-floor percentile (see AzigramProcess's AzigramPercentileBackground)
	 * - a better basis for deciding "is this cell real signal or background
	 * noise" than a fixed absolute dB threshold, since ambient noise varies by
	 * frequency, deployment, and season. Falls back to raw magnitude if the
	 * background tracker hasn't populated this for some reason.
	 * @return per-bin dB above the tracked background, or raw magnitude as a
	 * fallback.
	 */
	public double[] getSpectrogramAlpha() {
		return excessAboveBackground != null ? excessAboveBackground : getDirectionalMagnitude();
	}

	public void setExcessAboveBackground(double[] excessAboveBackground) {
		this.excessAboveBackground = excessAboveBackground;
	}

	public double[] getExcessAboveBackground() {
		return excessAboveBackground;
	}

	public void setDirectionalMagnitude(double[] mag) {
		directionalMagnitude = mag;
	}
	
	public double[] getDirectionalMagnitude() {
		return directionalMagnitude;
	}
}
