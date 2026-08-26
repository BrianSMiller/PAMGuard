package Azigram;

import java.io.Serializable;
import java.lang.reflect.Field;

import PamModel.parametermanager.ManagedParameters;
import PamModel.parametermanager.PamParameterSet;
import PamModel.parametermanager.PamParameterSet.ParameterSetType;
import PamModel.parametermanager.PrivatePamParameterData;
import PamView.GroupedSourceParameters;

public class AzigramParameters implements Serializable, ManagedParameters, Cloneable {

	static public final long serialVersionUID = 2;

	String name = "";

	public int channelBitmap;
	
	public GroupedSourceParameters dataSource = new GroupedSourceParameters();
	
	/**
	 * Seconds to average PSD for long-term display or 0 for no averaging 
	 * (parameter from Thode Azigram)
	 */
	public double secAvg = 0;  

	/**
	 * Bearing bias/correction (degrees?)
	 * (parameter from Thode Azigram)
	 */
	// 
	public double[] bearingCorrection = {0, 0}; 
	
	public float outputSampleRate = 6000;
	
	/**
	 * Apply gain (in dB) when displaying on the spectrogram. This is a hack
	 * to help the Azigram look nicer. It allows the user some discretion 
	 * to adjust how Azigram transparency/fade-to-black works. 
	 */
	public float displayGaindB = 0;

	/**
	 * Target percentile (0-100) used by the per-bin background/noise-floor
	 * tracker (see AzigramPercentileBackground) to distinguish real
	 * directional signal from background/incoherent demux noise for display
	 * fading. Lower values (e.g. 20-30) sit further below the bulk of the
	 * signal and are usually a better noise-floor estimate than the median
	 * (50), since they remain accurate even when real signal is present a
	 * meaningful fraction of the time. Replaces the original fixed dB
	 * threshold, which had to be re-tuned by hand for every recording since
	 * ambient noise varies by frequency, deployment, and season.
	 */
	public double backgroundPercentile = 25;

	/**
	 * How fast (in dB per second) the tracked background can rise or fall.
	 * Larger values adapt faster but are noisier; smaller values are more
	 * stable but slower to follow a genuinely changing noise floor. Tune
	 * this (along with backgroundPercentile) by trial and error against
	 * real data.
	 */
	public double backgroundStepDbPerSecond = 2;

	/**
	 * How many seconds of initial data the background tracker uses to snap
	 * to roughly the right starting level, rather than slowly crawling up
	 * from zero.
	 */
	public double backgroundRunInSeconds = 2;

	/**
	 * Below this many dB above the tracked background (see
	 * backgroundPercentile etc.), a cell is faded fully to the display's
	 * floor colour. Works in the same relative units as
	 * AzigramDataUnit.getSpectrogramAlpha() - NOT an absolute dB SPL value
	 * (unlike SpectrogramDisplay's own original hardcoded fade floor, which
	 * assumed one). Tune this by trial and error against real data.
	 */
	public double fadeFloorDb = 0;

	/**
	 * At or above this many dB above the tracked background, a cell is
	 * shown at full colour with no fading at all. Tune this (along with
	 * fadeFloorDb) by trial and error against real data.
	 */
	public double fadeThresholdDb = 15;

	/**
	 * How a cell's fade/transparency is decided: PERCENTILE uses the
	 * adaptive per-bin background tracker above (fadeFloorDb/fadeThresholdDb
	 * are then relative, "dB above tracked background"); ABSOLUTE instead
	 * uses a plain fixed dB re 1uPa threshold - useful when the user already
	 * knows their local ambient noise level (e.g. summer Southern Ocean
	 * conditions for Antarctic blue whales) and would rather set that
	 * directly than rely on the tracker.
	 */
	public enum BackgroundMode { PERCENTILE, ABSOLUTE }

	public BackgroundMode backgroundMode = BackgroundMode.PERCENTILE;

	/**
	 * Below this many dB re 1uPa, a cell is faded fully to the display's
	 * floor colour - only used when backgroundMode is ABSOLUTE. Separate
	 * from fadeFloorDb above since the two are different unit conventions
	 * (absolute dB SPL here vs relative dB-above-background there); keeping
	 * them as separate fields means switching modes doesn't silently reuse a
	 * number tuned for the other convention.
	 */
	public double absoluteFadeFloorDb = 70;

	/**
	 * At or above this many dB re 1uPa, a cell is shown at full colour with
	 * no fading at all - only used when backgroundMode is ABSOLUTE.
	 */
	public double absoluteFadeThresholdDb = 90;

	/**
	 * Fixes up the background/fade fields above if they're at their bare
	 * post-deserialisation default rather than a genuine value - because
	 * AzigramParameters is a plain Serializable class, Java's native
	 * deserialisation (used when loading settings from an older saved psfx
	 * that predates these fields) never runs the constructor, so the normal
	 * field initialisers above never execute; the fields are simply left at
	 * their bare Java default (0, or null for backgroundMode). Each check
	 * below detects a state that can only happen this way - never something
	 * this class's own dialog validation would allow to be saved
	 * deliberately - so its presence after a load reliably means that
	 * particular field (or group of fields) was never actually set. Called
	 * from AzigramControl.restoreSettings() after loading, so this applies
	 * regardless of whether it's a genuinely new module or an older saved
	 * settings blob.
	 */
	public void checkBackgroundDefaults() {
		AzigramParameters defaults = new AzigramParameters();
		if (fadeThresholdDb <= fadeFloorDb) {
			backgroundPercentile = defaults.backgroundPercentile;
			backgroundStepDbPerSecond = defaults.backgroundStepDbPerSecond;
			backgroundRunInSeconds = defaults.backgroundRunInSeconds;
			fadeFloorDb = defaults.fadeFloorDb;
			fadeThresholdDb = defaults.fadeThresholdDb;
		}
		if (backgroundMode == null) {
			backgroundMode = defaults.backgroundMode;
		}
		if (absoluteFadeThresholdDb <= absoluteFadeFloorDb) {
			absoluteFadeFloorDb = defaults.absoluteFadeFloorDb;
			absoluteFadeThresholdDb = defaults.absoluteFadeThresholdDb;
		}
	}
	
	@Override
	public PamParameterSet getParameterSet() {
		PamParameterSet ps = PamParameterSet.autoGenerate(this, ParameterSetType.DETECTOR);
		try {
			Field field = this.getClass().getDeclaredField("name");
			ps.put(new PrivatePamParameterData(this, field) {
				@Override
				public Object getData() throws IllegalArgumentException, IllegalAccessException {
					return name;
				}
			});
		} catch (NoSuchFieldException | SecurityException e) {
			e.printStackTrace();
		}
		return ps;
	}
	
	@Override
	public AzigramParameters clone() {
		try {
			return (AzigramParameters) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return null;
	}

	public float getOutputSampleRate() {

		return outputSampleRate;
	}

	/**
	 * Possible output-rates for the Azigram output. 
	 * @return
	 */
	public Float[] getOutputRateList() {
		// These all divide 48000 by a power of 2, so will keep FFT length a power of 2
		Float[] pow2rates = {6000f, 3000f, 1500f, 750f, 375f};

		// These include some rates that are more commonly used, but don't all
		// yield FFT lengths that are power of 2
		Float[] possibleRates = {8000f, 6000f, 4000f, 3000f, 2000f, 1500f, 1000f, 750f, 500f, 375f, 250f};
		return pow2rates;
	}

}
