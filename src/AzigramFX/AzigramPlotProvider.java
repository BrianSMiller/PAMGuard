package AzigramFX;

import PamController.PamControlledUnit;
import PamguardMVC.PamDataBlock;
import dataPlotsFX.data.TDDataInfoFX;
import dataPlotsFX.data.TDDataProviderFX;
import dataPlotsFX.layout.TDGraphFX;
import fftManager.FFTDataBlock;

/**
 * Registers the Azigram module's output as its own selectable FX plot type
 * ("Azigram Display"), distinct from the generic FFT/Spectrogram Display FX
 * plot type. Without this, a user has to add a generic Spectrogram Display
 * and manually point it at Azigram's output block, then - unless they also
 * happen to find and correctly set the "Scales" tab to 0-360 - see a blank
 * or near-blank display, exactly as reported at
 * https://bioacoustics.stackexchange.com/questions/1953.
 *
 * @author brian_mil
 */
public class AzigramPlotProvider extends TDDataProviderFX {

	private final PamControlledUnit azigramControl;

	public AzigramPlotProvider(PamControlledUnit azigramControl, PamDataBlock parentDataBlock) {
		super(parentDataBlock);
		this.azigramControl = azigramControl;
	}

	@Override
	public TDDataInfoFX createDataInfo(TDGraphFX tdGraph) {
		return new AzigramPlotInfo(this, tdGraph, azigramControl, (FFTDataBlock) getDataBlock());
	}

}
