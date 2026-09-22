package difar;

/**
 * One sonobuoy record: which buoy was on a channel, where it was, and how its
 * compass was corrected, from a given time on.
 * <p>
 * A record stays in force on its channel until the next record on that
 * channel, or until the buoy's end time, whichever comes first. It is a plain
 * value with no link to the database or the array, so records can come from
 * the database, from tests, or from edits made by the user.
 */
public final class SonobuoyRecord {

	private final int channel;
	private final long timeMillis;
	private final Long endTimeMillis;
	private final String name;
	private final Double latitude;
	private final Double longitude;
	private final Double heading;

	/**
	 * @param channel channel the buoy was on.
	 * @param timeMillis time the record comes into force.
	 * @param endTimeMillis time the buoy ended, or null if it has not ended.
	 * @param name buoy name, or null.
	 * @param latitude buoy latitude in degrees, or null if not known.
	 * @param longitude buoy longitude in degrees, or null if not known.
	 * @param heading compass correction in degrees, or null if not calibrated.
	 */
	public SonobuoyRecord(int channel, long timeMillis, Long endTimeMillis, String name,
			Double latitude, Double longitude, Double heading) {
		this.channel = channel;
		this.timeMillis = timeMillis;
		this.endTimeMillis = endTimeMillis;
		this.name = name;
		this.latitude = latitude;
		this.longitude = longitude;
		this.heading = heading;
	}

	/**
	 * @return channel the buoy was on.
	 */
	public int getChannel() {
		return channel;
	}

	/**
	 * @return time the record comes into force, in milliseconds.
	 */
	public long getTimeMillis() {
		return timeMillis;
	}

	/**
	 * @return time the buoy ended in milliseconds, or null if it has not ended.
	 */
	public Long getEndTimeMillis() {
		return endTimeMillis;
	}

	/**
	 * @return buoy name, or null.
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return latitude in degrees, or null if not known.
	 */
	public Double getLatitude() {
		return latitude;
	}

	/**
	 * @return longitude in degrees, or null if not known.
	 */
	public Double getLongitude() {
		return longitude;
	}

	/**
	 * @return compass correction in degrees, or null if not calibrated.
	 */
	public Double getHeading() {
		return heading;
	}

	/**
	 * @return true if both latitude and longitude are known.
	 */
	public boolean hasPosition() {
		return latitude != null && longitude != null;
	}

	/**
	 * @param timeMillis a time in milliseconds.
	 * @return true if the buoy had ended by this time. The end time itself
	 * counts as ended.
	 */
	public boolean hasEndedBy(long timeMillis) {
		return endTimeMillis != null && timeMillis >= endTimeMillis;
	}

	@Override
	public String toString() {
		return String.format("SonobuoyRecord[ch%d %s at %d, end %s, pos %s, %s, heading %s]",
				channel, name, timeMillis, endTimeMillis, latitude, longitude, heading);
	}
}
