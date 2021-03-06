/*******************************************************************************
 * Copyright (C) 2005, 2020 Wolfgang Schramm and Contributors
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
package net.tourbook.statistics.graphs;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.list.array.TLongArrayList;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.common.time.TimeTools;
import net.tourbook.common.time.TourDateTime;
import net.tourbook.common.util.SQL;
import net.tourbook.data.TourPerson;
import net.tourbook.data.TourType;
import net.tourbook.database.TourDatabase;
import net.tourbook.statistic.DurationTime;
import net.tourbook.statistics.StatisticServices;
import net.tourbook.tag.tour.filter.TourTagFilterSqlJoinBuilder;
import net.tourbook.ui.SQLFilter;
import net.tourbook.ui.TourTypeFilter;
import net.tourbook.ui.UI;

public class DataProvider_Tour_Day extends DataProvider {
   private static DataProvider_Tour_Day _instance;

   private TourData_Day                 _tourDayData;

   private boolean                      _isAdjustSamePosition;
   private boolean                      _isShowTrainingPerformance_AvgValue;

   private DataProvider_Tour_Day() {}

   public static DataProvider_Tour_Day getInstance() {

      if (_instance == null) {
         _instance = new DataProvider_Tour_Day();
      }

      return _instance;
   }

   private void adjustValues(final TFloatArrayList dbAllValues,
                             final float[] lowValues,
                             final float[] highValues,
                             final int sameDOY_FirstIndex,
                             final int sameDOY_LastIndex) {

      if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         float maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {
            maxValue += dbAllValues.get(valueIndex);
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = maxValue;
         }
      }
   }

   private void adjustValues(final TIntArrayList dbAllTourDuration,
                             final int[] duration_Low,
                             final int[] duration_High,
                             final int sameDOY_FirstIndex,
                             final int sameDOY_LastIndex) {

      if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         int maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {
            maxValue += dbAllTourDuration.get(valueIndex);
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            duration_Low[avgIndex] = 0;
            duration_High[avgIndex] = maxValue;
         }
      }
   }

   private void adjustValues_Avg(final TIntArrayList dbAllTourDuration,

                                 final TFloatArrayList dbAllValues,
                                 final float[] lowValues,
                                 final float[] highValues,

                                 final int sameDOY_FirstIndex,
                                 final int sameDOY_LastIndex) {

      if (_isShowTrainingPerformance_AvgValue && sameDOY_LastIndex != -1) {

         // compute average values

         double valueSquare = 0;
         double timeSquare = 0;

         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            final float value = dbAllValues.get(avgIndex);
            final float duration = dbAllTourDuration.get(avgIndex);

            // ignore 0 values
            if (value > 0) {

               valueSquare += value * duration;
               timeSquare += duration;
            }
         }

         final float avgValue = (float) (timeSquare == 0 ? 0 : valueSquare / timeSquare);

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = avgValue;
         }

      } else if (_isAdjustSamePosition && sameDOY_LastIndex != -1) {

         /*
          * This will ensure that a painted line graph do not move to the smallest value when it's
          * on the same day
          */

         float maxValue = 0;

         for (int valueIndex = sameDOY_FirstIndex; valueIndex <= sameDOY_LastIndex; valueIndex++) {

            final float value = dbAllValues.get(valueIndex);

            maxValue += value;
         }

         /*
          * Draw value to all points that the line graph is not moved to the bottom
          */
         for (int avgIndex = sameDOY_FirstIndex; avgIndex <= sameDOY_LastIndex; avgIndex++) {

            lowValues[avgIndex] = 0;
            highValues[avgIndex] = maxValue;
         }
      }
   }

   TourData_Day getDayData(final TourPerson person,
                           final TourTypeFilter tourTypeFilter,
                           final int lastYear,
                           final int numberOfYears,
                           final boolean refreshData,
                           final DurationTime durationTime) {

      // don't reload data which are already available
      if (person == _activePerson
            && tourTypeFilter == _activeTourTypeFilter
            && lastYear == _lastYear
            && numberOfYears == _numberOfYears
            && refreshData == false) {

         return _tourDayData;
      }

      String sql = null;

      try (Connection conn = TourDatabase.getInstance().getConnection()) {

         _activePerson = person;
         _activeTourTypeFilter = tourTypeFilter;

         _lastYear = lastYear;
         _numberOfYears = numberOfYears;

         initYearNumbers();

         int colorOffset = 0;
         if (tourTypeFilter.showUndefinedTourTypes()) {
            colorOffset = StatisticServices.TOUR_TYPE_COLOR_INDEX_OFFSET;
         }

         boolean isDurationTime_Break = false;
         boolean isDurationTime_Elapsed = false;
         boolean isDurationTime_Paused = false;
         boolean isDurationTime_Recorded = false;

         switch (durationTime) {
         case BREAK:
            isDurationTime_Break = true;
            break;

         case ELAPSED:
            isDurationTime_Elapsed = true;
            break;

         case PAUSED:
            isDurationTime_Paused = true;
            break;

         case RECORDED:
            isDurationTime_Recorded = true;
            break;

         case MOVING:
         default:
            // this is also the old implementation for the duration value
            break;
         }

         // get the tour types
         final ArrayList<TourType> tourTypeList = TourDatabase.getActiveTourTypes();
         final TourType[] tourTypes = tourTypeList.toArray(new TourType[tourTypeList.size()]);

         final SQLFilter sqlAppFilter = new SQLFilter(SQLFilter.TAG_FILTER);

         final TourTagFilterSqlJoinBuilder tagFilterSqlJoinBuilder = new TourTagFilterSqlJoinBuilder();

         sql = UI.EMPTY_STRING

               + "SELECT" + NL //                                       //$NON-NLS-1$

               + "   TourId," + NL //                                1  //$NON-NLS-1$

               + "   StartYear," + NL //                             2  //$NON-NLS-1$
               + "   StartWeek," + NL //                             3  //$NON-NLS-1$
               + "   TourStartTime," + NL //                         4  //$NON-NLS-1$
               + "   TimeZoneId," + NL //                            5  //$NON-NLS-1$

               + "   TourDeviceTime_Elapsed," + NL //                6  //$NON-NLS-1$
               + "   TourDeviceTime_Recorded," + NL //               7  //$NON-NLS-1$
               + "   TourDeviceTime_Paused," + NL //                 8  //$NON-NLS-1$
               + "   TourComputedTime_Moving," + NL //               9  //$NON-NLS-1$

               + "   TourDistance," + NL //                          10 //$NON-NLS-1$
               + "   TourAltUp," + NL //                             11 //$NON-NLS-1$
               + "   TourTitle," + NL //                             12 //$NON-NLS-1$
               + "   TourDescription," + NL //                       13 //$NON-NLS-1$

               + "   training_TrainingEffect_Aerob," + NL //         14 //$NON-NLS-1$
               + "   training_TrainingEffect_Anaerob," + NL //       15 //$NON-NLS-1$
               + "   training_TrainingPerformance," + NL //          16 //$NON-NLS-1$

               + "   TourType_typeId," + NL //                       17 //$NON-NLS-1$
               + "   jTdataTtag.TourTag_tagId" + NL //               18 //$NON-NLS-1$

               + "FROM " + TourDatabase.TABLE_TOUR_DATA + NL //         //$NON-NLS-1$

               // get/filter tag id's
               + tagFilterSqlJoinBuilder.getSqlTagJoinTable() + " jTdataTtag" //                //$NON-NLS-1$
               + " ON TourId = jTdataTtag.TourData_tourId" + NL //                              //$NON-NLS-1$

               + "WHERE StartYear IN (" + getYearList(lastYear, numberOfYears) + ")" + NL //    //$NON-NLS-1$ //$NON-NLS-2$
               + "   " + sqlAppFilter.getWhereClause() //$NON-NLS-1$

               + "ORDER BY TourStartTime" + NL; //                                              //$NON-NLS-1$

         final TLongArrayList dbAllTourIds = new TLongArrayList();

         final TIntArrayList dbAllYears = new TIntArrayList();
         final TIntArrayList dbAllMonths = new TIntArrayList();
         final TIntArrayList dbAllDays = new TIntArrayList();
         final TIntArrayList dbAllYearsDOY = new TIntArrayList(); // DOY...Day Of Year

         final TIntArrayList dbAllTourStartTime = new TIntArrayList();
         final TIntArrayList dbAllTourEndTime = new TIntArrayList();
         final TIntArrayList dbAllTourStartWeek = new TIntArrayList();
         final ArrayList<ZonedDateTime> dbAllTourStartDateTime = new ArrayList<>();

         final TIntArrayList dbAllTourDeviceTime_Elapsed = new TIntArrayList();
         final TIntArrayList dbAllTourDeviceTime_Recorded = new TIntArrayList();
         final TIntArrayList dbAllTourDeviceTime_Paused = new TIntArrayList();
         final TIntArrayList dbAllTourComputedTime_Moving = new TIntArrayList();
         final TIntArrayList dbAllTourDuration = new TIntArrayList();

         final TFloatArrayList dbAllDistance = new TFloatArrayList();
         final TFloatArrayList dbAllAvgSpeed = new TFloatArrayList();
         final TFloatArrayList dbAllAvgPace = new TFloatArrayList();
         final TFloatArrayList dbAllAltitudeUp = new TFloatArrayList();

         final TFloatArrayList dbAllTrain_Effect_Aerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Effect_Anaerob = new TFloatArrayList();
         final TFloatArrayList dbAllTrain_Performance = new TFloatArrayList();

         final ArrayList<String> dbAllTourTitle = new ArrayList<>();
         final ArrayList<String> dbAllTourDescription = new ArrayList<>();

         final TLongArrayList allTypeIds = new TLongArrayList();
         final TIntArrayList allTypeColorIndex = new TIntArrayList();

         final HashMap<Long, ArrayList<Long>> allTagIds = new HashMap<>();

         long lastTourId = -1;
         ArrayList<Long> tagIds = null;

         final PreparedStatement prepStmt = conn.prepareStatement(sql);

         int paramIndex = 1;
         paramIndex = tagFilterSqlJoinBuilder.setParameters(prepStmt, paramIndex);

         sqlAppFilter.setParameters(prepStmt, paramIndex);

         final ResultSet result = prepStmt.executeQuery();

         while (result.next()) {

            final long dbTourId = result.getLong(1);
            final Object dbTagId = result.getObject(18);

            if (dbTourId == lastTourId) {

               // get additional tags from tag join

               if (dbTagId instanceof Long) {
                  tagIds.add((Long) dbTagId);
               }

            } else {

               // get first record from a tour

// SET_FORMATTING_OFF

               final int dbTourYear                   = result.getShort(2);
               final int dbTourStartWeek              = result.getInt(3);

               final long dbStartTimeMilli            = result.getLong(4);
               final String dbTimeZoneId              = result.getString(5);

               final int dbElapsedTime                = result.getInt(6);
               final int dbRecordedTime               = result.getInt(7);
               final int dbPausedTime                 = result.getInt(8);
               final int dbMovingTime                 = result.getInt(9);

               final float dbDistance                 = result.getFloat(10);
               final int dbAltitudeUp                 = result.getInt(11);

               final String dbTourTitle               = result.getString(12);
               final String dbDescription             = result.getString(13);

               final float trainingEffect             = result.getFloat(14);
               final float trainingEffect_Anaerobic   = result.getFloat(15);
               final float trainingPerformance        = result.getFloat(16);

               final Object dbTypeIdObject            = result.getObject(17);

// SET_FORMATTING_ON

               final TourDateTime tourDateTime = TimeTools.createTourDateTime(dbStartTimeMilli, dbTimeZoneId);
               final ZonedDateTime zonedStartDateTime = tourDateTime.tourZonedDateTime;

               // get number for day of year, starts with 0
               final int tourDOY = tourDateTime.tourZonedDateTime.get(ChronoField.DAY_OF_YEAR) - 1;
               final int yearDOYs = getYearDOYs(dbTourYear);

               final int startDayTime = (zonedStartDateTime.getHour() * 3600)
                     + (zonedStartDateTime.getMinute() * 60)
                     + zonedStartDateTime.getSecond();

               int durationTimeValue = 0;

               if (isDurationTime_Break) {
                  durationTimeValue = dbElapsedTime - dbMovingTime;
               } else if (isDurationTime_Elapsed) {
                  durationTimeValue = dbElapsedTime;
               } else if (isDurationTime_Recorded) {
                  durationTimeValue = dbRecordedTime;
               } else if (isDurationTime_Paused) {
                  durationTimeValue = dbPausedTime;
               } else {
                  // moving time, this is also the old implementation for the duration value
                  durationTimeValue = dbMovingTime == 0 ? dbElapsedTime : dbMovingTime;
               }

               dbAllTourIds.add(dbTourId);

               dbAllYears.add(dbTourYear);
               dbAllMonths.add(zonedStartDateTime.getMonthValue());
               dbAllDays.add(zonedStartDateTime.getDayOfMonth());
               dbAllYearsDOY.add(yearDOYs + tourDOY);
               dbAllTourStartWeek.add(dbTourStartWeek);

               dbAllTourStartDateTime.add(zonedStartDateTime);
               dbAllTourStartTime.add(startDayTime);
               dbAllTourEndTime.add((startDayTime + dbElapsedTime));

               dbAllTourDeviceTime_Elapsed.add(dbElapsedTime);
               dbAllTourDeviceTime_Recorded.add(dbRecordedTime);
               dbAllTourDeviceTime_Paused.add(dbPausedTime);
               dbAllTourComputedTime_Moving.add(dbMovingTime);

               dbAllTourDuration.add(durationTimeValue);

               // round distance
               final float distance = dbDistance / UI.UNIT_VALUE_DISTANCE;

               dbAllDistance.add(distance);
               dbAllAltitudeUp.add(dbAltitudeUp / UI.UNIT_VALUE_ALTITUDE);

               dbAllAvgPace.add(distance == 0 ? 0 : dbMovingTime * 1000f / distance / 60.0f);
               dbAllAvgSpeed.add(dbMovingTime == 0 ? 0 : 3.6f * distance / dbMovingTime);

               dbAllTrain_Effect_Aerob.add(trainingEffect);
               dbAllTrain_Effect_Anaerob.add(trainingEffect_Anaerobic);
               dbAllTrain_Performance.add(trainingPerformance);

               dbAllTourTitle.add(dbTourTitle);
               dbAllTourDescription.add(dbDescription == null ? UI.EMPTY_STRING : dbDescription);

               if (dbTagId instanceof Long) {

                  tagIds = new ArrayList<>();
                  tagIds.add((Long) dbTagId);

                  allTagIds.put(dbTourId, tagIds);
               }

               /*
                * Convert type id to the type index in the tour types list which is also the color
                * index
                */
               int colorIndex = 0;
               long dbTypeId = TourDatabase.ENTITY_IS_NOT_SAVED;

               if (dbTypeIdObject instanceof Long) {

                  dbTypeId = (Long) dbTypeIdObject;

                  for (int typeIndex = 0; typeIndex < tourTypes.length; typeIndex++) {
                     if (dbTypeId == tourTypes[typeIndex].getTypeId()) {
                        colorIndex = colorOffset + typeIndex;
                        break;
                     }
                  }
               }

               allTypeColorIndex.add(colorIndex);
               allTypeIds.add(dbTypeId);
            }

            lastTourId = dbTourId;
         }

         final int[] allYearsDOY = dbAllYearsDOY.toArray();

         final int[] duration_High = dbAllTourDuration.toArray();
         final int serieLength = duration_High.length;

         final float[] altitude_High = dbAllAltitudeUp.toArray();
         final float[] avgPace_High = dbAllAvgPace.toArray();
         final float[] avgSpeed_High = dbAllAvgSpeed.toArray();
         final float[] distance_High = dbAllDistance.toArray();

         final float[] trainEffect_Aerob_High = dbAllTrain_Effect_Aerob.toArray();
         final float[] trainEffect_Anaerob_High = dbAllTrain_Effect_Anaerob.toArray();
         final float[] trainPerformance_High = dbAllTrain_Performance.toArray();

         final int[] duration_Low = new int[serieLength];
         final float[] altitude_Low = new float[serieLength];
         final float[] avgPace_Low = new float[serieLength];
         final float[] avgSpeed_Low = new float[serieLength];
         final float[] distance_Low = new float[serieLength];

         final float[] trainEffect_Aerob_Low = new float[serieLength];
         final float[] trainEffect_Anaerob_Low = new float[serieLength];
         final float[] trainPerformance_Low = new float[serieLength];

         /*
          * Adjust low/high values when a day has multiple tours
          */
         int prevTourDOY = -1;

         int sameDOY_FirstIndex = -1;
         int sameDOY_LastIndex = -1;

         for (int tourIndex = 0; tourIndex < allYearsDOY.length; tourIndex++) {

            final int tourDOY = allYearsDOY[tourIndex];

            if (prevTourDOY == tourDOY) {

               // current tour is at the same day as the previous tour

               sameDOY_LastIndex = tourIndex;

               if (sameDOY_FirstIndex == -1) {

                  // use previous index as first time slice
                  sameDOY_FirstIndex = tourIndex - 1;
               }

// SET_FORMATTING_OFF

               duration_High[tourIndex]               += duration_Low[tourIndex]             = duration_High[tourIndex - 1];

               altitude_High[tourIndex]               += altitude_Low[tourIndex]             = altitude_High[tourIndex - 1];
               avgPace_High[tourIndex]                += avgPace_Low[tourIndex]              = avgPace_High[tourIndex - 1];
               avgSpeed_High[tourIndex]               += avgSpeed_Low[tourIndex]             = avgSpeed_High[tourIndex - 1];
               distance_High[tourIndex]               += distance_Low[tourIndex]             = distance_High[tourIndex - 1];

               trainEffect_Aerob_High[tourIndex]      += trainEffect_Aerob_Low[tourIndex]    = trainEffect_Aerob_High[tourIndex - 1];
               trainEffect_Anaerob_High[tourIndex]    += trainEffect_Anaerob_Low[tourIndex]  = trainEffect_Anaerob_High[tourIndex - 1];
               trainPerformance_High[tourIndex]       += trainPerformance_Low[tourIndex]     = trainPerformance_High[tourIndex - 1];

// SET_FORMATTING_ON

            } else {

               // current tour is at another day as the tour before

               prevTourDOY = tourDOY;

// SET_FORMATTING_OFF

               adjustValues(dbAllTourDuration,           duration_Low,  duration_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

               adjustValues(dbAllDistance,               distance_Low,  distance_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllAltitudeUp,             altitude_Low,  altitude_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllAvgPace,                avgPace_Low,   avgPace_High,     sameDOY_FirstIndex,  sameDOY_LastIndex);
               adjustValues(dbAllAvgSpeed,               avgSpeed_Low,  avgSpeed_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

               adjustValues(dbAllTrain_Effect_Aerob,     trainEffect_Aerob_Low,     trainEffect_Aerob_High,    sameDOY_FirstIndex,     sameDOY_LastIndex);
               adjustValues(dbAllTrain_Effect_Anaerob,   trainEffect_Anaerob_Low,   trainEffect_Anaerob_High,  sameDOY_FirstIndex,     sameDOY_LastIndex);

               adjustValues_Avg(dbAllTourDuration,       dbAllTrain_Performance,    trainPerformance_Low,      trainPerformance_High,  sameDOY_FirstIndex,  sameDOY_LastIndex);

// SET_FORMATTING_ON

               sameDOY_FirstIndex = -1;
               sameDOY_LastIndex = -1;
            }
         }

         // compute for the last values

// SET_FORMATTING_OFF

         adjustValues(dbAllTourDuration,           duration_Low,  duration_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllDistance,               distance_Low,  distance_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAltitudeUp,             altitude_Low,  altitude_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgPace,                avgPace_Low,   avgPace_High,     sameDOY_FirstIndex,  sameDOY_LastIndex);
         adjustValues(dbAllAvgSpeed,               avgSpeed_Low,  avgSpeed_High,    sameDOY_FirstIndex,  sameDOY_LastIndex);

         adjustValues(dbAllTrain_Effect_Aerob,     trainEffect_Aerob_Low,     trainEffect_Aerob_High,    sameDOY_FirstIndex,     sameDOY_LastIndex);
         adjustValues(dbAllTrain_Effect_Anaerob,   trainEffect_Anaerob_Low,   trainEffect_Anaerob_High,  sameDOY_FirstIndex,     sameDOY_LastIndex);

         adjustValues_Avg(dbAllTourDuration,       dbAllTrain_Performance,    trainPerformance_Low,      trainPerformance_High,  sameDOY_FirstIndex,  sameDOY_LastIndex);

//SET_FORMATTING_ON

         // get number of days for all years
         int yearDays = 0;
         for (final int doy : allYearDays) {
            yearDays += doy;
         }

         _tourDayData = new TourData_Day();

         _tourDayData.allTourIds = dbAllTourIds.toArray();

         _tourDayData.allYears = dbAllYears.toArray();
         _tourDayData.allMonths = dbAllMonths.toArray();
         _tourDayData.allDays = dbAllDays.toArray();
         _tourDayData.setDoyValues(allYearsDOY);
         _tourDayData.allWeeks = dbAllTourStartWeek.toArray();

         _tourDayData.allDaysInAllYears = yearDays;
         _tourDayData.allYearDays = allYearDays;
         _tourDayData.allYearNumbers = allYearNumbers;

         _tourDayData.allTypeIds = allTypeIds.toArray();
         _tourDayData.allTypeColorIndices = allTypeColorIndex.toArray();

         _tourDayData.tagIds = allTagIds;

         _tourDayData.setDurationLow(duration_Low);
         _tourDayData.setDurationHigh(duration_High);

         _tourDayData.allElevation_Low = altitude_Low;
         _tourDayData.allElevation_High = altitude_High;
         _tourDayData.allDistance_Low = distance_Low;
         _tourDayData.allDistance_High = distance_High;

         _tourDayData.allAvgPace_Low = avgPace_Low;
         _tourDayData.allAvgPace_High = avgPace_High;
         _tourDayData.allAvgSpeed_Low = avgSpeed_Low;
         _tourDayData.allAvgSpeed_High = avgSpeed_High;

         _tourDayData.allTrainingEffect_Aerob_Low = trainEffect_Aerob_Low;
         _tourDayData.allTrainingEffect_Aerob_High = trainEffect_Aerob_High;
         _tourDayData.allTrainingEffect_Anaerob_Low = trainEffect_Anaerob_Low;
         _tourDayData.allTrainingEffect_Anaerob_High = trainEffect_Anaerob_High;
         _tourDayData.allTrainingPerformance_Low = trainPerformance_Low;
         _tourDayData.allTrainingPerformance_High = trainPerformance_High;

         _tourDayData.allStartTime = dbAllTourStartTime.toArray();
         _tourDayData.allEndTime = dbAllTourEndTime.toArray();
         _tourDayData.allStartDateTimes = dbAllTourStartDateTime;

         _tourDayData.allDistance = dbAllDistance.toArray();
         _tourDayData.allAltitude = dbAllAltitudeUp.toArray();

         _tourDayData.allTraining_Effect = dbAllTrain_Effect_Aerob.toArray();
         _tourDayData.allTraining_Effect_Anaerobic = dbAllTrain_Effect_Anaerob.toArray();
         _tourDayData.allTraining_Performance = dbAllTrain_Performance.toArray();

         _tourDayData.allDeviceTime_Elapsed = dbAllTourDeviceTime_Elapsed.toArray();
         _tourDayData.allDeviceTime_Recorded = dbAllTourDeviceTime_Recorded.toArray();
         _tourDayData.allDeviceTime_Paused = dbAllTourDeviceTime_Paused.toArray();
         _tourDayData.allComputedTime_Moving = dbAllTourComputedTime_Moving.toArray();

         _tourDayData.allTourTitles = dbAllTourTitle;
         _tourDayData.allTourDescriptions = dbAllTourDescription;

         setStatisticValues();

      } catch (final SQLException e) {
         SQL.showException(e, sql);
      }

      return _tourDayData;
   }

   public void setGraphContext(final boolean isShowTrainingPerformance_AvgValue, final boolean isAdjustmentSamePosition) {

      _isShowTrainingPerformance_AvgValue = isShowTrainingPerformance_AvgValue;
      _isAdjustSamePosition = isAdjustmentSamePosition;
   }

   private void setStatisticValues() {

      final StringBuilder sb = new StringBuilder();

      final String headerLine1 = UI.EMPTY_STRING

            + HEAD1_DATE_YEAR
            + HEAD1_DATE_MONTH
            + HEAD1_DATE_DAY
            + HEAD1_DATE_DOY

            + HEAD1_DEVICE_TIME_ELAPSED
            + HEAD1_DEVICE_TIME_RECORDED
            + HEAD1_DEVICE_TIME_PAUSED

            + HEAD1_COMPUTED_TIME_MOVING
            + HEAD1_COMPUTED_TIME_BREAK

            + HEAD1_DURATION_LOW
            + HEAD1_DURATION_HIGH
            + HEAD1_ELEVATION_LOW
            + HEAD1_ELEVATION_HIGH
            + HEAD1_DISTANCE_LOW
            + HEAD1_DISTANCE_HIGH
            + HEAD1_SPEED_LOW
            + HEAD1_SPEED_HIGH
            + HEAD1_PACE_LOW
            + HEAD1_PACE_HIGH

            + HEAD1_TRAINING_AEROB_LOW
            + HEAD1_TRAINING_AEROB_HIGH
            + HEAD1_TRAINING_ANAEROB_LOW
            + HEAD1_TRAINING_ANAEROB_HIGH
            + HEAD1_TRAINING_PERFORMANCE_LOW
            + HEAD1_TRAINING_PERFORMANCE_HIGH;

      final String headerLine2 = UI.EMPTY_STRING

            + HEAD2_DATE_YEAR
            + HEAD2_DATE_MONTH
            + HEAD2_DATE_DAY
            + HEAD2_DATE_DOY

            + HEAD2_DEVICE_TIME_ELAPSED
            + HEAD2_DEVICE_TIME_RECORDED
            + HEAD2_DEVICE_TIME_PAUSED

            + HEAD2_COMPUTED_TIME_MOVING
            + HEAD2_COMPUTED_TIME_BREAK

            + HEAD2_DURATION_LOW
            + HEAD2_DURATION_HIGH
            + HEAD2_ELEVATION_LOW
            + HEAD2_ELEVATION_HIGH
            + HEAD2_DISTANCE_LOW
            + HEAD2_DISTANCE_HIGH

            + HEAD2_SPEED_LOW
            + HEAD2_SPEED_HIGH
            + HEAD2_PACE_LOW
            + HEAD2_PACE_HIGH

            + HEAD2_TRAINING_AEROB_LOW
            + HEAD2_TRAINING_AEROB_HIGH
            + HEAD2_TRAINING_ANAEROB_LOW
            + HEAD2_TRAINING_ANAEROB_HIGH
            + HEAD2_TRAINING_PERFORMANCE_LOW
            + HEAD2_TRAINING_PERFORMANCE_HIGH

      ;

      final String valueFormatting = UI.EMPTY_STRING

            // date
            + VALUE_DATE_YEAR
            + VALUE_DATE_MONTH
            + VALUE_DATE_DAY
            + VALUE_DATE_DOY

            // device time
            + VALUE_DEVICE_TIME_ELAPSED
            + VALUE_DEVICE_TIME_RECORDED
            + VALUE_DEVICE_TIME_PAUSED

            // computed time
            + VALUE_COMPUTED_TIME_MOVING
            + VALUE_COMPUTED_TIME_BREAK

            + VALUE_DURATION_LOW
            + VALUE_DURATION_HIGH
            + VALUE_ELEVATION_LOW
            + VALUE_ELEVATION_HIGH
            + VALUE_DISTANCE_LOW
            + VALUE_DISTANCE_HIGH

            + VALUE_SPEED_LOW
            + VALUE_SPEED_HIGH
            + VALUE_PACE_LOW
            + VALUE_PACE_HIGH

            + VALUE_TRAINING_AEROB_LOW
            + VALUE_TRAINING_AEROB_HIGH
            + VALUE_TRAINING_ANAEROB_LOW
            + VALUE_TRAINING_ANAEROB_HIGH
            + VALUE_TRAINING_PERFORMANCE_LOW
            + VALUE_TRAINING_PERFORMANCE_HIGH

            + NL;

      sb.append(headerLine1 + NL);
      sb.append(headerLine2 + NL);

      final float[] durationLow = _tourDayData.getDurationLowFloat();
      final float[] durationHigh = _tourDayData.getDurationHighFloat();
      final int[] doyValues = _tourDayData.getDoyValues();

      final int numDataItems = durationLow.length;

      // set initial value
      int prevMonth = numDataItems > 0 ? _tourDayData.allMonths[0] : 0;

      for (int dataIndex = 0; dataIndex < numDataItems; dataIndex++) {

         final int month = _tourDayData.allMonths[dataIndex];

         // group by month
         if (month != prevMonth) {
            prevMonth = month;
            sb.append(NL);
         }

         final int elapsedTime = _tourDayData.allDeviceTime_Elapsed[dataIndex];
         final int movingTime = _tourDayData.allComputedTime_Moving[dataIndex];
         final int breakTime = elapsedTime - movingTime;

         sb.append(String.format(valueFormatting,

               _tourDayData.allYears[dataIndex],
               month,
               _tourDayData.allDays[dataIndex],
               doyValues[dataIndex],

               elapsedTime,
               _tourDayData.allDeviceTime_Recorded[dataIndex],
               _tourDayData.allDeviceTime_Paused[dataIndex],
               movingTime,
               breakTime,

               durationLow[dataIndex],
               durationHigh[dataIndex],

               _tourDayData.allElevation_Low[dataIndex],
               _tourDayData.allElevation_High[dataIndex],

               _tourDayData.allDistance_Low[dataIndex],
               _tourDayData.allDistance_High[dataIndex],

               _tourDayData.allAvgSpeed_Low[dataIndex],
               _tourDayData.allAvgSpeed_High[dataIndex],

               _tourDayData.allAvgPace_Low[dataIndex],
               _tourDayData.allAvgPace_High[dataIndex],

               _tourDayData.allTrainingEffect_Aerob_Low[dataIndex],
               _tourDayData.allTrainingEffect_Aerob_High[dataIndex],
               _tourDayData.allTrainingEffect_Anaerob_Low[dataIndex],
               _tourDayData.allTrainingEffect_Anaerob_High[dataIndex],
               _tourDayData.allTrainingPerformance_Low[dataIndex],
               _tourDayData.allTrainingPerformance_High[dataIndex]

         ));
      }

      _tourDayData.statisticValuesRaw = sb.toString();
   }
}
