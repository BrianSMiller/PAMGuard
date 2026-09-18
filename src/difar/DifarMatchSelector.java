package difar;

import java.util.ArrayList;
import java.util.List;

import PamguardMVC.PamDataUnit;
import difar.targetmotion.Simplex2D;
import difar.targetmotion.TargetMotionResult;

/**
 * Chooses which detections on other buoys are the same call as a given one.
 * <p>
 * Each buoy may have several detections that could be the call: same species,
 * overlapping in frequency, and close enough in time given how far apart the
 * buoys are. This class localises every combination of them, throws away the
 * ones whose residuals are too large, and returns the best of what is left.
 * <p>
 * The best is the largest group that passes, with ties broken by chi2 per
 * degree of freedom. A buoy therefore joins a group only if it is consistent
 * with it, and among equally sized groups the closer fit wins.
 * <p>
 * Ranking groups of different sizes by any goodness-of-fit measure, chi2 per
 * degree of freedom included, does not work, because a pair and a triplet are
 * fitted to different measurements. A well marked pair fits almost perfectly,
 * so such a rule would prefer pairs over triplets. The principled answer is the
 * one used for data association in multi target tracking: compare the
 * likelihood of the measurements if they came from one source against the
 * likelihood if they came from separate sources. Every detection then
 * contributes its own factor, so groups of different sizes can be compared.
 * That is worth doing, and is what this rule approximates.
 * <p>
 * This class does not read data blocks, so its behaviour can be tested with
 * detections built by hand.
 */
public class DifarMatchSelector {

	/** A group of detections, localised. */
	public static class Match {

		private final List<PamDataUnit> units;

		private final TargetMotionResult result;

		private final DifarLocalisationResiduals residuals;

		private Match(List<PamDataUnit> units, TargetMotionResult result,
				DifarLocalisationResiduals residuals) {
			this.units = units;
			this.result = result;
			this.residuals = residuals;
		}

		/** @return the detections in this group, the seed first. */
		public List<PamDataUnit> getUnits() {
			return units;
		}

		/** @return the localisation of the group. */
		public TargetMotionResult getResult() {
			return result;
		}

		/** @return how far the localisation sits from the measurements. */
		public DifarLocalisationResiduals getResiduals() {
			return residuals;
		}

		/**
		 * @return chi2 per degree of freedom, or NaN if it cannot be worked
		 * out. Only meaningful between groups of the same size.
		 */
		public double getChi2PerDegreeOfFreedom() {
			if (result == null || result.getChi2() == null
					|| result.getnDegreesFreedom() == null || result.getnDegreesFreedom() < 1) {
				return Double.NaN;
			}
			return result.getChi2() / result.getnDegreesFreedom();
		}
	}

	private final DifarProcess difarProcess;

	private final double timingErrorSeconds;

	private final double maxBearingResidual;

	private final double maxTimeDelayResidual;

	private final Simplex2D simplex = new Simplex2D();

	/**
	 * @param difarProcess the process the localisations belong to. May be null
	 * outside a running configuration.
	 * @param timingErrorSeconds timing error of one detection, in seconds.
	 * @param maxBearingResidual largest acceptable bearing residual, in degrees.
	 * @param maxTimeDelayResidual largest acceptable timing residual, in seconds.
	 */
	public DifarMatchSelector(DifarProcess difarProcess, double timingErrorSeconds,
			double maxBearingResidual, double maxTimeDelayResidual) {
		this.difarProcess = difarProcess;
		this.timingErrorSeconds = timingErrorSeconds;
		this.maxBearingResidual = maxBearingResidual;
		this.maxTimeDelayResidual = maxTimeDelayResidual;
	}

	/**
	 * Find the best group of detections that includes the seed.
	 * @param seed the detection being matched.
	 * @param candidatesByBuoy candidate detections, one list per other buoy.
	 * Lists may be empty. Order within a list does not matter.
	 * @return the best match, or null if no group passed the limits.
	 */
	public Match select(PamDataUnit seed, List<List<PamDataUnit>> candidatesByBuoy) {
		if (seed == null || candidatesByBuoy == null) {
			return null;
		}
		Match best = null;
		for (List<PamDataUnit> group : combinations(seed, candidatesByBuoy)) {
			Match match = localise(group);
			if (match == null) {
				continue;
			}
			if (!match.getResiduals().isWithin(maxBearingResidual, maxTimeDelayResidual)) {
				continue;
			}
			if (isBetter(match, best)) {
				best = match;
			}
		}
		return best;
	}

	/**
	 * @return true if a match should replace the best one so far. Bigger groups
	 * win. Between groups of the same size, the closer fit wins.
	 */
	private boolean isBetter(Match match, Match best) {
		if (best == null) {
			return true;
		}
		if (match.getUnits().size() != best.getUnits().size()) {
			return match.getUnits().size() > best.getUnits().size();
		}
		double theirs = best.getChi2PerDegreeOfFreedom();
		double ours = match.getChi2PerDegreeOfFreedom();
		if (Double.isNaN(ours)) {
			return false;
		}
		return Double.isNaN(theirs) || ours < theirs;
	}

	/**
	 * Every group that contains the seed and at most one detection from each
	 * other buoy, and that has at least two detections in total.
	 * @param seed the detection being matched.
	 * @param candidatesByBuoy candidate detections, one list per other buoy.
	 * @return the groups to try.
	 */
	private List<List<PamDataUnit>> combinations(PamDataUnit seed,
			List<List<PamDataUnit>> candidatesByBuoy) {
		List<List<PamDataUnit>> groups = new ArrayList<>();
		List<PamDataUnit> start = new ArrayList<>();
		start.add(seed);
		groups.add(start);
		for (List<PamDataUnit> buoyCandidates : candidatesByBuoy) {
			if (buoyCandidates == null || buoyCandidates.isEmpty()) {
				continue;
			}
			List<List<PamDataUnit>> grown = new ArrayList<>(groups);
			for (List<PamDataUnit> group : groups) {
				for (PamDataUnit candidate : buoyCandidates) {
					List<PamDataUnit> longer = new ArrayList<>(group);
					longer.add(candidate);
					grown.add(longer);
				}
			}
			groups = grown;
		}
		List<List<PamDataUnit>> usable = new ArrayList<>();
		for (List<PamDataUnit> group : groups) {
			if (group.size() > 1) {
				usable.add(group);
			}
		}
		return usable;
	}

	/**
	 * Localise one group of detections.
	 * @param group the detections, the seed first.
	 * @return the localised group, or null if the fit failed.
	 */
	private Match localise(List<PamDataUnit> group) {
		ArrayList<PamDataUnit> units = new ArrayList<>(group);
		DIFARTargetMotionInformation info = new DIFARTargetMotionInformation(difarProcess, units);
		info.setTimingErrorSeconds(timingErrorSeconds);
		simplex.setStartPoint(info.getMeanPosition());
		TargetMotionResult[] results = simplex.runModel(info);
		if (results == null || results.length != 1 || results[0] == null
				|| results[0].getLatLong() == null) {
			return null;
		}
		DifarLocalisationResiduals residuals =
				DifarLocalisationResiduals.calculate(info, results[0].getLatLong());
		return new Match(units, results[0], residuals);
	}
}
