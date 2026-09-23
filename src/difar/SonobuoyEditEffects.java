package difar;

import java.util.List;

import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;

/**
 * What changing one buoy record does to the detections made on it.
 * <p>
 * Bearings are worked out when they are drawn, so they follow a new calibration
 * by themselves. Triangulations were worked out once and saved, so they are
 * left describing a calibration that no longer exists. This works out how many
 * of each are affected, over the whole time the record is in force, so the user
 * can be told before the change is made.
 */
public class SonobuoyEditEffects {

	private final SonobuoyRecord record;

	private final long startTime;

	private final Long endTime;

	private int bearings;

	private int triangulations;

	private boolean countsAreOfLoadedData;

	/**
	 * Work out what changing a record affects.
	 * @param record the record being changed.
	 * @param history the sonobuoy history.
	 * @param detections the processed DIFAR detections that are loaded.
	 * @param loadedStart start of the loaded period.
	 * @param loadedEnd end of the loaded period.
	 */
	public SonobuoyEditEffects(SonobuoyRecord record, SonobuoyHistory history,
			List<PamDataUnit> detections, long loadedStart, long loadedEnd) {
		this.record = record;
		this.startTime = record.getTimeMillis();
		this.endTime = history.getInForceUntil(record);
		count(detections);
		countsAreOfLoadedData = loadedStart > startTime
				|| (endTime != null && loadedEnd < endTime);
	}

	private void count(List<PamDataUnit> detections) {
		if (detections == null) {
			return;
		}
		for (PamDataUnit unit : detections) {
			if (!(unit instanceof DifarDataUnit) || !covers(unit)) {
				continue;
			}
			bearings++;
			if (((DifarDataUnit) unit).getDifarCrossing() != null) {
				triangulations++;
			}
		}
	}

	/**
	 * @param unit a detection.
	 * @return true if it was made on the record's buoy while the record was in
	 * force.
	 */
	public boolean covers(PamDataUnit unit) {
		if (PamUtils.getSingleChannel(unit.getChannelBitmap()) != record.getChannel()) {
			return false;
		}
		long time = unit.getTimeMilliseconds();
		return time >= startTime && (endTime == null || time < endTime);
	}

	/**
	 * @return start of the time the record is in force.
	 */
	public long getStartTime() {
		return startTime;
	}

	/**
	 * @return end of the time the record is in force, or null if it is the
	 * buoy in force on its channel.
	 */
	public Long getEndTime() {
		return endTime;
	}

	/**
	 * @return end of the time to reprocess, taking an open ended record as
	 * running to the end of the data.
	 */
	public long getEndTimeOrLatest() {
		return endTime == null ? Long.MAX_VALUE : endTime;
	}

	/**
	 * @return how many detections on this buoy have their bearing changed.
	 */
	public int getBearings() {
		return bearings;
	}

	/**
	 * @return how many saved triangulations are left out of date.
	 */
	public int getTriangulations() {
		return triangulations;
	}

	/**
	 * @return true if anything downstream is affected at all.
	 */
	public boolean isAnythingAffected() {
		return bearings > 0 || triangulations > 0;
	}

	/**
	 * What the user is told before the change is made.
	 * @param viewer true in viewer mode, where the change can be carried
	 * through to the saved data.
	 * @return the message for the dialog. What happens to the triangulations is
	 * chosen in the dialog itself.
	 */
	public String getMessage(boolean viewer) {
		StringBuilder message = new StringBuilder();
		message.append(String.format("<html>Buoy %s on channel %d, deployed %s.<p><p>",
				record.getName(), record.getChannel(), PamCalendar.formatDateTime(startTime)));
		if (endTime == null) {
			message.append(String.format("This buoy applies to detections from %s onwards.<p><p>",
					PamCalendar.formatDateTime(startTime)));
		} else {
			message.append(String.format("This buoy applies to detections from %s to %s.<p><p>",
					PamCalendar.formatDateTime(startTime), PamCalendar.formatDateTime(endTime)));
		}
		message.append(String.format("%d bearings will be updated.<p>", bearings));
		if (triangulations == 0) {
			message.append("No saved triangulations are affected.");
		} else {
			message.append(String.format("%d saved triangulations no longer describe this buoy.",
					triangulations));
			if (!viewer) {
				message.append("<p><p>Only those still in memory can be changed. Detections "
						+ "already written to file keep their old triangulation until the data "
						+ "are reprocessed in Viewer mode, from DIFAR offline tasks.");
			}
		}
		if (countsAreOfLoadedData) {
			message.append("<p><p>These counts are of the data loaded now. Any others in the same "
					+ "period are treated the same way.");
		}
		message.append("</html>");
		return message.toString();
	}
}
