package AzigramFX;

import dataPlotsFX.spectrogramPlotFX.SpectrogramParamsFX;
import pamViewFX.fxNodes.utilsFX.ColourArray.ColourArrayType;

/**
 * SpectrogramParamsFX for the Azigram FX display. The Azigram module's output
 * is bearing angle in degrees (0-360), not a dB magnitude - see
 * Azigram.AzigramProcess#getRecommendedScaleMin()/Max(). The inherited
 * SpectrogramParamsFX defaults (amplitude scale {50,120}, i.e. dB-oriented)
 * would silently clamp almost the entire 0-360 degree range to a single flat
 * colour - indistinguishable from no data at all, which is exactly the "blank
 * display" reported at
 * https://bioacoustics.stackexchange.com/questions/1953. Setting sensible
 * defaults here means a user gets a correctly-scaled display immediately,
 * with no manual "Scales" tab step required.
 *
 * @author brian_mil
 */
public class AzigramParamsFX extends SpectrogramParamsFX {

	private static final long serialVersionUID = 1L;

	public AzigramParamsFX() {
		super();
		amplitudeLimitsSerial = new double[] {0, 360};
		maxAmplitudeLimits = new double[] {0, 360};
		// re-derive the live DoubleProperty[] from the serial values just set,
		// since PlotParams2D's field initialiser already ran (using the old
		// dB-oriented amplitudeLimitsSerial default) before this constructor body.
		createAmplitudeProperty();
		// HSV is circular, which suits circular bearing data - the same
		// recommendation the module's own help docs give for the Swing version.
		setColourMap(ColourArrayType.HSV);
	}

}
