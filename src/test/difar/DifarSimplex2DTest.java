package test.difar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;

import org.junit.jupiter.api.Test;

import GPS.GpsData;
import PamDetection.AbstractLocalisation;
import PamDetection.LocContents;
import PamUtils.LatLong;
import PamguardMVC.PamDataUnit;
import difar.DIFARTargetMotionInformation;
import difar.targetmotion.Simplex2D;
import difar.targetmotion.TargetMotionResult;
import pamMaths.PamVector;

/**
 * Baseline tests for DIFAR bearing-only localisation, before any change to
 * the fit. Buoys and sources are placed on a flat local grid in metres, and
 * each buoy reports the exact true bearing to the source.
 * <p>
 * The tests drive the real DIFARTargetMotionInformation and Simplex2D.
 * Only the data units are fakes, so no PamController or audio is needed.
 */
public class DifarSimplex2DTest {

	/** Reference position, in the Southern Ocean. */
	private static final LatLong REF = new LatLong(-60.0, 140.0);

	/** Bearing standard deviation used by the fakes, in degrees. */
	private static final double BEARING_SD_DEG = 5.0;

	/** Allowed position error for noise-free bearings, in metres. */
	private static final double POSITION_TOL_M = 50.0;

	@Test
	public void pairFindsSource() {
		double[][] buoys = {{0, 0}, {15000, 0}};
		double[] source = {5000, 8000};
		TargetMotionResult result = localise(buoys, source, new double[] {0, 0});
		assertNearSource(result, source);
		assertTrue(result.getChi2() < 1e-3, "chi2 for an exact pair should be near zero: " + result.getChi2());
	}

	@Test
	public void tripletFindsSource() {
		double[][] buoys = {{0, 0}, {15000, 0}, {7500, -12000}};
		double[] source = {5000, 8000};
		TargetMotionResult result = localise(buoys, source, new double[] {0, 0, 0});
		assertNearSource(result, source);
		assertTrue(result.getChi2() < 1e-3, "chi2 for exact bearings should be near zero: " + result.getChi2());
	}

	@Test
	public void tripletWithBadBearingHasLargerChi2() {
		double[][] buoys = {{0, 0}, {15000, 0}, {7500, -12000}};
		double[] source = {5000, 8000};
		TargetMotionResult exact = localise(buoys, source, new double[] {0, 0, 0});
		TargetMotionResult biased = localise(buoys, source, new double[] {0, 0, 15});
		assertTrue(biased.getChi2() > exact.getChi2() + 1.0,
				"a 15 degree error on one buoy should raise chi2: " + biased.getChi2());
	}

	/**
	 * Build fake DIFAR units for each buoy, then run the real localiser.
	 * @param buoys buoy positions in metres, as {x east, y north}
	 * @param source source position in metres
	 * @param biasDeg error added to each buoy's bearing, in degrees
	 * @return the single localisation result
	 */
	private TargetMotionResult localise(double[][] buoys, double[] source, double[] biasDeg) {
		ArrayList<PamDataUnit> units = new ArrayList<>();
		for (int i = 0; i < buoys.length; i++) {
			double dx = source[0] - buoys[i][0];
			double dy = source[1] - buoys[i][1];
			double bearingDeg = Math.toDegrees(Math.atan2(dx, dy)) + biasDeg[i];
			LatLong buoyLL = REF.addDistanceMeters(buoys[i][0], buoys[i][1]);
			units.add(new FakeBuoyUnit(1000L, i, buoyLL, bearingDeg));
		}
		DIFARTargetMotionInformation tmi = new DIFARTargetMotionInformation(null, units);
		Simplex2D simplex = new Simplex2D();
		simplex.setStartPoint(tmi.getMeanPosition());
		TargetMotionResult[] results = simplex.runModel(tmi);
		assertNotNull(results);
		assertEquals(1, results.length);
		assertNotNull(results[0], "optimisation failed");
		return results[0];
	}

	/** Check a result lies close to the source, measured from the first buoy's origin. */
	private void assertNearSource(TargetMotionResult result, double[] source) {
		LatLong ll = result.getLatLong();
		assertNotNull(ll);
		double x = REF.distanceToMetresX(ll);
		double y = REF.distanceToMetresY(ll);
		double err = Math.hypot(x - source[0], y - source[1]);
		assertTrue(err < POSITION_TOL_M, String.format(
				"expected (%.0f, %.0f), got (%.0f, %.0f), error %.0f m", source[0], source[1], x, y, err));
	}

	/** A data unit on one buoy, with a fixed position and one true bearing. */
	private static class FakeBuoyUnit extends PamDataUnit {

		private final GpsData origin;

		FakeBuoyUnit(long timeMillis, int channel, LatLong position, double bearingDeg) {
			super(timeMillis);
			setChannelBitmap(1 << channel);
			origin = new GpsData(position);
			setLocalisation(new FakeBearing(this, bearingDeg));
		}

		@Override
		public GpsData getOriginLatLong(boolean recalculate) {
			return origin;
		}
	}

	/** A single true bearing with a fixed error, shaped like DifarLocalisation. */
	private static class FakeBearing extends AbstractLocalisation {

		private final double bearingDeg;

		FakeBearing(PamDataUnit unit, double bearingDeg) {
			super(unit, LocContents.HAS_BEARING, 0);
			this.bearingDeg = bearingDeg;
		}

		@Override
		public boolean bearingAmbiguity() {
			return false;
		}

		@Override
		public double[] getAngles() {
			return new double[] {Math.toRadians(bearingDeg)};
		}

		@Override
		public double[] getAngleErrors() {
			return new double[] {Math.toRadians(BEARING_SD_DEG)};
		}

		@Override
		public PamVector[] getWorldVectors() {
			double radians = Math.toRadians(90 - bearingDeg);
			return new PamVector[] {new PamVector(Math.cos(radians), Math.sin(radians), 0)};
		}
	}
}
