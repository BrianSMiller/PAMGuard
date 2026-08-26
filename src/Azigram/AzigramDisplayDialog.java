package Azigram;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.SpinnerNumberModel;
import javax.swing.border.TitledBorder;

import Azigram.AzigramParameters.BackgroundMode;
import PamView.dialog.GroupedSourcePanel;
import PamView.dialog.PamDialog;
import PamView.dialog.PamLabel;
import PamView.panel.PamPanel;
import PamguardMVC.PamDataBlock;
import fftManager.FFTDataBlock;
import fftManager.FFTDataUnit;
import noiseMonitor.ResolutionPanel;


public class AzigramDisplayDialog extends PamDialog {
	
	private static AzigramDisplayDialog singleInstance;
	
	private AzigramControl azigramControl;
	
	private AzigramParameters azigramParameters;
	
	private JComboBox<Float> sampleRateCombo;
	Float[] outputRates;
	
//	private SourcePanel sourcePanel;
	private GroupedSourcePanel sourcePanel;
	
	private ResolutionPanel resolutionPanel = new ResolutionPanel();

	private JRadioButton percentileModeButton;
	private JRadioButton absoluteModeButton;

	private JSpinner percentileSpinner;
	private JSpinner stepDbPerSecondSpinner;
	private JSpinner runInSecondsSpinner;
	private JSpinner fadeFloorDbSpinner;
	private JSpinner fadeThresholdDbSpinner;

	private JSpinner absoluteFadeFloorDbSpinner;
	private JSpinner absoluteFadeThresholdDbSpinner;
	
	public AzigramDisplayDialog(Window parentFrame, AzigramControl azigramControl) {
		super(parentFrame, "Azigram Settings", true);
		this.azigramControl = azigramControl;
		JPanel p = new JPanel(new BorderLayout());
//		p.setBorder(new TitledBorder("Input"));
		sourcePanel = new GroupedSourcePanel(this, "Data Source", FFTDataUnit.class, true, true, false);
		p.add(BorderLayout.NORTH, sourcePanel.getPanel());
//		this.add(p);
		
		outputRates = azigramControl.azigramParameters.getOutputRateList();
		sampleRateCombo = new JComboBox<Float>(outputRates);
		JPanel outputPanel = new PamPanel();
		outputPanel.setBorder(new TitledBorder("Output") );
		outputPanel.setLayout(new BoxLayout(outputPanel,BoxLayout.Y_AXIS));

		PamPanel sampleRatePanel = new PamPanel(new FlowLayout());
		sampleRatePanel.add(new PamLabel("Sample Rate:"));
		sampleRatePanel.add(sampleRateCombo);
		sampleRatePanel.add(new PamLabel("Hz"));
		String tooltip = "<HTML><i>FFT Length</i> and <i>hop</i> are <br>controlled by the Source FFT module.</HTML>";
		
		outputPanel.add(BorderLayout.NORTH,sampleRatePanel);
		outputPanel.add(BorderLayout.CENTER,resolutionPanel.getPanel());

		sampleRatePanel.setToolTipText(tooltip);
		sampleRateCombo.setToolTipText(tooltip);
		resolutionPanel.getPanel().setToolTipText(tooltip);
		
		sampleRateCombo.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				FFTDataBlock inputBlock = (FFTDataBlock) azigramControl.azigramProcess.getInputDataBlock();
				float outputSampleRate = (float) sampleRateCombo.getSelectedItem();	
				int decimateFactor = (int) (inputBlock.getSampleRate()/outputSampleRate);
				resolutionPanel.setParams(outputSampleRate, inputBlock.getFftLength()/decimateFactor,
						inputBlock.getFftHop()/decimateFactor);
			}
			
		});
		
		p.add(BorderLayout.CENTER,outputPanel);
		JTabbedPane tabbedPane = new JTabbedPane();
		tabbedPane.add("Data", p);
		tabbedPane.add("Background", createBackgroundPanel());
		setDialogComponent(tabbedPane);
		AzigramPlugin azPlug = new AzigramPlugin();
		
		// Can't for the life of me figure out how to reference help for a 
		// plugin, so just pick the next nearest thing
		setHelpPoint("sound_processing.DifarAudio.docs.directionalAudio");
		
	}

	/**
	 * Builds the "Background" tab: a choice between the adaptive per-bin
	 * percentile background tracker (AzigramPercentileBackground) and a
	 * plain fixed absolute dB re 1uPa threshold - useful when the user
	 * already knows their local ambient noise level and would rather set
	 * that directly - plus the fade floor/threshold controls for whichever
	 * mode is selected. These all feed both the Swing and FX displays
	 * identically, since they're read straight off AzigramParameters by
	 * AzigramProcess - not something either display owns separately.
	 */
	private JPanel createBackgroundPanel() {
		JPanel panel = new JPanel(new GridBagLayout());
		panel.setBorder(new TitledBorder("Background / Fade"));

		percentileModeButton = new JRadioButton("Adaptive (percentile tracker)");
		absoluteModeButton = new JRadioButton("Fixed (absolute dB re 1\u00b5Pa)");
		ButtonGroup modeGroup = new ButtonGroup();
		modeGroup.add(percentileModeButton);
		modeGroup.add(absoluteModeButton);
		percentileModeButton.setToolTipText("<HTML>Tracks a percentile of each frequency bin's magnitude over time,<br>"
				+ "adapting automatically to local noise conditions.</HTML>");
		absoluteModeButton.setToolTipText("<HTML>Use a plain fixed dB re 1\u00b5Pa threshold instead - useful if you<br>"
				+ "already know your local ambient noise level (e.g. summer Southern<br>"
				+ "Ocean conditions) and would rather set that directly.</HTML>");

		percentileSpinner = new JSpinner(new SpinnerNumberModel(25., 0., 100., 1.));
		stepDbPerSecondSpinner = new JSpinner(new SpinnerNumberModel(2., 0.01, 50., 0.1));
		runInSecondsSpinner = new JSpinner(new SpinnerNumberModel(2., 0.1, 60., 0.5));
		fadeFloorDbSpinner = new JSpinner(new SpinnerNumberModel(0., -40., 100., 1.));
		fadeThresholdDbSpinner = new JSpinner(new SpinnerNumberModel(15., -40., 150., 1.));

		absoluteFadeFloorDbSpinner = new JSpinner(new SpinnerNumberModel(70., -40., 200., 1.));
		absoluteFadeThresholdDbSpinner = new JSpinner(new SpinnerNumberModel(90., -40., 250., 1.));

		percentileSpinner.setToolTipText("<HTML>Target percentile (0-100) the background tracker follows.<br>"
				+ "Lower values (e.g. 20-30) sit further below the bulk of the signal and<br>"
				+ "are usually a better noise-floor estimate than the median (50), since<br>"
				+ "they stay accurate even while real signal is present some of the time -<br>"
				+ "though a signal that sweeps quickly through each frequency bin (e.g. a<br>"
				+ "fast chirp) may actually want a HIGHER percentile, since any one bin<br>"
				+ "sees real signal only briefly. Tune this against real data.</HTML>");
		stepDbPerSecondSpinner.setToolTipText("<HTML>How fast (dB/second) the tracked background can rise or fall.<br>"
				+ "Larger = faster-adapting but noisier; smaller = slower but more stable.</HTML>");
		runInSecondsSpinner.setToolTipText("<HTML>How many seconds of initial data the tracker uses to snap to<br>"
				+ "roughly the right starting level, rather than slowly crawling up from zero.</HTML>");
		fadeFloorDbSpinner.setToolTipText("<HTML>Below this many dB above the tracked background, a cell fades<br>"
				+ "fully to the display's floor colour.</HTML>");
		fadeThresholdDbSpinner.setToolTipText("<HTML>At or above this many dB above the tracked background, a cell<br>"
				+ "is shown at full colour with no fading at all.</HTML>");
		absoluteFadeFloorDbSpinner.setToolTipText("<HTML>Below this many dB re 1\u00b5Pa, a cell fades fully to the<br>"
				+ "display's floor colour.</HTML>");
		absoluteFadeThresholdDbSpinner.setToolTipText("<HTML>At or above this many dB re 1\u00b5Pa, a cell is shown at<br>"
				+ "full colour with no fading at all.</HTML>");

		ActionListener modeListener = new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				updateModeEnabled();
			}
		};
		percentileModeButton.addActionListener(modeListener);
		absoluteModeButton.addActionListener(modeListener);

		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(3, 3, 3, 3);
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 3;
		panel.add(percentileModeButton, c);
		c.gridy++;
		addSpinnerRow(panel, c, "Background percentile:", percentileSpinner, "%");
		addSpinnerRow(panel, c, "Background step rate:", stepDbPerSecondSpinner, "dB/sec");
		addSpinnerRow(panel, c, "Background run-in:", runInSecondsSpinner, "sec");
		addSpinnerRow(panel, c, "Fade floor:", fadeFloorDbSpinner, "dB above background");
		addSpinnerRow(panel, c, "Fade threshold:", fadeThresholdDbSpinner, "dB above background");

		c.gridx = 0;
		c.gridwidth = 3;
		panel.add(absoluteModeButton, c);
		c.gridy++;
		addSpinnerRow(panel, c, "Fade floor:", absoluteFadeFloorDbSpinner, "dB re 1\u00b5Pa");
		addSpinnerRow(panel, c, "Fade threshold:", absoluteFadeThresholdDbSpinner, "dB re 1\u00b5Pa");

		return panel;
	}

	/**
	 * Enables only the spinner group belonging to whichever mode radio
	 * button is currently selected, so it's visually obvious which controls
	 * are actually in effect.
	 */
	private void updateModeEnabled() {
		boolean percentileMode = percentileModeButton.isSelected();
		percentileSpinner.setEnabled(percentileMode);
		stepDbPerSecondSpinner.setEnabled(percentileMode);
		runInSecondsSpinner.setEnabled(percentileMode);
		fadeFloorDbSpinner.setEnabled(percentileMode);
		fadeThresholdDbSpinner.setEnabled(percentileMode);
		absoluteFadeFloorDbSpinner.setEnabled(!percentileMode);
		absoluteFadeThresholdDbSpinner.setEnabled(!percentileMode);
	}

	/**
	 * Adds one label/spinner/unit row to a GridBagLayout panel, and advances
	 * c.gridy ready for the next row. Restores gridwidth to 1 for the row's
	 * three cells, since callers may have just used a wider gridwidth for a
	 * full-width row (e.g. a section heading) above.
	 */
	private void addSpinnerRow(JPanel panel, GridBagConstraints c, String label, JSpinner spinner, String unit) {
		c.gridwidth = 1;
		c.gridx = 0;
		panel.add(new PamLabel(label), c);
		c.gridx = 1;
		panel.add(spinner, c);
		c.gridx = 2;
		panel.add(new PamLabel(unit), c);
		c.gridy++;
	}


	@Override
	public boolean getParams() {
		PamDataBlock dataSource = sourcePanel.getSource();
		if (dataSource != null) {
			sourcePanel.getParams(azigramParameters.dataSource);
		}
		
		try{
			azigramParameters.outputSampleRate = (float) sampleRateCombo.getSelectedItem();
		}catch(Exception e ){
			
			return showWarning("Parameter problem: sample rate ");
		}

		double percentile = (Double) percentileSpinner.getValue();
		double stepDbPerSecond = (Double) stepDbPerSecondSpinner.getValue();
		double runInSeconds = (Double) runInSecondsSpinner.getValue();
		double fadeFloorDb = (Double) fadeFloorDbSpinner.getValue();
		double fadeThresholdDb = (Double) fadeThresholdDbSpinner.getValue();
		double absoluteFadeFloorDb = (Double) absoluteFadeFloorDbSpinner.getValue();
		double absoluteFadeThresholdDb = (Double) absoluteFadeThresholdDbSpinner.getValue();

		if (fadeThresholdDb <= fadeFloorDb) {
			return showWarning("Fade threshold must be greater than the fade floor");
		}
		if (absoluteFadeThresholdDb <= absoluteFadeFloorDb) {
			return showWarning("Fade threshold must be greater than the fade floor");
		}

		azigramParameters.backgroundMode = percentileModeButton.isSelected() ?
				BackgroundMode.PERCENTILE : BackgroundMode.ABSOLUTE;
		azigramParameters.backgroundPercentile = percentile;
		azigramParameters.backgroundStepDbPerSecond = stepDbPerSecond;
		azigramParameters.backgroundRunInSeconds = runInSeconds;
		azigramParameters.fadeFloorDb = fadeFloorDb;
		azigramParameters.fadeThresholdDb = fadeThresholdDb;
		azigramParameters.absoluteFadeFloorDb = absoluteFadeFloorDb;
		azigramParameters.absoluteFadeThresholdDb = absoluteFadeThresholdDb;

		return true;
	}
	
	private void setParams() {
		sourcePanel.setParams(azigramParameters.dataSource);
		
		sampleRateCombo.setSelectedItem(azigramParameters.outputSampleRate);			
		
		FFTDataBlock datablock = azigramControl.azigramProcess.getOutputDataBlock();
		if (datablock == null) {
			resolutionPanel.setParams(null);
		}
		else {
			resolutionPanel.setParams(datablock.getSampleRate(), datablock.getFftLength(),
					datablock.getFftHop());
		}

		if (azigramParameters.backgroundMode == BackgroundMode.ABSOLUTE) {
			absoluteModeButton.setSelected(true);
		}
		else {
			percentileModeButton.setSelected(true);
		}
		percentileSpinner.setValue(azigramParameters.backgroundPercentile);
		stepDbPerSecondSpinner.setValue(azigramParameters.backgroundStepDbPerSecond);
		runInSecondsSpinner.setValue(azigramParameters.backgroundRunInSeconds);
		fadeFloorDbSpinner.setValue(azigramParameters.fadeFloorDb);
		fadeThresholdDbSpinner.setValue(azigramParameters.fadeThresholdDb);
		absoluteFadeFloorDbSpinner.setValue(azigramParameters.absoluteFadeFloorDb);
		absoluteFadeThresholdDbSpinner.setValue(azigramParameters.absoluteFadeThresholdDb);
		updateModeEnabled();
		
	}

	@Override
	public void cancelButtonPressed() {
		azigramParameters = null;
	}

	@Override
	public void restoreDefaultSettings() {
		PamDataBlock dataSource = sourcePanel.getSource();
		if (dataSource == null) {
			return;
		}
		AzigramParameters dummyParams = new AzigramParameters();
//		azigramParameters.dataSourceName = dataSource.getDataName();
		azigramParameters = dummyParams;
		setParams();

	}


	public static AzigramParameters showDialog(AzigramControl azigramControl2, Frame parentFrame) {
		if (singleInstance == null || azigramControl2 != singleInstance.azigramControl ||
				parentFrame != singleInstance.getOwner()) {
			singleInstance = new AzigramDisplayDialog(parentFrame, azigramControl2);
		}
		singleInstance.azigramParameters = azigramControl2.azigramParameters;
		singleInstance.setParams();
		singleInstance.setVisible(true);
		singleInstance.azigramControl.azigramProcess.setupProcess();
		return singleInstance.azigramParameters;
	}




}
