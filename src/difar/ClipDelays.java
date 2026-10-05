package difar;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Time delays between DIFAR clips of the same call, measured by
 * cross-correlating their spectrograms with {@link ClipCorrelator}.
 * <p>
 * Each clip's spectrogram is made once from the omni channel of its
 * demultiplexed audio, and each pair's delay is measured once. Both are kept
 * in caches of limited size. A delay depends only on the two clips' audio and
 * frequency limits, not on the bearing chosen in the DIFARGram, so matching a
 * clip again after a click on the DIFARGram finds its delays in the cache and
 * does not correlate again.
 * <p>
 * The demultiplexed audio is taken to start at the clip's time. That holds
 * for the AMMC demultiplexer. The Greeneridge demultiplexer trims audio where
 * it lost lock on the carrier, and the trim is not recorded, so its clips can
 * start later than their time.
 * <p>
 * Matching runs on the demultiplexing worker's thread, the offline task
 * thread, and the Swing event thread after a click on the DIFARGram. The
 * methods here are synchronized, so one correlation runs at a time.
 */
public class ClipDelays {

	/** Most spectrograms kept. About 150 kB each for a 2 s clip. */
	private static final int MAX_SPECTROGRAMS = 200;

	/** Most pair delays kept. */
	private static final int MAX_DELAYS = 20000;

	private final DifarControl difarControl;

	private final Map<String, double[][]> spectrograms = lru(MAX_SPECTROGRAMS);

	/** Delay in seconds for each pair, or NaN where no peak passed the threshold. */
	private final Map<String, Double> delays = lru(MAX_DELAYS);

	private ClipCorrelator correlator;

	/** The settings the correlator and the cached delays were made with. */
	private String settingsKey;

	private int correlated, fromCache, noPeak, noAudio;
	private long correlationNanos;

	/**
	 * @param difarControl the DIFAR module, for its settings and FFT sizes.
	 */
	public ClipDelays(DifarControl difarControl) {
		this.difarControl = difarControl;
	}

	/**
	 * The delay of a call from one clip to another: its arrival time on the
	 * other clip minus its arrival time on the first.
	 * @param first the first clip, normally the one being matched.
	 * @param other a clip on another sonobuoy that may hold the same call.
	 * @param maxDelaySeconds largest delay possible between the two
	 * sonobuoys, in seconds.
	 * @return the delay of the highest correlation peak, in seconds, or null
	 * if either clip has no audio, the clips cannot be compared, or no peak
	 * passed the threshold.
	 */
	public synchronized Double getDelaySeconds(DifarDataUnit first, DifarDataUnit other, double maxDelaySeconds) {
		DifarParameters params = difarControl.getDifarParameters();
		checkSettings(params);
		float sampleRate = first.getDisplaySampleRate();
		int fftLength = difarControl.getDifarProcess().getDisplayFFTLength(first);
		int hop = difarControl.getDifarProcess().getDisplayFFTHop(first);
		if (sampleRate <= 0 || other.getDisplaySampleRate() != sampleRate
				|| difarControl.getDifarProcess().getDisplayFFTLength(other) != fftLength
				|| difarControl.getDifarProcess().getDisplayFFTHop(other) != hop) {
			noAudio++;
			return null;
		}
		int[] bins = binRange(first.getFrequency(), other.getFrequency(), sampleRate, fftLength);
		if (bins == null) {
			noAudio++;
			return null;
		}
		String key = String.format("%d:%d:%d:%d:%d:%d:%.3f", first.getUID(), first.getTimeMilliseconds(),
				other.getUID(), other.getTimeMilliseconds(), bins[0], bins[1], maxDelaySeconds);
		Double cached = delays.get(key);
		if (cached != null) {
			fromCache++;
			return cached.isNaN() ? null : cached;
		}
		double[][] specA = getSpectrogram(first, fftLength, hop);
		double[][] specB = getSpectrogram(other, fftLength, hop);
		if (specA == null || specB == null) {
			noAudio++;
			return null;
		}
		long start = System.nanoTime();
		double offsetSeconds = (other.getTimeMilliseconds() - first.getTimeMilliseconds()) / 1000.;
		List<ClipCorrelator.Peak> peaks = correlator.correlate(specA, specB, (double) hop / sampleRate,
				offsetSeconds, bins, maxDelaySeconds);
		correlationNanos += System.nanoTime() - start;
		correlated++;
		Double delay = peaks.isEmpty() ? null : peaks.get(0).getDelaySeconds();
		if (delay == null) {
			noPeak++;
		}
		delays.put(key, delay == null ? Double.NaN : delay);
		return delay;
	}

	/**
	 * Report what was done since the last report, and start counting again.
	 * @return a line for the console, or null if nothing was correlated.
	 */
	public synchronized String takeSummary() {
		String summary = null;
		if (correlated + fromCache + noAudio > 0) {
			summary = String.format("DIFAR: correlated %d pairs of clips in %.1f s; %d had no peak above the threshold, "
					+ "%d could not be correlated (no audio or different FFT settings), %d were already known",
					correlated, correlationNanos / 1e9, noPeak, noAudio, fromCache);
		}
		correlated = fromCache = noPeak = noAudio = 0;
		correlationNanos = 0;
		return summary;
	}

	/**
	 * Forget every spectrogram and delay, for example after clips are
	 * reloaded or their audio changes.
	 */
	public synchronized void clear() {
		spectrograms.clear();
		delays.clear();
	}

	/** Make a new correlator, and forget the delays, if the settings changed. */
	private void checkSettings(DifarParameters params) {
		String key = params.correlationThreshold + ":" + params.correlationSmoothing + ":"
				+ params.correlationMinSeparation + ":" + params.correlationMinOverlap;
		if (!key.equals(settingsKey)) {
			correlator = new ClipCorrelator(params.correlationThreshold, 1, params.correlationMinOverlap,
					params.correlationSmoothing, params.correlationMinSeparation);
			delays.clear();
			settingsKey = key;
		}
	}

	/**
	 * @return the spectrogram of a clip's omni channel, made once and kept,
	 * or null if the clip has no demultiplexed audio.
	 */
	private double[][] getSpectrogram(DifarDataUnit unit, int fftLength, int hop) {
		String key = String.format("%d:%d:%d:%d", unit.getUID(), unit.getTimeMilliseconds(), fftLength, hop);
		double[][] spec = spectrograms.get(key);
		if (spec != null) {
			return spec;
		}
		double[][] demux = unit.getDemuxedDecimatedData();
		if (demux == null || demux.length < 1 || demux[0] == null) {
			return null;
		}
		// the demultiplexed channels are omni, east-west, north-south
		spec = ClipCorrelator.spectrogram(demux[0], fftLength, hop);
		if (spec != null) {
			spectrograms.put(key, spec);
		}
		return spec;
	}

	/**
	 * The bins covering both clips' frequency limits.
	 * @return first bin and one past the last, or null if the limits do not
	 * overlap or are missing.
	 */
	static int[] binRange(double[] fA, double[] fB, float sampleRate, int fftLength) {
		if (fA == null || fB == null) {
			return null;
		}
		double low = Math.max(fA[0], fB[0]);
		double high = Math.min(fA[1], fB[1]);
		if (high <= low) {
			return null;
		}
		double binWidth = sampleRate / fftLength;
		int first = Math.max(0, (int) Math.floor(low / binWidth));
		int last = Math.min(fftLength / 2, (int) Math.ceil(high / binWidth) + 1);
		return last - first < 2 ? null : new int[] {first, last};
	}

	/** A map that forgets its least recently used entries beyond a size. */
	private static <K, V> Map<K, V> lru(int maxEntries) {
		return new LinkedHashMap<K, V>(16, 0.75f, true) {
			private static final long serialVersionUID = 1L;

			@Override
			protected boolean removeEldestEntry(Map.Entry<K, V> eldest) {
				return size() > maxEntries;
			}
		};
	}
}
