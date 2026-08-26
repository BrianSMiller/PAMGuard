package fftManager;

/**
 * Marker interface for an FFTDataUnit subclass whose getSpectrogramData()
 * values represent something other than FFT magnitude (e.g. the Azigram
 * module's bearing angle in degrees), while getMagnitudeData() still returns
 * the true underlying signal magnitude for that same cell.
 * <p>
 * Displays such as the Spectrogram Display can check for this interface to
 * decide whether to apply a magnitude-based fade/transparency effect,
 * instead of inferring it indirectly by comparing the two arrays for
 * inequality (or, in one other case, by comparing object references from two
 * separate calls to getSpectrogramData() and relying on incidental caching
 * behaviour) - both of which are fragile: either check could silently start
 * doing the wrong thing for every ordinary spectrogram the moment
 * FFTDataUnit's getMagnitudeData()/getSpectrogramData() implementation
 * changes, e.g. to cache its result for performance.
 *
 * @author brian_mil
 */
public interface NonMagnitudeSpectrogramData {
}
