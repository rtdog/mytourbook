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
package net.tourbook.statistics;

import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.ChartDataSerie;
import net.tourbook.chart.ChartDataXSerie;
import net.tourbook.chart.ChartDataYSerie;
import net.tourbook.colors.GraphColorDefaults;
import net.tourbook.ui.UI;

public class StatisticWeekAltitude extends StatisticWeek {

	@Override
	void updateChart(TourDataWeek tourWeekData) {

		ChartDataModel chartModel = new ChartDataModel(ChartDataModel.CHART_TYPE_BAR);

		// set the x-axis
		ChartDataXSerie xData = new ChartDataXSerie(ProviderTourWeek.fAllWeeks);
		xData.setAxisUnit(ChartDataXSerie.AXIS_UNIT_NUMBER);
		chartModel.setXData(xData);

		// altitude
		ChartDataYSerie yData = new ChartDataYSerie(ChartDataModel.CHART_TYPE_BAR,
				ChartDataYSerie.BAR_LAYOUT_STACKED,
				tourWeekData.fAltitudeLow,
				tourWeekData.fAltitudeHigh);
		yData.setYTitle(Messages.LABEL_GRAPH_ALTITUDE);
		yData.setUnitLabel(UI.UNIT_LABEL_ALTITUDE);
		yData.setAxisUnit(ChartDataSerie.AXIS_UNIT_NUMBER);
		yData.setAllValueColors(0);
		yData.setVisibleMinValue(0);
		chartModel.addYData(yData);
		StatisticServices.setTourTypeColors(yData, GraphColorDefaults.PREF_GRAPH_ALTITUDE);
		StatisticServices.setTourTypeColorIndex(yData, tourWeekData.fTypeIds);

		if (fIsSynchScaleEnabled) {
			fMinMaxKeeper.setMinMaxValues(chartModel);
		}

		// show the fDataModel in the chart
		fChart.updateChart(chartModel);
	}
}
