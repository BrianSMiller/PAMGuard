package difar.display;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamView.PamColors.PamColor;
import PamView.panel.PamPanel;
import PamguardMVC.PamDataUnit;
import difar.DIFARMessage;
import difar.DifarControl;
import difar.DifarMatchSelector;

/**
 * Shows which detections on other buoys could have been the same call as the
 * one being looked at, and what happened to each of them.
 * <p>
 * Every combination the matching tried is listed, best first. The one that was
 * used is highlighted. The rest carry the reason they were not used, which is
 * usually that the arrival times do not fit where the bearings cross.
 * <p>
 * The table shows the matching as it would happen now, with the settings now in
 * force. It is not a record of what happened when the data were collected.
 */
public class DifarMatchPanel extends PamPanel implements DIFARDisplayUnit {

	private static final long serialVersionUID = 1L;

	private final DifarControl difarControl;

	private final MatchTableModel tableModel = new MatchTableModel();

	private final JTable table = new JTable(tableModel);

	private final JLabel header = new JLabel(" ");

	public DifarMatchPanel(DifarControl difarControl) {
		super(PamColor.BORDER);
		this.difarControl = difarControl;
		setLayout(new BorderLayout());
		setBorder(BorderFactory.createTitledBorder("Match candidates"));

		header.setBorder(BorderFactory.createEmptyBorder(2, 4, 2, 4));
		add(BorderLayout.NORTH, header);

		table.setDefaultRenderer(Object.class, new MatchCellRenderer());
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		add(BorderLayout.CENTER, new JScrollPane(table));
	}

	@Override
	public String getName() {
		return difarControl.getUnitName() + " match candidates";
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public int difarNotification(DIFARMessage difarMessage) {
		/*
		 * A unit is matched as part of processing it, so its candidates are
		 * ready once the demultiplexing has finished. That covers both clicking
		 * a clip in viewer mode and processing one normally.
		 */
		if (difarMessage.message == DIFARMessage.DemuxComplete) {
			showUnit(difarMessage.difarDataUnit);
		}
		return 0;
	}

	/**
	 * Show the candidates for a detection.
	 * @param unit the detection, or null to clear the table.
	 */
	public void showUnit(PamDataUnit unit) {
		List<DifarMatchSelector.Match> matches = unit == null
				? null : difarControl.getDifarProcess().getMatchLog().get(unit);
		SwingUtilities.invokeLater(() -> {
			if (unit == null) {
				header.setText(" ");
			} else {
				header.setText(String.format("%s on channel %d, %d candidate groups",
						PamCalendar.formatTime(unit.getTimeMilliseconds(), true),
						PamUtils.getSingleChannel(unit.getChannelBitmap()),
						matches == null ? 0 : matches.size()));
			}
			tableModel.setMatches(matches);
		});
	}

	/** Rows are candidate groups, best first. */
	private class MatchTableModel extends AbstractTableModel {

		private static final long serialVersionUID = 1L;

		private final String[] columns = {"Buoys", "Other detections", "Bearing (deg)",
				"Timing (s)", "Chi2/dof", "Status"};

		private List<DifarMatchSelector.Match> matches;

		void setMatches(List<DifarMatchSelector.Match> matches) {
			this.matches = matches;
			fireTableDataChanged();
		}

		DifarMatchSelector.Match getMatch(int row) {
			if (matches == null || row < 0 || row >= matches.size()) {
				return null;
			}
			return matches.get(row);
		}

		@Override
		public int getRowCount() {
			return matches == null ? 0 : matches.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			DifarMatchSelector.Match match = getMatch(row);
			if (match == null) {
				return null;
			}
			switch (column) {
			case 0:
				return match.getUnits().size();
			case 1:
				return otherDetections(match);
			case 2:
				return format(match.getResiduals().getMaxBearingErrorDegrees(), 1);
			case 3:
				return format(match.getResiduals().getMaxTimeDelayErrorSeconds(), 2);
			case 4:
				return format(match.getChi2PerDegreeOfFreedom(), 2);
			case 5:
				return match.isAccepted() ? "used" : match.getRejectReason();
			}
			return null;
		}

		/** @return the other detections of a group, as channel and time. */
		private String otherDetections(DifarMatchSelector.Match match) {
			StringBuilder text = new StringBuilder();
			for (int i = 1; i < match.getUnits().size(); i++) {
				PamDataUnit unit = match.getUnits().get(i);
				if (text.length() > 0) {
					text.append(", ");
				}
				text.append(String.format("ch%d %s",
						PamUtils.getSingleChannel(unit.getChannelBitmap()),
						PamCalendar.formatTime(unit.getTimeMilliseconds(), true)));
			}
			return text.toString();
		}

		private String format(double value, int decimals) {
			if (Double.isNaN(value)) {
				return "-";
			}
			return String.format("%." + decimals + "f", value);
		}
	}

	/**
	 * Marks the group that was used in bold, so it stands out. Colours are left
	 * to the table, which follows whatever look and colour scheme is in use.
	 */
	private class MatchCellRenderer extends DefaultTableCellRenderer {

		private static final long serialVersionUID = 1L;

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value,
				boolean isSelected, boolean hasFocus, int row, int column) {
			Component component = super.getTableCellRendererComponent(table, value,
					isSelected, hasFocus, row, column);
			DifarMatchSelector.Match match = tableModel.getMatch(table.convertRowIndexToModel(row));
			boolean used = match != null && match.isAccepted();
			component.setFont(component.getFont().deriveFont(used ? Font.BOLD : Font.PLAIN));
			return component;
		}
	}
}
