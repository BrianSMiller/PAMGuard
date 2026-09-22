package difar;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/**
 * Which sonobuoy record was in force on each channel at any time.
 * <p>
 * This answers one question, for normal and viewer mode alike: given a channel
 * and a time, which record applies? A record applies from its own time until
 * the next record on the same channel, or until the buoy's end time. Before a
 * channel's first record, or after its buoy has ended, there is no record, and
 * the answer is null. A later record is never returned in place of an earlier
 * one.
 * <p>
 * The history holds no link to the database or the array. It is given the
 * full set of records and replaces what it held before. Lookups do not change
 * anything, so any number of callers can use it, in any order, on any thread.
 */
public class SonobuoyHistory {

	/**
	 * Records for each channel, keyed by the time they come into force. Replaced
	 * as a whole, never changed, so readers always see a complete set.
	 */
	private volatile Map<Integer, NavigableMap<Long, SonobuoyRecord>> recordsByChannel =
			Collections.emptyMap();

	/**
	 * Replace every record with a new set.
	 * <p>
	 * The records may be in any order. Where a channel has two records at the
	 * same time, the later one in the collection is kept. This removes the
	 * duplicates that the viewer can load.
	 * @param records the full set of records. Null entries are ignored.
	 */
	public void setRecords(Collection<SonobuoyRecord> records) {
		Map<Integer, NavigableMap<Long, SonobuoyRecord>> newRecords = new HashMap<>();
		if (records != null) {
			for (SonobuoyRecord record : records) {
				if (record == null) {
					continue;
				}
				newRecords.computeIfAbsent(record.getChannel(), c -> new TreeMap<>())
						.put(record.getTimeMillis(), record);
			}
		}
		for (Map.Entry<Integer, NavigableMap<Long, SonobuoyRecord>> entry : newRecords.entrySet()) {
			entry.setValue(Collections.unmodifiableNavigableMap(entry.getValue()));
		}
		recordsByChannel = Collections.unmodifiableMap(newRecords);
	}

	/**
	 * The record in force on a channel at a time.
	 * @param channel the channel.
	 * @param timeMillis the time in milliseconds.
	 * @return the latest record at or before this time, or null if there is
	 * none, or if that buoy had ended by this time.
	 */
	public SonobuoyRecord getRecordAt(int channel, long timeMillis) {
		NavigableMap<Long, SonobuoyRecord> records = recordsByChannel.get(channel);
		if (records == null) {
			return null;
		}
		Map.Entry<Long, SonobuoyRecord> entry = records.floorEntry(timeMillis);
		if (entry == null) {
			return null;
		}
		SonobuoyRecord record = entry.getValue();
		if (record.hasEndedBy(timeMillis)) {
			return null;
		}
		return record;
	}

	/**
	 * @return the number of records held, over all channels.
	 */
	public int getRecordCount() {
		int n = 0;
		for (NavigableMap<Long, SonobuoyRecord> records : recordsByChannel.values()) {
			n += records.size();
		}
		return n;
	}
}
