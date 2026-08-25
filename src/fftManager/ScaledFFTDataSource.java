package fftManager;

/**
 * Optional interface for a PamProcess whose FFTDataBlock output isn't a normal
 * dB magnitude spectrum, but some other physical quantity expressed per
 * frequency bin - for example the Azigram module's bearing angle in degrees.
 * <p>
 * Displays such as the Spectrogram Display default their colour scale to a
 * dB-oriented range (see SpectrogramParameters.amplitudeLimits). For a source
 * like this, that default silently clamps almost the whole value range to a
 * single flat colour. A process can implement this interface so that a
 * display picking it up as a data source can adopt a sensible colour scale
 * automatically, rather than requiring the user to find and manually
 * reconfigure a "Scales" tab that gives no indication it needs changing.
 * <p>
 * Kept in fftManager (rather than in Spectrogram or Azigram) so that neither
 * of those packages needs to depend on the other's classes to make use of it.
 *
 * @author brian_mil
 */
public interface ScaledFFTDataSource {

	/**
	 * @return the recommended lower bound of the display colour scale for this
	 * source's output values (in whatever units they're expressed in - e.g.
	 * degrees for a bearing source).
	 */
	double getRecommendedScaleMin();

	/**
	 * @return the recommended upper bound of the display colour scale for this
	 * source's output values.
	 */
	double getRecommendedScaleMax();

	/**
	 * @return true if this source's values wrap around (e.g. a compass bearing,
	 * where 359 degrees is adjacent to 0 degrees) and so are best displayed
	 * with a circular/cyclic colour map rather than a linear one. Defaults to
	 * false. Deliberately just a boolean hint rather than naming a specific
	 * colour map - Swing and FX each have their own, separate ColourArrayType
	 * enum, and this interface has no dependency on either.
	 */
	default boolean isCircularScale() {
		return false;
	}

}
