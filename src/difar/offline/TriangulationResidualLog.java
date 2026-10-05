package difar.offline;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import Localiser.algorithms.genericLocaliser.Chi2TimeDelays;
import PamUtils.PamCalendar;
import PamUtils.PamUtils;
import PamguardMVC.PamDataUnit;
import difar.DIFARTargetMotionInformation;
import difar.DifarDataUnit;
import difar.DifarMatchSelector;
import difar.SonobuoyHistory;
import difar.SonobuoyRecord;
import difar.crossings.DifarCrossing;
import generalDatabase.DBControlUnit;
import pamMaths.PamVector;

/**
 * The residuals of every triangulation a rematch makes, one row per time
 * delay, written to a CSV file beside the database.
 * <p>
 * This is the evidence for choosing timing errors and limits. Each row gives
 * a delay's measured and predicted values, whether it was measured by
 * correlation, the separation of its two sonobuoys, how long each had been
 * deployed, and the bearing residual at each end. Plotted against separation
 * and against time since deployment, the residuals show how much of the timing
 * error comes from the measurement, and how much from sonobuoys that are not
 * where their records say, for example because they have drifted.
 */
public class TriangulationResidualLog {

	private static final String HEADER = "TriangulationUID,UTC,Species,Sonobuoys,Chi2PerDof,"
			+ "ChannelA,ChannelB,ClipUIDA,ClipUIDB,SonobuoyA,SonobuoyB,"
			+ "SeparationM,TravelTimeS,MeasuredDelayS,PredictedDelayS,ResidualS,DelayErrorS,Correlated,"
			+ "HoursDeployedA,HoursDeployedB,BearingResidualADeg,BearingResidualBDeg";

	private final List<String> rows = new ArrayList<>();

	private int triangulations;

	/**
	 * Add the delays of one triangulation.
	 * @param crossing the triangulation that was made.
	 * @param match the match it was made from, with the measurements it was
	 * fitted to.
	 * @param history the sonobuoy history, for names and deployment times; may
	 * be null.
	 */
	public void add(DifarCrossing crossing, DifarMatchSelector.Match match, SonobuoyHistory history) {
		if (crossing == null || match == null || match.getInfo() == null || match.getResult() == null
				|| match.getResult().getLatLong() == null) {
			return;
		}
		DIFARTargetMotionInformation info = match.getInfo();
		List<PamDataUnit> units = match.getUnits();
		ArrayList<Double> measured = row(info.getTimeDelays());
		ArrayList<Double> errors = row(info.getTimeDelayErrors());
		ArrayList<double[]> positions = row(info.getDelayHydrophonePositions());
		ArrayList<Boolean> correlated = info.getCorrelatedDelays();
		if (measured == null || errors == null || positions == null) {
			return;
		}
		double speedOfSound = info.getSpeedOfSound();
		PamVector source = info.latLongToMetres(match.getResult().getLatLong());
		double[] sourcePos = {source.getElement(0), source.getElement(1), 0};
		ArrayList<Double> predicted = row(Chi2TimeDelays.calcTimeDelays(sourcePos, info.getDelayHydrophonePositions(),
				speedOfSound));
		double[] bearingResiduals = bearingResiduals(info, source);

		String species = units.get(0) instanceof DifarDataUnit ? ((DifarDataUnit) units.get(0)).getSpeciesCode() : null;
		String common = String.format(Locale.ROOT, "%d,%s,%s,%d,%s", crossing.getUID(),
				PamCalendar.formatDBDateTime(crossing.getTimeMilliseconds(), true), text(species), units.size(),
				number(match.getChi2PerDegreeOfFreedom(), 3));

		ArrayList<Integer> first = PamUtils.indexM1(units.size());
		ArrayList<Integer> second = PamUtils.indexM2(units.size());
		for (int j = 0; j < first.size() && j < measured.size(); j++) {
			int a = first.get(j);
			int b = second.get(j);
			PamDataUnit unitA = units.get(a);
			PamDataUnit unitB = units.get(b);
			double[] pa = positions.get(a);
			double[] pb = positions.get(b);
			double separation = Math.hypot(pb[0] - pa[0], pb[1] - pa[1]);
			double residual = predicted == null || j >= predicted.size() ? Double.NaN : measured.get(j) - predicted.get(j);
			rows.add(String.format(Locale.ROOT, "%s,%d,%d,%d,%d,%s,%s,%.1f,%s,%s,%s,%s,%s,%b,%s,%s,%s,%s",
					common, channel(unitA), channel(unitB), unitA.getUID(), unitB.getUID(),
					text(name(history, unitA)), text(name(history, unitB)), separation,
					number(separation / speedOfSound, 4), number(measured.get(j), 4),
					number(predicted == null || j >= predicted.size() ? Double.NaN : predicted.get(j), 4),
					number(residual, 4), number(errors.get(j), 4),
					correlated != null && j < correlated.size() && correlated.get(j),
					number(hoursDeployed(history, unitA), 3), number(hoursDeployed(history, unitB), 3),
					number(bearingResiduals[a], 2), number(bearingResiduals[b], 2)));
		}
		triangulations++;
	}

	/**
	 * Write the rows to a file beside the database.
	 * @return the file written, or null if there was nothing to write or no
	 * single-file database to put it beside.
	 * @throws IOException if the file cannot be written.
	 */
	public File write() throws IOException {
		if (rows.isEmpty()) {
			return null;
		}
		DBControlUnit dbControl = DBControlUnit.findDatabaseControl();
		File database = dbControl == null || dbControl.getDatabaseSystem() == null ? null
				: new File(dbControl.getDatabaseSystem().getDatabaseName());
		if (database == null || !database.isFile()) {
			return null;
		}
		String stem = database.getName().replaceFirst("\\.[^.]*$", "");
		SimpleDateFormat stamp = new SimpleDateFormat("yyyyMMdd_HHmmss");
		stamp.setTimeZone(TimeZone.getTimeZone("UTC"));
		File file = new File(database.getParentFile(),
				stem + "_triangulation_residuals_" + stamp.format(new Date()) + ".csv");
		try (PrintWriter out = new PrintWriter(file, "UTF-8")) {
			out.println(HEADER);
			for (String row : rows) {
				out.println(row);
			}
		}
		return file;
	}

	/** @return how many triangulations were added. */
	public int getTriangulationCount() {
		return triangulations;
	}

	/** @return how many delays were added. */
	public int getDelayCount() {
		return rows.size();
	}

	/**
	 * @return the difference between each clip's bearing and the bearing from
	 * its sonobuoy to the fitted position, in degrees, by clip; NaN where a
	 * clip has no bearing.
	 */
	private static double[] bearingResiduals(DIFARTargetMotionInformation info, PamVector source) {
		List<PamDataUnit> units = info.getCurrentDetections();
		PamVector[] origins = info.getOrigins();
		double[] residuals = new double[units.size()];
		for (int i = 0; i < units.size(); i++) {
			residuals[i] = Double.NaN;
			if (origins == null || i >= origins.length || units.get(i).getLocalisation() == null) {
				continue;
			}
			double[] angles = units.get(i).getLocalisation().getAngles();
			if (angles == null || angles.length < 1) {
				continue;
			}
			double predicted = Math.toDegrees(Math.atan2(source.getElement(0) - origins[i].getElement(0),
					source.getElement(1) - origins[i].getElement(1)));
			double difference = (Math.toDegrees(angles[0]) - predicted) % 360.;
			if (difference > 180.) {
				difference -= 360.;
			}
			if (difference < -180.) {
				difference += 360.;
			}
			residuals[i] = difference;
		}
		return residuals;
	}

	private static double hoursDeployed(SonobuoyHistory history, PamDataUnit unit) {
		if (history == null) {
			return Double.NaN;
		}
		Long deployed = history.getDeploymentTime(channel(unit), unit.getTimeMilliseconds());
		return deployed == null ? Double.NaN : (unit.getTimeMilliseconds() - deployed) / 3600000.;
	}

	private static String name(SonobuoyHistory history, PamDataUnit unit) {
		if (history == null) {
			return null;
		}
		SonobuoyRecord record = history.getRecordAt(channel(unit), unit.getTimeMilliseconds());
		return record == null ? null : record.getName();
	}

	private static int channel(PamDataUnit unit) {
		return PamUtils.getSingleChannel(unit.getChannelBitmap());
	}

	private static <T> ArrayList<T> row(ArrayList<ArrayList<T>> rows) {
		return rows == null || rows.isEmpty() ? null : rows.get(0);
	}

	/** @return a number to the given decimals, or empty if it is NaN. */
	private static String number(double value, int decimals) {
		return Double.isNaN(value) || Double.isInfinite(value) ? "" : String.format(Locale.ROOT, "%." + decimals + "f", value);
	}

	/** @return text quoted for CSV, or empty if null. */
	private static String text(String value) {
		return value == null ? "" : "\"" + value.replace("\"", "\"\"") + "\"";
	}
}
