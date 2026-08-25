package PamguardMVC;

import PamguardMVC.superdet.SuperDetection;

/**
 * Data units that can be plotted on the FX 2D displays, such as FFT data, beam former 
 * output, etc.  
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

}
