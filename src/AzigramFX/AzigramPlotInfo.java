package AzigramFX;

import PamController.PamControlledUnit;
import PamController.PamController;
import PamguardMVC.DataBlock2D;
import dataPlotsFX.data.TDDataProviderFX;
import dataPlotsFX.layout.TDGraphFX;
import dataPlotsFX.spectrogramPlotFX.FFTPlotInfo;
import dataPlotsFX.spectrogramPlotFX.SpectrogramParamsFX;
import pamViewFX.fxNodes.utilsFX.ColourArray.ColourArrayType;

/**
 * FX plot info for the Azigram module's bearing-angle output. Extends
 * FFTPlotInfo to reuse its frequency-axis binding, settings pane, and
 * serialisation logic wholesale - the only things that differ for Azigram
 * are the default scale/colour map (see AzigramParamsFX) and how a global
 * medium change is handled.
 *
 * @author brian_mil
 */
public class AzigramPlotInfo extends FFTPlotInfo {

	public AzigramPlotInfo(TDDataProviderFX tdDataProvider, TDGraphFX tdGraph, PamControlledUnit azigramControl,
			DataBlock2D azigramDataBlock) {
		super(tdDataProvider, tdGraph, azigramControl, azigramDataBlock);
		// SpectrogramControlPane (the settings pane FFTPlotInfo builds above, in
		// super()) keeps its own hardcoded RED default, entirely independent of
		// whatever colour map AzigramParamsFX was constructed with - for a
		// normal FFT plot this is invisible, since PlotParams2D's own default
		// colour map also happens to be RED, but it means our HSV default here
		// never actually reaches the settings pane (or, via its colour-box
		// change-listener, the live render colours) without this explicit push.
		getSpectrogramControlPane().setColourArrayType(ColourArrayType.HSV);
	}

	@Override
	protected SpectrogramParamsFX createSpectrogramParams() {
		return new AzigramParamsFX();
	}

	@Override
	public void notifyChange(int changeType) {
		if (changeType == PamController.GLOBAL_MEDIUM_UPDATE) {
			// FFTPlotInfo's handling of this resets the amplitude scale to
			// PamController...getDefaultdBHzScales() - a dB range. Bearing angle
			// in degrees has nothing to do with the acoustic medium (air/water),
			// so ignore this rather than let it silently overwrite our degree
			// scale with a dB one.
			return;
		}
		super.notifyChange(changeType);
	}

}

