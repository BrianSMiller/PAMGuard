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
	 * in PERCENTILE mode - a better basis for deciding "is this cell real
	 * signal or background noise" than a fixed absolute dB threshold, since
	 * ambient noise varies by frequency, deployment, and season.
	 * <p>
	 * In ABSOLUTE mode (see AzigramParameters.BackgroundMode),
	 * excessAboveBackground is never populated, so this falls back to
	 * getMagnitudeData() instead of getDirectionalMagnitude(). That's a
	 * deliberate choice, not an arbitrary fallback: getMagnitudeData() is
	 * genuinely calibrated dB re 1uPa (it operates on P, the demuxed
	 * omnidirectional signal - see AzigramProcess.runDemux()'s
	 * newFFTUnit.setFftData(P) - and the inherited FFTDataUnit machinery
	 * already runs that through PAMGuard's standard Array Manager hydrophone
	 * sensitivity / fftAmplitude2dB() calibration chain automatically).
	 * getDirectionalMagnitude() (mag[] in AzigramProcess.runAzigram()), by
	 * contrast, is 20*log10 of a cross-product of P with the demuxed
	 * sidebands used to compute bearing - not a physically meaningful sound
	 * pressure level on its own, so it's meaningless to compare against a
	 * user-set absolute dB SPL threshold.
	 * <p>
	 * Note this calibration is only as good as the Array Manager hydrophone
	 * sensitivity configured for the channel - it does not (yet) apply any
	 * sonobuoy/receiver frequency-response correction of the kind described
	 * in the DIFAR Localisation module's own help documentation.
	 * @return per-bin dB above the tracked background (PERCENTILE mode), or
	 * calibrated dB re 1uPa (ABSOLUTE mode fallback).
	 */
	public double[] getSpectrogramAlpha() {
		return excessAboveBackground != null ? excessAboveBackground : getMagnitudeData();
	}

	@Override
	public double[] getAlphaData() {
		return getSpectrogramAlpha();
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
