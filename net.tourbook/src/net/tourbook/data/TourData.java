/*******************************************************************************
 * Copyright (C) 2005, 2007  Wolfgang Schramm and Contributors
 *  
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software 
 * Foundation version 2 of the License.
 *  
 * This program is distributed in the hope that it will be useful, but WITHOUT 
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS 
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with 
 * this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110, USA    
 *******************************************************************************/
package net.tourbook.data;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.PostLoad;
import javax.persistence.PostUpdate;
import javax.persistence.Transient;

import net.tourbook.Messages;
import net.tourbook.chart.ChartMarker;
import net.tourbook.plugin.TourbookPlugin;
import net.tourbook.preferences.ITourbookPreferences;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.UI;

import org.eclipse.jface.preference.IPreferenceStore;
import org.hibernate.annotations.Cascade;

/**
 * Tour data contains all data for a tour (except markers), an entity will be saved in the database
 */
@Entity
public class TourData {

	/**
	 * 
	 */
	@Transient
	public static final int		MIN_TIMEINTERVAL_FOR_MAX_SPEED	= 20;
	@Transient
	public static final float	MAX_BIKE_SPEED					= 120f;

	/**
	 * Unique persistence id which identifies the tour
	 */
	@Id
	private Long				tourId;

	/**
	 * HH (d) hour of tour
	 */
	private short				startHour;

	/**
	 * MM (d) minute of tour
	 */
	private short				startMinute;

	/**
	 * year of tour start
	 */
	private short				startYear;

	/**
	 * mm (d) month of tour
	 */
	private short				startMonth;

	/**
	 * dd (d) day of tour
	 */
	private short				startDay;

	/**
	 * week of the tour, 0 is the first week
	 */
	private short				startWeek;

	/**
	 * tttt (h) total distance at tour start (km)
	 */
	private int					startDistance;

	/**
	 * ssss distance msw
	 * <p>
	 * is not used any more since 6.12.2006 but is necessary because it's a field in the database
	 */
	@SuppressWarnings("unused")
	private int					distance;

	/**
	 * aaaa (h) initial altitude (m)
	 */
	private short				startAltitude;

	/**
	 * pppp (h) initial pulse (bpm)
	 */
	private short				startPulse;

	/**
	 * Tolerance in the Douglas Peucker algorithm when segmenting the tour
	 */
	private short				dpTolerance						= 50;

	/**
	 * tt (h) type of tour <br>
	 * "2E" bike2 (CM414M) <br>
	 * "3E" bike1 (CM414M) <br>
	 * "81" jogging <br>
	 * "91" ski <br>
	 * "A1" bike<br>
	 * "B1" ski-bike
	 */
	@Column(length = 2)
	private String				deviceTourType;

	/*
	 * data from the device
	 */
	private long				deviceTravelTime;
	private int					deviceDistance;

	private int					deviceWheel;
	private int					deviceWeight;

	private int					deviceTotalUp;
	private int					deviceTotalDown;

	/**
	 * total distance (m)
	 */
	private int					tourDistance;

	/**
	 * total recording time (sec)
	 */
	private int					tourRecordingTime;

	/**
	 * total driving time (sec)
	 */
	private int					tourDrivingTime;

	/**
	 * altitude up (m)
	 */
	private int					tourAltUp;

	/**
	 * altitude down (m)
	 */
	private int					tourAltDown;

	/**
	 * plugin id for the device which was used for this tour
	 */
	private String				devicePluginId;

	/**
	 * Profile used by the device
	 */
	private short				deviceMode;													// db-version 3

	private short				deviceTimeInterval;											// db-version 3

	private int					maxAltitude;													// db-version 4
	private int					maxPulse;														// db-version 4
	private float				maxSpeed;														// db-version 4

	private int					avgPulse;														// db-version 4
	private int					avgCadence;													// db-version 4
	private int					avgTemperature;												// db-version 4

	private String				tourTitle;														// db-version 4
	private String				tourDescription;												// db-version 4
	private String				tourStartPlace;												// db-version 4
	private String				tourEndPlace;													// db-version 4

	private String				calories;														// db-version 4
	private float				bikerWeight;													// db-version 4

	/**
	 * visible name for the used plugin to import the data
	 */
	private String				devicePluginName;												// db-version 5

	/**
	 * visible name for <code>deviceMode</code>
	 */
	private String				deviceModeName;												// db-version 5

	/**
	 * data series for time, speed, altitude,...
	 */
	@Basic(optional = false)
	private SerieData			serieData;

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourMarker>		tourMarkers						= new HashSet<TourMarker>();

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "tourData", fetch = FetchType.EAGER)
	@Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
	private Set<TourReference>	tourReferences					= new HashSet<TourReference>();

	@ManyToMany(mappedBy = "tourData", fetch = FetchType.EAGER)
	private Set<TourCategory>	tourCategory					= new HashSet<TourCategory>();

	/**
	 * Category of the tour, e.g. bike, mountainbike, jogging, inlinescating
	 */
	@ManyToOne
	private TourType			tourType;

	/**
	 * Person which created this tour
	 */
	@ManyToOne
	private TourPerson			tourPerson;

	/**
	 * plugin id for the device which was used for this tour Bike used for this tour
	 */
	@ManyToOne
	private TourBike			tourBike;

	/*
	 * data series from the device
	 */
	@Transient
	public int[]				timeSerie;

	/**
	 * contains the distance data serie in the metric system
	 */
	@Transient
	public int[]				distanceSerie;
	@Transient
	public int[]				distanceSerieImperial;

	@Transient
	public int[]				altitudeSerie;
	@Transient
	public int[]				altitudeSerieImperial;

	@Transient
	public int[]				cadenceSerie;

	@Transient
	public int[]				pulseSerie;

	@Transient
	public int[]				temperatureSerie;

	/*
	 * computed data series
	 */
	@Transient
	public int[]				speedSerie;
	@Transient
	public int[]				speedSerieImperial;

	@Transient
	public int[]				powerSerie;

	@Transient
	public int[]				altimeterSerie;

	@Transient
	public int[]				gradientSerie;

	@Transient
	public int[]				tourCompareSerie;

	/**
	 * Index of the segmented data in the original serie
	 */
	@Transient
	public int					segmentSerieIndex[];

	/**
	 * oooo (o) DD-record // offset
	 */
	@Transient
	public int					offsetDDRecord;

	@Transient
	private Object[]			fTourSegments;

	/*
	 * data for the tour segments
	 */
	@Transient
	public int[]				segmentSerieAltitude;

	@Transient
	public int[]				segmentSerieDistance;

	@Transient
	public int[]				segmentSerieTime;

	@Transient
	public int[]				segmentSerieDrivingTime;

	@Transient
	public float[]				segmentSerieAltimeter;

	@Transient
	public int[]				segmentSerieAltitudeDown;

	@Transient
	public float[]				segmentSerieSpeed;

	@Transient
	public float[]				segmentSerieGradient;

	@Transient
	public float[]				segmentSeriePulse;

	@Transient
	public float[]				segmentSerieCadence;

	/**
	 * contains the filename from which the data are imported, when set to <code>null</code> the
	 * data are not imported they are from the database
	 */
	@Transient
	public String				importRawDataFile;

	public TourData() {}

	/**
	 * remove speed data series so the next time when they are used they will recomputed
	 */
	public void cleanSpeedSerie() {
		speedSerie = null;
		speedSerieImperial = null;
	}

	public void computeAvgCadence() {
		long cadenceSum = 0;
		int cadenceCount = 0;

		for (int cadence : cadenceSerie) {
			if (cadence > 0) {
				cadenceCount++;
				cadenceSum += cadence;
			}
		}
		if (cadenceCount > 0) {
			avgCadence = (int) cadenceSum / cadenceCount;
		}
	}

	public void computeAvgFields() {
		computeMaxAltitude();
		computeMaxPulse();
		computeAvgPulse();
		computeAvgCadence();
		computeAvgTemperature();
		computeMaxSpeed();
	}

	public void computeAvgPulse() {
		long pulseSum = 0;
		int pulseCount = 0;

		for (int pulse : pulseSerie) {
			if (pulse > 0) {
				pulseCount++;
				pulseSum += pulse;
			}
		}

		if (pulseCount > 0) {
			avgPulse = (int) pulseSum / pulseCount;
		}
	}

	public void computeAvgTemperature() {
		long temperatureSum = 0;

		for (int temperature : temperatureSerie) {
			temperatureSum += temperature;
		}

		final int tempLength = temperatureSerie.length;
		if (tempLength > 0) {
			avgTemperature = (int) temperatureSum / tempLength;
		}
	}

	public void computeMaxAltitude() {
		int maxAltitude = 0;
		for (int altitude : altitudeSerie) {
			if (altitude > maxAltitude) {
				maxAltitude = altitude;
			}
		}
		this.maxAltitude = maxAltitude;
	}

	public void computeMaxPulse() {
		int maxPulse = 0;

		for (int pulse : pulseSerie) {
			if (pulse > maxPulse) {
				maxPulse = pulse;
			}
		}
		this.maxPulse = maxPulse;
	}

	public void computeMaxSpeed() {
		float maxSpeed = 0;
		int anzValuesSum = 1;
		if (distanceSerie.length >= 2) {
			if (deviceTimeInterval > 0) {
				anzValuesSum = MIN_TIMEINTERVAL_FOR_MAX_SPEED / deviceTimeInterval;
				if (anzValuesSum == 0) {
					anzValuesSum = 1;
				}
			}

			for (int i = 0 + anzValuesSum; i <= distanceSerie.length - 1; i++) {
				float speed = ((float) distanceSerie[i] - (float) distanceSerie[i - anzValuesSum])
						/ ((float) timeSerie[i] - (float) timeSerie[i - anzValuesSum])
						* 3.6f;

				if (speed > maxSpeed && speed < MAX_BIKE_SPEED) {
					maxSpeed = speed;
				}
			}
		}
		this.maxSpeed = maxSpeed;
	}

	public void computeSpeedSerieCustom() {

		final float distanceMeasurementFactor = UI.UNIT_VALUE_DISTANCE;

		if (distanceMeasurementFactor == 1) {

			// use metric system

			if (speedSerie == null) {
				speedSerie = computeSpeedSerieCustom(distanceMeasurementFactor);
			}

		} else {

			// use imperial system

			if (speedSerieImperial == null) {
				speedSerieImperial = computeSpeedSerieCustom(distanceMeasurementFactor);
			}
		}

	}

	private int[] computeSpeedSerieCustom(float distanceMeasurementFactor) {

		final int serieLength = timeSerie.length;

		final int dataSerie[] = new int[serieLength];
		final int deviceTimeInterval = getDeviceTimeInterval();

		int lowIndexAdjustment = 0;
		int highIndexAdjustment = 1;

		final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
		final int speedTimeSlice = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE);

		final int slices = speedTimeSlice / deviceTimeInterval;

		highIndexAdjustment = Math.max(1, slices / 2);
		lowIndexAdjustment = slices / 2;

		// round up
		if (lowIndexAdjustment + highIndexAdjustment < slices) {
			highIndexAdjustment++;
		}

		for (int speedIndex = 0; speedIndex < serieLength; speedIndex++) {

			// adjust index to the array size
			final int distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustment),
					serieLength - 1);
			final int distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustment,
					serieLength - 1));

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float timeInterval = deviceTimeInterval * (distIndexHigh - distIndexLow);

			final int speed = (int) ((distance * 36F) / timeInterval / distanceMeasurementFactor);

			dataSerie[speedIndex] = speed;
		}

		return dataSerie;
	}

	/**
	 * Computes the speed data serie with the internal algorithm for a fix time interval
	 */
	public void computeSpeedSerieInternal() {

		final float distanceMeasurementFactor = UI.UNIT_VALUE_DISTANCE;

		if (distanceMeasurementFactor == 1) {

			// use metric system

			if (speedSerie == null) {
				speedSerie = computeSpeedSerieInternal(distanceMeasurementFactor);
			}

		} else {

			// use imperial system

			if (speedSerieImperial == null) {
				speedSerieImperial = computeSpeedSerieInternal(distanceMeasurementFactor);
			}
		}
	}

	/**
	 * Computes the speed data serie with the internal algorithm for a fix time interval
	 * 
	 * @param distanceMeasurementFactor
	 *        measurement system factor for the distance
	 * @return
	 */
	private int[] computeSpeedSerieInternal(float distanceMeasurementFactor) {

		final int serieLength = timeSerie.length;

		final int dataSerie[] = new int[serieLength];
		final int deviceTimeInterval = getDeviceTimeInterval();

		int lowIndexAdjustmentDefault = 0;
		int highIndexAdjustmentDefault = 0;

		if (deviceTimeInterval <= 2) {
			lowIndexAdjustmentDefault = 2;
			highIndexAdjustmentDefault = 3;

		} else if (deviceTimeInterval <= 5) {
			lowIndexAdjustmentDefault = 1;
			highIndexAdjustmentDefault = 1;

		} else if (deviceTimeInterval <= 10) {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		} else {
			lowIndexAdjustmentDefault = 0;
			highIndexAdjustmentDefault = 1;
		}

		for (int speedIndex = 0; speedIndex < serieLength; speedIndex++) {

			// adjust index to the array size
			int distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustmentDefault),
					serieLength - 1);

			int distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustmentDefault,
					serieLength - 1));

			final int distanceDefault = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];

			// adjust the accuracy for the distance
			int lowIndexAdjustment = lowIndexAdjustmentDefault;
			int highIndexAdjustment = highIndexAdjustmentDefault;

			if (distanceDefault < 30) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 3;
				highIndexAdjustment = highIndexAdjustmentDefault + 3;
			} else if (distanceDefault < 50) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 2;
				highIndexAdjustment = highIndexAdjustmentDefault + 2;
			} else if (distanceDefault < 100) {
				lowIndexAdjustment = lowIndexAdjustmentDefault + 1;
				highIndexAdjustment = highIndexAdjustmentDefault + 1;
			}

			// adjust index to the array size
			distIndexLow = Math.min(Math.max(0, speedIndex - lowIndexAdjustment), serieLength - 1);
			distIndexHigh = Math.max(0, Math.min(speedIndex + highIndexAdjustment, serieLength - 1));

			final int distance = distanceSerie[distIndexHigh] - distanceSerie[distIndexLow];
			final float timeInterval = deviceTimeInterval * (distIndexHigh - distIndexLow);

			final int speed = (int) ((distance * 36F) / timeInterval / distanceMeasurementFactor);

			dataSerie[speedIndex] = speed;
		}

		return dataSerie;
	}

	public void computeSpeedSerieInternalWithVariableInterval(boolean useCustomValue) {

		final float distanceMeasurementFactor = UI.UNIT_VALUE_DISTANCE;

		if (distanceMeasurementFactor == 1) {

			// use metric system

			if (speedSerie == null) {
				speedSerie = computeSpeedSerieInternalWithVariableInterval(distanceMeasurementFactor,
						useCustomValue);
			}

		} else {

			// use imperial system

			if (speedSerieImperial == null) {
				speedSerieImperial = computeSpeedSerieInternalWithVariableInterval(distanceMeasurementFactor,
						useCustomValue);
			}
		}
	}

	/**
	 * compute the speed when the time serie has unequal time intervalls
	 * 
	 * @param tourData
	 * @param hasPropertyChanged
	 * @param tourChartProperty
	 */
	private int[] computeSpeedSerieInternalWithVariableInterval(float distanceMeasurementFactor,
																final boolean useCustomValue) {

		final int serieLength = timeSerie.length;
		final int dataSerie[] = new int[serieLength];

		for (int distIndex = 0; distIndex < serieLength; distIndex++) {

			// adjust index to the array size
			int lowIndex = Math.max(0, distIndex - 1);

			int timeDiff = timeSerie[distIndex] - timeSerie[lowIndex];
			int distDiff = 0;

			int minInterval;
			if (useCustomValue) {
				final IPreferenceStore store = TourbookPlugin.getDefault().getPreferenceStore();
				minInterval = store.getInt(ITourbookPreferences.GRAPH_PROPERTY_TIMESLICE_COMPUTE_VALUE);
			} else {
				minInterval = 20;
			}

			if (timeDiff < minInterval) {
				while (timeDiff < minInterval) {

					// make sure to be in the array range
					if (lowIndex < 1) {
						break;
					}

					lowIndex--;

					timeDiff = timeSerie[distIndex] - timeSerie[lowIndex];
				}
			}

			distDiff = distanceSerie[distIndex] - distanceSerie[lowIndex];

			int speed;
			if (timeDiff == 0) {
				speed = 0;
			} else {
				speed = (int) ((distDiff * 36f) / timeDiff / distanceMeasurementFactor);
			}

			dataSerie[distIndex] = speed < 0 ? 0 : speed;
		}

		return dataSerie;
	}

	public void computeTourDrivingTime() {

		int maxIndex = Math.max(0, timeSerie.length - 1);

		int ignoreTimeSlices = deviceTimeInterval == 0
				? 0
				: TourManager.getIgnoreTimeSlices(distanceSerie,
						0,
						maxIndex,
						10 / deviceTimeInterval);

		tourDrivingTime = Math.max(0, timeSerie[maxIndex] - (ignoreTimeSlices * deviceTimeInterval));

	}

	/**
	 * Convert <code>TimeData</code> into <code>TourData</code>
	 * 
	 * @param createMarker
	 *        creates the markers when set to <code>true</code>
	 */
	public void createTimeSeries(ArrayList<TimeData> timeDataList, boolean createMarker) {

		int serieLength = timeDataList.size();

		if (serieLength > 0) {

			timeSerie = new int[serieLength];
			distanceSerie = new int[serieLength];
			altitudeSerie = new int[serieLength];
			pulseSerie = new int[serieLength];
			cadenceSerie = new int[serieLength];
			temperatureSerie = new int[serieLength];

			// speedSerie = new int[serieLength];
			// powerSerie = new int[serieLength];

			int distanceDiff[] = new int[serieLength];
			int timeIndex = 0;

			int timeAbsolute = 0;

			int altitudeAbsolute = 0;
			int distanceAbsolute = 0;

			// convert data from the tour format into an interger[]
			for (TimeData timeItem : timeDataList) {

				timeSerie[timeIndex] = timeAbsolute;

				distanceSerie[timeIndex] = distanceAbsolute += timeItem.distance;
				altitudeSerie[timeIndex] = altitudeAbsolute += timeItem.altitude;

				pulseSerie[timeIndex] = timeItem.pulse;
				temperatureSerie[timeIndex] = timeItem.temperature;
				cadenceSerie[timeIndex] = timeItem.cadence;

				// powerSerie[index] = 0;

				distanceDiff[timeIndex] = timeItem.distance;

				if (createMarker && timeItem.marker != 0) {

					// create a new marker
					TourMarker tourMarker = new TourMarker(this, ChartMarker.MARKER_TYPE_DEVICE);
					tourMarker.setLabel(Messages.TourData_Label_device_marker);
					tourMarker.setVisualPosition(ChartMarker.VISUAL_HORIZONTAL_ABOVE_GRAPH_CENTERED);
					tourMarker.setTime(timeAbsolute + timeItem.marker);
					tourMarker.setDistance(distanceAbsolute);
					tourMarker.setSerieIndex(timeIndex);

					getTourMarkers().add(tourMarker);
				}

//				timeAbsolute += deviceTimeInterval;
				timeAbsolute += timeItem.time;

				timeIndex++;
			}

			tourDistance = distanceAbsolute;
			tourRecordingTime = timeAbsolute;
		}
	}

	/**
	 * Creates the unique tour id from the tour date/time and the unique key
	 * 
	 * @param uniqueKey
	 *        unique key to identify a tour
	 */
	public void createTourId(String uniqueKey) {

//		final String uniqueKey = Integer.toString(Math.abs(getStartDistance()));

		String tourId;

		try {
			/*
			 * this is the default implementation to create a tour id, but on the 5.5.2007 a
			 * NumberFormatException occured so the calculation for the tour id was adjusted
			 */
			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey;

			setTourId(Long.parseLong(tourId));

		} catch (NumberFormatException e) {

			/*
			 * the distance was shorted so that the maximum of a Long datatype is not exceeded
			 */

			tourId = Short.toString(getStartYear())
					+ Short.toString(getStartMonth())
					+ Short.toString(getStartDay())
					+ Short.toString(getStartHour())
					+ Short.toString(getStartMinute())
					+ uniqueKey.substring(0, Math.min(5, uniqueKey.length()));

			setTourId(Long.parseLong(tourId));
		}

	}

	/**
	 * Create the tour segment list from the segment index array
	 * 
	 * @return
	 */
	public Object[] createTourSegments() {

		final int segmentSerieLength = segmentSerieIndex.length;

		if (segmentSerieIndex == null || segmentSerieLength < 2) {
			// at least two points are required to build a segment
			return new Object[0];
		}

		final ArrayList<TourSegment> tourSegments = new ArrayList<TourSegment>(segmentSerieLength);
		final int firstSerieIndex = segmentSerieIndex[0];

		// get start values
		int distanceStart = distanceSerie[firstSerieIndex];
		int altitudeStart = altitudeSerie[firstSerieIndex];
		int timeStart = timeSerie[firstSerieIndex];
		int timeSlice = timeSerie[1] - timeSerie[0];
		if (timeSlice == 0 && timeSerie.length > 1) {
			timeSlice = timeSerie[2] - timeSerie[1];
		}

		segmentSerieAltitude = new int[segmentSerieLength];
		segmentSerieDistance = new int[segmentSerieLength];
		segmentSerieTime = new int[segmentSerieLength];
		segmentSerieDrivingTime = new int[segmentSerieLength];
		segmentSerieAltitudeDown = new int[segmentSerieLength];

		segmentSerieAltimeter = new float[segmentSerieLength];
		segmentSerieSpeed = new float[segmentSerieLength];
		segmentSerieGradient = new float[segmentSerieLength];
		segmentSeriePulse = new float[segmentSerieLength];
		segmentSerieCadence = new float[segmentSerieLength];

		for (int iSegment = 1; iSegment < segmentSerieLength; iSegment++) {

			final int segmentIndex = iSegment;

			final int segmentStartIndex = segmentSerieIndex[iSegment - 1];
			final int segmentEndIndex = segmentSerieIndex[iSegment];

			final TourSegment segment = new TourSegment();
			tourSegments.add(segment);

			segment.serieIndexStart = segmentStartIndex;
			segment.serieIndexEnd = segmentEndIndex;

			// compute difference values between start and end
			final int altitudeEnd = altitudeSerie[segmentEndIndex];
			final int distanceEnd = distanceSerie[segmentEndIndex];
			final int timeEnd = timeSerie[segmentEndIndex];
			final int recordingTime = timeEnd - timeStart;

			segmentSerieAltitude[segmentIndex] = segment.altitude = altitudeEnd - altitudeStart;
			segmentSerieDistance[segmentIndex] = segment.distance = distanceEnd - distanceStart;

			segmentSerieTime[segmentIndex] = segment.recordingTime = recordingTime;
			final int ignoreTimeSlices = timeSlice == 0
					? 0
					: TourManager.getIgnoreTimeSlices(distanceSerie,
							segmentStartIndex,
							segmentEndIndex,
							10 / timeSlice);
			segmentSerieDrivingTime[segmentIndex] = segment.drivingTime = (recordingTime - (ignoreTimeSlices * timeSlice));

			int altitudeUp = 0;
			int altitudeDown = 0;
			int altitude1 = altitudeSerie[segmentStartIndex];

			// compute altitude up/down for the segment
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {

				final int altitude2 = altitudeSerie[serieIndex];
				final int altitudeDiff = altitude2 - altitude1;
				altitude1 = altitude2;

				altitudeUp += altitudeDiff >= 0 ? altitudeDiff : 0;
				altitudeDown += altitudeDiff < 0 ? altitudeDiff : 0;
			}

			// compute pulse average
			int pulseSum = 0;
			for (int serieIndex = segmentStartIndex + 1; serieIndex <= segmentEndIndex; serieIndex++) {
				pulseSum += pulseSerie[serieIndex];
			}

			segment.altitudeUp = altitudeUp;
			segmentSerieAltitudeDown[segmentIndex] = segment.altitudeDown = altitudeDown;

			segmentSerieSpeed[segmentIndex] = segment.speed = segment.drivingTime == 0
					? 0
					: (float) ((float) segment.distance / segment.drivingTime * 3.6);

			segmentSerieGradient[segmentIndex] = segment.gradient = (float) segment.altitude
					* 100
					/ segment.distance;

			segmentSerieAltimeter[segmentIndex] = segment.drivingTime == 0
					? 0
					: (float) (segment.altitudeUp) / segment.drivingTime * 3600;

			segmentSeriePulse[segmentIndex] = pulseSum / (segmentEndIndex - segmentStartIndex);

			// end point of current segment is the start of the next segment
			altitudeStart = altitudeEnd;
			distanceStart = distanceEnd;
			timeStart = timeEnd;
		}

		fTourSegments = tourSegments.toArray();

		return fTourSegments;
	}

	public void dumpData() {

		PrintStream out = System.out;

		out.println("----------------------------------------------------"); //$NON-NLS-1$
		out.println("TOUR DATA"); //$NON-NLS-1$
		out.println("----------------------------------------------------"); //$NON-NLS-1$
// out.println("Typ: " + getDeviceTourType()); //$NON-NLS-1$
		out.println("Date:			" + getStartDay() + "." + getStartMonth() + "." + getStartYear()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		out.println("Time:			" + getStartHour() + ":" + getStartMinute()); //$NON-NLS-1$ //$NON-NLS-2$
		out.println("Total distance:		" + getStartDistance()); //$NON-NLS-1$
		// out.println("Distance: " + getDistance());
		out.println("Altitude:		" + getStartAltitude()); //$NON-NLS-1$
		out.println("Pulse:			" + getStartPulse()); //$NON-NLS-1$
		out.println("Offset DD record:	" + offsetDDRecord); //$NON-NLS-1$
	}

	public void dumpTime() {
		PrintStream out = System.out;

		out.print((getTourRecordingTime() / 3600) + ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) % 60)
				+ "  "); //$NON-NLS-1$
		out.print(getTourDistance());
	}

	public void dumpTourTotal() {

		PrintStream out = System.out;

		out.println("Tour distance (m):	" + getTourDistance()); //$NON-NLS-1$

		out.println("Tour time:		" //$NON-NLS-1$
				+ (getTourRecordingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourRecordingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourRecordingTime() % 3600)
				% 60);

		out.println("Driving time:		" //$NON-NLS-1$
				+ (getTourDrivingTime() / 3600)
				+ ":" //$NON-NLS-1$
				+ ((getTourDrivingTime() % 3600) / 60)
				+ ":" //$NON-NLS-1$
				+ (getTourDrivingTime() % 3600)
				% 60);

		out.println("Altitude up (m):	" + getTourAltUp()); //$NON-NLS-1$
		out.println("Altitude down (m):	" + getTourAltDown()); //$NON-NLS-1$
	}

	/**
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj == null) {
			return false;
		}

		if (this == obj) {
			return true;
		}

		if (!this.getClass().equals(obj.getClass())) {
			return false;
		}

		TourData td = (TourData) obj;

		return this.getStartYear() == td.getStartYear()
				&& this.getStartMonth() == td.getStartMonth()
				&& this.getStartDay() == td.getStartDay()
				&& this.getStartHour() == td.getStartHour()
				&& this.getStartMinute() == td.getStartMinute()
				&& this.getTourDistance() == td.getTourDistance()
				&& this.getTourRecordingTime() == td.getTourRecordingTime();
	}

	/**
	 * @return the avgCadence
	 */
	public int getAvgCadence() {
		return avgCadence;
	}

	/**
	 * @return the avgPulse
	 */
	public int getAvgPulse() {
		return avgPulse;
	}

	/**
	 * @return the avgTemperature
	 */
	public int getAvgTemperature() {
		return avgTemperature;
	}

	/**
	 * @return the bikerWeight
	 */
	public float getBikerWeight() {
		return bikerWeight;
	}

	/**
	 * @return the calories
	 */
	public String getCalories() {
		return calories;
	}

	public int getDeviceDistance() {
		return deviceDistance;
	}

	public String getDeviceId() {
		return devicePluginId;
	}

	public short getDeviceMode() {
		return deviceMode;
	}

	public String getDeviceModeName() {
		return deviceModeName;
	}

	public String getDeviceName() {
		return devicePluginName;
	}

	public short getDeviceTimeInterval() {
		return deviceTimeInterval;
	}

	public int getDeviceTotalDown() {
		return deviceTotalDown;
	}

	public int getDeviceTotalUp() {
		return deviceTotalUp;
	}

	public String getDeviceTourType() {
		return deviceTourType;
	}

	public long getDeviceTravelTime() {
		return deviceTravelTime;
	}

	public int getDeviceWeight() {
		return deviceWeight;
	}

	public int getDeviceWheel() {
		return deviceWheel;
	}

	/**
	 * @return Returns the distance data serie for the current measurement system which can be
	 *         metric or imperial
	 */
	public int[] getDistanceSerie() {

		int[] serie;

		final float unitValueDistance = UI.UNIT_VALUE_DISTANCE;

		if (unitValueDistance != 1) {

			// use imperial system

			if (distanceSerieImperial == null) {

				// compute imperial data

				distanceSerieImperial = new int[distanceSerie.length];

				for (int valueIndex = 0; valueIndex < distanceSerie.length; valueIndex++) {
					distanceSerieImperial[valueIndex] = (int) (distanceSerie[valueIndex] / unitValueDistance);
				}
			}
			serie = distanceSerieImperial;

		} else {

			// use metric system

			serie = distanceSerie;
		}

		return serie;
	}

	public short getDpTolerance() {
		return dpTolerance;
	}

	/**
	 * @return the maxAltitude
	 */
	public int getMaxAltitude() {
		return maxAltitude;
	}

	/**
	 * @return the maxPulse
	 */
	public int getMaxPulse() {
		return maxPulse;
	}

	/**
	 * @return the maxSpeed
	 */
	public float getMaxSpeed() {
		return maxSpeed;
	}

	/**
	 * @return Returns the distance serie in the metric system, the distance serie is saved in the
	 *         database <b>always</b> in the metric system
	 */
	public int[] getMetricDistanceSerie() {
		return distanceSerie;
	}

	public SerieData getSerieData() {
		return serieData;
	}

	public int[] getSpeedSerie() {

		final float distanceMeasurementFactor = UI.UNIT_VALUE_DISTANCE;

		if (distanceMeasurementFactor == 1) {

			// use metric system

			if (speedSerie == null) {
				speedSerie = computeSpeedSerieInternal(distanceMeasurementFactor);
			}

			return speedSerie;

		} else {

			// use imperial system

			if (speedSerieImperial == null) {
				speedSerieImperial = computeSpeedSerieInternal(distanceMeasurementFactor);
			}

			return speedSerieImperial;
		}
	}

	public short getStartAltitude() {
		return startAltitude;
	}

	public short getStartDay() {
		return startDay;
	}

	public int getStartDistance() {
		return startDistance;
	}

	public short getStartHour() {
		return startHour;
	}

	public short getStartMinute() {
		return startMinute;
	}

	public short getStartMonth() {
		return startMonth;
	}

	public short getStartPulse() {
		return startPulse;
	}

	public short getStartWeek() {
		return startWeek;
	}

	public short getStartYear() {
		return startYear;
	}

	public int getTourAltDown() {
		return tourAltDown;
	}

	public int getTourAltUp() {
		return tourAltUp;
	}

	public TourBike getTourBike() {
		return tourBike;
	}

	public Collection<TourCategory> getTourCategory() {
		return tourCategory;
	}

	/**
	 * @return the tourDescription
	 */
	public String getTourDescription() {
		return tourDescription == null ? "" : tourDescription; //$NON-NLS-1$
	}

	public int getTourDistance() {
		return tourDistance;
	}

	public int getTourDrivingTime() {
		return tourDrivingTime;
	}

	/**
	 * @return the tourEndPlace
	 */
	public String getTourEndPlace() {
		return tourEndPlace == null ? "" : tourEndPlace; //$NON-NLS-1$
	}

	/**
	 * @return Returns the unique key in the database for this {@link TourData} entity
	 */
	public Long getTourId() {
		return tourId;
	}

	public Set<TourMarker> getTourMarkers() {
		return tourMarkers;
	}

	/**
	 * @return returns the person for whom the tour data is saved or <code>null</code> when the
	 *         tour is not saved in the database
	 */
	public TourPerson getTourPerson() {
		return tourPerson;
	}

	public int getTourRecordingTime() {
		return tourRecordingTime;
	}

	public Collection<TourReference> getTourReferences() {
		return tourReferences;
	}

	public Object[] getTourSegments() {
		return fTourSegments;
	}

	/**
	 * @return the tourStartPlace
	 */
	public String getTourStartPlace() {
		return tourStartPlace == null ? "" : tourStartPlace; //$NON-NLS-1$
	}

	/**
	 * @return the tourTitle
	 */
	public String getTourTitle() {
		return tourTitle == null ? "" : tourTitle; //$NON-NLS-1$
	}

	public TourType getTourType() {
		return tourType;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		int result = 17;

		result = 37 * result + this.getStartYear();
		result = 37 * result + this.getStartMonth();
		result = 37 * result + this.getStartDay();
		result = 37 * result + this.getStartHour();
		result = 37 * result + this.getStartMinute();
		result = 37 * result + this.getTourDistance();
		result = 37 * result + this.getTourRecordingTime();

		return result;
	}

	/**
	 * Called after the object was loaded from the persistence store
	 */
	@PostLoad
	@PostUpdate
	public void onPostLoad() {
		/*
		 * create tourdata from the serialized data
		 */

		int serieLength = serieData.timeSerie.length;

		altitudeSerie = new int[serieLength];
		cadenceSerie = new int[serieLength];
		distanceSerie = new int[serieLength];
		pulseSerie = new int[serieLength];
		temperatureSerie = new int[serieLength];
		timeSerie = new int[serieLength];

		// speedSerie = new int[serieLength];
		// powerSerie = new int[serieLength];

		System.arraycopy(serieData.altitudeSerie, 0, altitudeSerie, 0, serieLength);
		System.arraycopy(serieData.cadenceSerie, 0, cadenceSerie, 0, serieLength);
		System.arraycopy(serieData.distanceSerie, 0, distanceSerie, 0, serieLength);
		System.arraycopy(serieData.pulseSerie, 0, pulseSerie, 0, serieLength);
		System.arraycopy(serieData.temperatureSerie, 0, temperatureSerie, 0, serieLength);
		System.arraycopy(serieData.timeSerie, 0, timeSerie, 0, serieLength);

		// System.arraycopy(serieData.speedSerie, 0, speedSerie, 0,
		// serieLength);
		// System.arraycopy(serieData.powerSerie, 0, powerSerie, 0,
		// serieLength);
	}

	/**
	 * Called before this object gets persisted, copy data from the tourdata object into the object
	 * which gets serialized
	 */
	/*
	 * @PrePersist + @PreUpdate is currently disabled for EJB events because of bug
	 * http://opensource.atlassian.com/projects/hibernate/browse/HHH-1921 2006-08-11
	 */
	public void onPrePersist() {

		if (timeSerie == null) {
			setSerieData(new SerieData(0));
			return;
		}

		final int serieLength = timeSerie.length;

		serieData = new SerieData(serieLength);

		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);

		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
		// serieLength);
		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
		// serieLength);
	}

	/**
	 * @param avgCadence
	 *        the avgCadence to set
	 */
	public void setAvgCadence(int avgCadence) {
		this.avgCadence = avgCadence;
	}

	/**
	 * @param avgPulse
	 *        the avgPulse to set
	 */
	public void setAvgPulse(int avgPulse) {
		this.avgPulse = avgPulse;
	}

	/**
	 * @param avgTemperature
	 *        the avgTemperature to set
	 */
	public void setAvgTemperature(int avgTemperature) {
		this.avgTemperature = avgTemperature;
	}

	/**
	 * @param bikerWeight
	 *        the bikerWeight to set
	 */
	public void setBikerWeight(float bikerWeight) {
		this.bikerWeight = bikerWeight;
	}

	/**
	 * @param calories
	 *        the calories to set
	 */
	public void setCalories(String calories) {
		this.calories = calories;
	}

	public void setDeviceDistance(int deviceDistance) {
		this.deviceDistance = deviceDistance;
	}

	public void setDeviceId(String deviceId) {
		this.devicePluginId = deviceId;
	}

	public void setDeviceMode(short deviceMode) {
		this.deviceMode = deviceMode;
	}

	public void setDeviceModeName(String deviceModeName) {
		this.deviceModeName = deviceModeName;
	}

	public void setDeviceName(String deviceName) {
		devicePluginName = deviceName;
	}

	/**
	 * time difference between 2 time slices or <code>-1</code> when the time slices are unequally
	 * 
	 * @param deviceTimeInterval
	 */
	public void setDeviceTimeInterval(short deviceTimeInterval) {
		this.deviceTimeInterval = deviceTimeInterval;
	}

	public void setDeviceTotalDown(int deviceTotalDown) {
		this.deviceTotalDown = deviceTotalDown;
	}

	public void setDeviceTotalUp(int deviceTotalUp) {
		this.deviceTotalUp = deviceTotalUp;
	}

	public void setDeviceTourType(String tourType) {
		this.deviceTourType = tourType;
	}

	public void setDeviceTravelTime(long deviceTravelTime) {
		this.deviceTravelTime = deviceTravelTime;
	}

	public void setDeviceWeight(int deviceWeight) {
		this.deviceWeight = deviceWeight;
	}

	public void setDeviceWheel(int deviceWheel) {
		this.deviceWheel = deviceWheel;
	}

	public void setDpTolerance(short dpTolerance) {
		this.dpTolerance = dpTolerance;
	}

	/**
	 * @param maxAltitude
	 *        the maxAltitude to set
	 */
	public void setMaxAltitude(int maxAltitude) {
		this.maxAltitude = maxAltitude;
	}

	/**
	 * @param maxPulse
	 *        the maxPulse to set
	 */
	public void setMaxPulse(int maxPulse) {
		this.maxPulse = maxPulse;
	}

	/**
	 * @param maxSpeed
	 *        the maxSpeed to set
	 */
	public void setMaxSpeed(float maxSpeed) {
		this.maxSpeed = maxSpeed;
	}

	public void setSerieData(SerieData serieData) {
		this.serieData = serieData;
	}

/*
 * private int maxAltitude; private int maxPulse; private int avgPulse; private int avgCadence;
 * private int avgTemperature; private float maxSpeed;
 */

	public void setStartAltitude(short startAltitude) {
		this.startAltitude = startAltitude;
	}

	public void setStartDay(short startDay) {
		this.startDay = startDay;
	}

	public void setStartDistance(int startDistance) {
		this.startDistance = startDistance;
	}

	public void setStartHour(short startHour) {
		this.startHour = startHour;
	}

	public void setStartMinute(short startMinute) {
		this.startMinute = startMinute;
	}

	public void setStartMonth(short startMonth) {
		this.startMonth = startMonth;
	}

	public void setStartPulse(short startPulse) {
		this.startPulse = startPulse;
	}

	public void setStartWeek(short startWeek) {
		this.startWeek = startWeek;
	}

	public void setStartYear(short startYear) {
		this.startYear = startYear;
	}

	public void setTourAltDown(int tourAltDown) {
		this.tourAltDown = tourAltDown;
	}

	public void setTourAltUp(int tourAltUp) {
		this.tourAltUp = tourAltUp;
	}

	public void setTourBike(TourBike tourBike) {
		this.tourBike = tourBike;
	}

	/**
	 * @param tourDescription
	 *        the tourDescription to set
	 */
	public void setTourDescription(String tourDescription) {
		this.tourDescription = tourDescription;
	}

	/**
	 * Set total driving time
	 * 
	 * @param tourDrivingTime
	 */
	public void setTourDrivingTime(int tourDrivingTime) {
		this.tourDrivingTime = tourDrivingTime;
	}

	/**
	 * @param tourEndPlace
	 *        the tourEndPlace to set
	 */
	public void setTourEndPlace(String tourEndPlace) {
		this.tourEndPlace = tourEndPlace;
	}

	public void setTourId(Long tourId) {
		this.tourId = tourId;
	}

	public void setTourMarkers(Set<TourMarker> tourMarkers) {
		this.tourMarkers = tourMarkers;
	}

	public void setTourPerson(TourPerson tourPerson) {
		this.tourPerson = tourPerson;
	}

	/**
	 * @param tourStartPlace
	 *        the tourStartPlace to set
	 */
	public void setTourStartPlace(String tourStartPlace) {
		this.tourStartPlace = tourStartPlace;
	}

	/**
	 * @param tourTitle
	 *        the tourTitle to set
	 */
	public void setTourTitle(String tourTitle) {
		this.tourTitle = tourTitle;
	}

	public void setTourType(TourType tourType) {
		this.tourType = tourType;
	}

	public void updateSerieData() {

		final int serieLength = timeSerie.length;

		System.arraycopy(altitudeSerie, 0, serieData.altitudeSerie, 0, serieLength);
		System.arraycopy(cadenceSerie, 0, serieData.cadenceSerie, 0, serieLength);
		System.arraycopy(distanceSerie, 0, serieData.distanceSerie, 0, serieLength);
		System.arraycopy(pulseSerie, 0, serieData.pulseSerie, 0, serieLength);
		System.arraycopy(temperatureSerie, 0, serieData.temperatureSerie, 0, serieLength);
		System.arraycopy(timeSerie, 0, serieData.timeSerie, 0, serieLength);

		// System.arraycopy(speedSerie, 0, serieData.speedSerie, 0,
		// serieLength);
		// System.arraycopy(powerSerie, 0, serieData.powerSerie, 0,
		// serieLength);
	}
}
