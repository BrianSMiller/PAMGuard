package difar.dataSelector;

import java.util.ArrayList;

import difar.DifarControl;
import difar.crossings.DifarCrossingDataBlock;
import generalDatabase.lookupTables.LookupList;
import PamguardMVC.PamDataBlock;
import PamguardMVC.PamDataUnit;
import PamguardMVC.dataSelector.CompoundDataSelector;
import PamguardMVC.dataSelector.DataSelectParams;
import PamguardMVC.dataSelector.DataSelector;
import PamguardMVC.dataSelector.DataSelectorCreator;
import PamguardMVC.dataSelector.SuperDetDataSelector;

public class DifarDataSelectCreator extends DataSelectorCreator {

	private DifarControl difarControl;

	public DifarDataSelectCreator(DifarControl difarControl, PamDataBlock pamDataBlock) {
		super(pamDataBlock);
		this.difarControl = difarControl;
	}

	@Override
	public DataSelector createDataSelector(String selectorName,
			boolean allowScores, String selectorType) {
		return new DifarDataSelector(difarControl, getPamDataBlock(), selectorName, allowScores);
	}

	/**
	 * PAMGuard adds a section to the dialog for each grouping a DIFAR clip can
	 * belong to. The section for triangulations is left out: the DIFAR clip
	 * filter offers the same choice in plain words, and uses the display's
	 * own triangulation filter, so bearings and triangulations agree. Sections
	 * for other groupings are kept.
	 */
	@Override
	protected DataSelector addSuperDetectionOptions(DataSelector ds, String selectorName,
			boolean allowScores, String selectorType) {
		DataSelector withSections = super.addSuperDetectionOptions(ds, selectorName, allowScores, selectorType);
		if (withSections == ds) {
			return ds;
		}
		if (isTriangulationSection(withSections)) {
			return ds;
		}
		if (!(withSections instanceof CompoundDataSelector)) {
			return withSections;
		}
		ArrayList<DataSelector> sections = ((CompoundDataSelector) withSections).getSelectorList();
		sections.removeIf(DifarDataSelectCreator::isTriangulationSection);
		return sections.size() == 1 ? sections.get(0) : withSections;
	}

	/**
	 * @return true if a selector is PAMGuard's section for DIFAR triangulations.
	 */
	private static boolean isTriangulationSection(DataSelector selector) {
		return selector instanceof SuperDetDataSelector
				&& selector.getPamDataBlock() instanceof DifarCrossingDataBlock;
	}

	@Override
	public DataSelectParams createNewParams(String name) {
		LookupList speciesList = difarControl.getDifarParameters().getSpeciesList(difarControl);

		int channelBitmap = 0;
		PamDataBlock<PamDataUnit> sourceBlock = difarControl.getDifarProcess().getSourceDataBlock();
		if (sourceBlock != null) {
			channelBitmap = sourceBlock.getChannelMap();
		}
		return new DifarSelectParameters(speciesList, channelBitmap);
	}

}
