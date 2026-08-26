package PamguardMVC;

import PamguardMVC.superdet.SuperDetection;

/**
 * Data units that can be plotted on the FX 2D displays, such as FFT data, beam former 
 * output, etc.  
 * <p>
 * BeamOGramDataUnit is a natural second candidate for the getSpectrogramData()/
 * getAlphaData() overrides below - it also carries direction, frequency,
 * magnitude and time (see beamformer.continuous.BeamOGramDataUnit), much like
 * Azigram. It doesn't currently override either method: its getMagnitudeData()
 * collapses away the angle dimension entirely, and its angular view is shown
 * through a separate, dedicated display rather than through this generic
 * colour-mapped pipeline. If that ever changes - i.e. if BeamOGram's angle
 * data should be colour-mapped through a display like this one, the way
 * Azigram's bearing is - these two methods are where that hook already
 * exists, rather than something to reinvent from scratch.
 * @author Doug Gillespie
 *
 * @param <T>
 * @param <U>
 */
abstract public class DataUnit2D<T extends PamDataUnit, U extends SuperDetection> extends PamDataUnit<T, U> {

	public DataUnit2D(DataUnitBaseData basicData) {
		super(basicData);
	}

	public DataUnit2D(long timeMilliseconds, int channelBitmap, long startSample, long durationSamples) {
		super(timeMilliseconds, channelBitmap, startSample, durationSamples);
	}

	public DataUnit2D(long timeMilliseconds) {
		super(timeMilliseconds);
	}
	
	/**
	 * 
	 * @return data for plotting. Should be converted to the same scale as is 
	 * used by the plot axis (usually dB, but might be counts or some other data type)
	 */
	abstract public double[] getMagnitudeData();

	/**
	 * The value to actually display for this data unit in a 2D scrolling plot
	 * (e.g. the Spectrogram Display or its FX equivalent). Defaults to the
	 * magnitude, which is correct for the great majority of DataUnit2D types.
	 * A subclass whose plotted value legitimately differs from its magnitude -
	 * e.g. the Azigram module's bearing angle in degrees, while
	 * getMagnitudeData() still returns the true signal magnitude for that same
	 * cell - can override this instead, without affecting getMagnitudeData()'s
	 * own meaning for anything else that calls it (e.g. localisation).
	 * @return the values to be colour-mapped and drawn for this data unit.
	 */
	public double[] getSpectrogramData() {
		return getMagnitudeData();
	}

	/**
	 * The value used to decide how much to fade a cell towards the
	 * background/floor colour in a 2D scrolling plot (e.g. the Spectrogram
	 * Display or its FX equivalent). Defaults to the magnitude, which is
	 * correct for the great majority of DataUnit2D types - for those, this is
	 * simply the same value being displayed, so no fading occurs beyond what
	 * the display's own amplitude scale already does. A subclass whose
	 * displayed value (getSpectrogramData()) isn't itself a meaningful
	 * measure of signal strength - e.g. the Azigram module's bearing angle,
	 * which says nothing about how confident that bearing is - can override
	 * this to return a genuine confidence/strength measure instead, so the
	 * display can fade low-confidence cells even though their angle/colour
	 * value is just as "valid" a number as any other.
	 * @return the per-bin values to drive display fading for this data unit.
	 */
	public double[] getAlphaData() {
		return getMagnitudeData();
	}

}
