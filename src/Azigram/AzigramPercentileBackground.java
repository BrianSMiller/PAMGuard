package Azigram;

/**
 * Tracks a target percentile (rather than the mean) of a per-frequency-bin
 * signal, expressed in dB, over time.
 * <p>
 * Intended as a robust background/noise-floor estimate for the Azigram
 * module: ambient ocean noise varies by frequency, deployment, and season,
 * so a single fixed dB threshold (as originally used to decide whether a
 * cell was "real signal" or background noise for display fading) has to be
 * re-tuned by hand for every recording. A per-bin estimate that adapts
 * automatically is more robust - but a simple decaying mean (see
 * Spectrogram.SpectrumBackground) isn't a good choice for this specifically,
 * because real signal (e.g. whale calls) passing through gets folded into
 * the average and drags the "background" estimate upward, particularly
 * during periods with frequent calling.
 * <p>
 * This uses a simple stochastic-approximation quantile tracker (in the
 * style of Tierney 1983): the estimate takes a step towards each new value,
 * but by a DIFFERENT amount depending on whether the new value is above or
 * below the current estimate. For a target percentile p (0-1), the ratio of
 * the "step up" size to the "step down" size is set to p/(1-p). In steady
 * state the estimate then drifts up on the (1-p) fraction of samples that
 * exceed it and down on the p fraction that fall below it, with the drift
 * balancing to zero exactly when the estimate sits at the p-th percentile
 * of the underlying (slowly-varying) distribution. At p=0.5 with equal
 * steps, this is exactly the familiar decaying mean - so this is a strict
 * generalisation of that approach, not a different one. Choosing a lower
 * percentile (e.g. 20-30) gives an estimate that sits further below the
 * bulk of the signal, and stays accurate even when real signal is present a
 * meaningful fraction of the time - usually the more useful choice for a
 * noise-floor estimate specifically, as opposed to a "typical value"
 * estimate.
 *
 * @author brian_mil
 */
public class AzigramPercentileBackground {

	private double[] backgroundDb;
	private double stepUpDb;
	private double stepDownDb;
	private int nRunin;
	private int nDone;

	/**
	 * @param targetPercentile percentile to track, 0-100. Lower values (e.g.
	 * 20-30) sit further below the bulk of the signal and are usually a
	 * better noise-floor estimate than the median (50).
	 * @param stepDbPerFrame overall step size, in dB per processed frame,
	 * controlling how fast the estimate can move. Larger values adapt faster
	 * but are noisier; smaller values are slower but more stable. Tune this
	 * (along with targetPercentile) by trial and error against real data -
	 * the same way the original fixed dB threshold was tuned.
	 * @param nRunInFrames number of initial frames during which the estimate
	 * snaps directly to the incoming value, rather than slowly crawling up
	 * or down from zero, so it starts near the right level quickly.
	 */
	public void prepare(double targetPercentile, double stepDbPerFrame, int nRunInFrames) {
		double p = Math.max(0, Math.min(100, targetPercentile)) / 100.0;
		this.stepUpDb = stepDbPerFrame * p;
		this.stepDownDb = stepDbPerFrame * (1 - p);
		this.nRunin = Math.max(1, nRunInFrames);
		this.nDone = 0;
	}

	/**
	 * Update the per-bin background estimate with a new frame of dB-valued
	 * data, and return the updated estimate (the same array each call - see
	 * copyBackground() if you need a stable snapshot rather than a live,
	 * mutated-in-place reference).
	 * <p>
	 * Self-sizes to the length of dBValues on first use (or if that length
	 * changes), so the caller doesn't need to know the final bin count in
	 * advance.
	 * @param dBValues per-bin magnitude for this frame, in dB.
	 * @return the updated per-bin background estimate, in dB.
	 */
	public double[] process(double[] dBValues) {
		if (backgroundDb == null || backgroundDb.length != dBValues.length) {
			backgroundDb = new double[dBValues.length];
			nDone = 0;
		}
		nDone++;
		synchronized (this) {
			for (int i = 0; i < backgroundDb.length; i++) {
				double v = dBValues[i];
				if (!Double.isFinite(v)) { // guard against a NaN/infinite value creeping in
					continue;
				}
				if (nDone <= nRunin) {
					// Run-in: snap directly to the incoming value so the
					// estimate starts near the right level, rather than
					// crawling there one small step at a time from zero.
					backgroundDb[i] = v;
				}
				else if (v > backgroundDb[i]) {
					// Clamp so a single step can never overshoot past the
					// actual sample - avoids oscillation if stepUpDb happens
					// to be large relative to how much the true background
					// actually varies frame to frame.
					backgroundDb[i] = Math.min(backgroundDb[i] + stepUpDb, v);
				}
				else if (v < backgroundDb[i]) {
					backgroundDb[i] = Math.max(backgroundDb[i] - stepDownDb, v);
				}
			}
		}
		return backgroundDb;
	}

	/**
	 * @return a defensive copy of the current per-bin background estimate,
	 * or null if process() hasn't been called yet. Prefer this over holding
	 * onto the array returned by process(), which is reused and mutated in
	 * place on every subsequent call.
	 */
	public double[] copyBackground() {
		synchronized (this) {
			return backgroundDb == null ? null : backgroundDb.clone();
		}
	}

}
