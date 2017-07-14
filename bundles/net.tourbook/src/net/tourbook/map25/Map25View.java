/*******************************************************************************
 * Copyright (C) 2005, 2017 Wolfgang Schramm and Contributors
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
package net.tourbook.map25;

import gnu.trove.list.array.TIntArrayList;

import java.awt.BorderLayout;
import java.awt.Canvas;
import java.awt.Frame;
import java.util.ArrayList;
import java.util.HashMap;

import net.tourbook.application.TourbookPlugin;
import net.tourbook.chart.Chart;
import net.tourbook.chart.ChartDataModel;
import net.tourbook.chart.SelectionChartInfo;
import net.tourbook.chart.SelectionChartXSliderPosition;
import net.tourbook.common.tooltip.ActionToolbarSlideout;
import net.tourbook.common.tooltip.IOpeningDialog;
import net.tourbook.common.tooltip.OpenDialogManager;
import net.tourbook.common.tooltip.ToolbarSlideout;
import net.tourbook.common.util.Util;
import net.tourbook.data.TourData;
import net.tourbook.importdata.RawDataManager;
import net.tourbook.map2.Messages;
import net.tourbook.map25.action.ActionSelectMap25Provider;
import net.tourbook.map25.action.ActionShowEntireTour;
import net.tourbook.map25.action.ActionSynchMapWithChartSlider;
import net.tourbook.map25.action.ActionSynchMapWithTour;
import net.tourbook.map25.ui.SlideoutMap25Options;
import net.tourbook.map25.ui.SlideoutTourTrackConfig;
import net.tourbook.photo.PhotoSelection;
import net.tourbook.tour.ITourEventListener;
import net.tourbook.tour.SelectionDeletedTours;
import net.tourbook.tour.SelectionTourData;
import net.tourbook.tour.SelectionTourId;
import net.tourbook.tour.SelectionTourIds;
import net.tourbook.tour.SelectionTourMarker;
import net.tourbook.tour.TourEvent;
import net.tourbook.tour.TourEventId;
import net.tourbook.tour.TourManager;
import net.tourbook.ui.views.tourCatalog.SelectionTourCatalogView;

import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.awt.SWT_AWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.part.ViewPart;
import org.oscim.core.BoundingBox;
import org.oscim.core.GeoPoint;
import org.oscim.core.MapPosition;
import org.oscim.map.Animator;
import org.oscim.map.Map;
import org.oscim.utils.Easing;

import de.byteholder.gpx.PointOfInterest;

public class Map25View extends ViewPart {

// SET_FORMATTING_OFF
	
	public static final String		ID										= "net.tourbook.map25.Map25View";				//$NON-NLS-1$
	
	private static final String		STATE_IS_TOUR_VISIBLE					= "STATE_IS_TOUR_VISIBLE";						//$NON-NLS-1$
	private static final String		STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER	= "STATE_SYNCH_MAP_WITH_CHART_SLIDER";			//$NON-NLS-1$
	private static final String		STATE_IS_SYNCH_MAP_WITH_TOUR			= "STATE_SYNCH_MAP_WITH_TOUR";					//$NON-NLS-1$
	
	private static final ImageDescriptor	_actionImageDescriptor			= TourbookPlugin.getImageDescriptor(Messages.image_action_show_tour_in_map);
	private static final ImageDescriptor	_actionImageDescriptorDisabled	= TourbookPlugin.getImageDescriptor(Messages.image_action_show_tour_in_map_disabled);
	
// SET_FORMATTING_ON

	private static final IDialogSettings	_state									= TourbookPlugin.getState(ID);

	private Map25App						_mapApp;

	private OpenDialogManager				_openDlgMgr								= new OpenDialogManager();

	private boolean							_isPartVisible;

	private IPartListener2					_partListener;
	private ISelectionListener				_postSelectionListener;
	private ITourEventListener				_tourEventListener;

	private ISelection						_lastHiddenSelection;
	private ISelection						_selectionWhenHidden;

	private ActionMap25Options				_actionMap25Options;
	private ActionSelectMap25Provider		_actionSelectMapProvider;
	private ActionSynchMapWithChartSlider	_actionSynchMapWithChartSlider;
	private ActionSynchMapWithTour			_actionSynchMapWithTour;
	private ActionShowEntireTour			_actionShowEntireTour;
	private ActionTourTrackConfig			_actionTourTrackConfig;

	private ArrayList<TourData>				_allTourData							= new ArrayList<>();
	private TIntArrayList					_allTourStarts							= new TIntArrayList();
	private GeoPoint[]						_allGeoPoints;
	private BoundingBox						_allBoundingBox;

	private int								_hashTourId;
	private int								_hashTourData;

	private boolean							_isSynchMapWithChartSlider;
	private boolean							_isSynchMapWithTour;

	/*
	 * UI controls
	 */
	private Composite						_swtContainer;
	private Composite						_parent;

	private class ActionMap25Options extends ActionToolbarSlideout {

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutMap25Options(_parent, toolbar, Map25View.this);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}
	}

	private class ActionTourTrackConfig extends ActionToolbarSlideout {

		public ActionTourTrackConfig() {

			super(_actionImageDescriptor, _actionImageDescriptorDisabled);

			isToggleAction = true;
			notSelectedTooltip = Messages.map_action_show_tour_in_map;
		}

		@Override
		protected ToolbarSlideout createSlideout(final ToolBar toolbar) {

			return new SlideoutTourTrackConfig(_parent, toolbar, Map25View.this);
		}

		@Override
		protected void onBeforeOpenSlideout() {
			closeOpenedDialogs(this);
		}

		@Override
		protected void onSelect() {

			super.onSelect();

			actionShowTour(getSelection());
		}
	}

	public void actionShowTour(final boolean isTrackVisible) {

		_mapApp.getTourLayer().setEnabled(isTrackVisible);
		_mapApp.getMap().render();

		updateActionsState();
	}

	public void actionSynchMapPositionWithSlider() {

		final boolean isSync = _actionSynchMapWithChartSlider.isChecked();

		_isSynchMapWithChartSlider = isSync;

		if (isSync) {

			// ensure that the track sliders are displayed

//			_actionShowTrackSlider.setChecked(true);
		}

	}

	public void actionSynchMapViewWithTour() {

		_isSynchMapWithTour = _actionSynchMapWithTour.isChecked();

		paintTours_AndUpdateMap();

//		if (_isMapSynchedWithTour) {
//
//			// force tour to be repainted, that it is synched immediately
//			_previousTourData = null;
//
//			_actionShowTourInMap.setChecked(true);
//			gdxMap.setShowOverlays(true);
//
//			paintTours_20_One(_allTourData.get(0), true);
//
//		} else {
//
//			// disable synch with slider
//			_isMapSynchedWithSlider = false;
//			_actionSynchWithSlider.setChecked(false);
//		}
	}

	public void actionZoomShowEntireTour() {

		if (_allBoundingBox == null) {

			// a tour is not yet displayed

			showToursFromTourProvider();

			return;
		}

		final Map map25 = _mapApp.getMap();

		map25.post(new Runnable() {

			@Override
			public void run() {

				final Animator animator = map25.animator();

				animator.cancel();
				animator.animateTo(//
						2000,
						_allBoundingBox,
						Easing.Type.SINE_INOUT,
						Animator.ANIM_MOVE | Animator.ANIM_SCALE);

				map25.updateMap(true);
			}
		});

	}

	private void addPartListener() {

		_partListener = new IPartListener2() {

			@Override
			public void partActivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partBroughtToTop(final IWorkbenchPartReference partRef) {}

			@Override
			public void partClosed(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {
					saveState();
				}
			}

			@Override
			public void partDeactivated(final IWorkbenchPartReference partRef) {}

			@Override
			public void partHidden(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {
					_isPartVisible = false;
				}
			}

			@Override
			public void partInputChanged(final IWorkbenchPartReference partRef) {}

			@Override
			public void partOpened(final IWorkbenchPartReference partRef) {}

			@Override
			public void partVisible(final IWorkbenchPartReference partRef) {
				if (partRef.getPart(false) == Map25View.this) {

					_isPartVisible = true;

					if (_lastHiddenSelection != null) {

						onSelectionChanged(_lastHiddenSelection);

						_lastHiddenSelection = null;
					}
				}
			}
		};
		getViewSite().getPage().addPartListener(_partListener);
	}

	/**
	 * listen for events when a tour is selected
	 */
	private void addSelectionListener() {

		_postSelectionListener = new ISelectionListener() {
			@Override
			public void selectionChanged(final IWorkbenchPart part, final ISelection selection) {
				onSelectionChanged(selection);
			}
		};

		getSite().getPage().addPostSelectionListener(_postSelectionListener);
	}

	private void addTourEventListener() {

		_tourEventListener = new ITourEventListener() {
			@Override
			public void tourChanged(final IWorkbenchPart part, final TourEventId eventId, final Object eventData) {

				if (part == Map25View.this) {
					return;
				}

				if (eventId == TourEventId.TOUR_CHART_PROPERTY_IS_MODIFIED) {

//					resetMap();

				} else if ((eventId == TourEventId.TOUR_CHANGED) && (eventData instanceof TourEvent)) {

//					final ArrayList<TourData> modifiedTours = ((TourEvent) eventData).getModifiedTours();
//					if ((modifiedTours != null) && (modifiedTours.size() > 0)) {
//
//						_allTourData.clear();
//						_allTourData.addAll(modifiedTours);
//
//						resetMap();
//					}

				} else if (eventId == TourEventId.UPDATE_UI || eventId == TourEventId.CLEAR_DISPLAYED_TOUR) {

//					clearView();

				} else if (eventId == TourEventId.MARKER_SELECTION) {

//					if (eventData instanceof SelectionTourMarker) {
//
//						onSelectionChanged_TourMarker((SelectionTourMarker) eventData, false);
//					}

				} else if ((eventId == TourEventId.TOUR_SELECTION) && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);

				} else if (eventId == TourEventId.SLIDER_POSITION_CHANGED && eventData instanceof ISelection) {

					onSelectionChanged((ISelection) eventData);
				}
			}
		};

		TourManager.getInstance().addTourEventListener(_tourEventListener);
	}

	private void clearView() {
		// TODO Auto-generated method stub

	}

	/**
	 * Close all opened dialogs except the opening dialog.
	 * 
	 * @param openingDialog
	 */
	public void closeOpenedDialogs(final IOpeningDialog openingDialog) {
		_openDlgMgr.closeOpenedDialogs(openingDialog);
	}

	private void createActions() {

		_actionMap25Options = new ActionMap25Options();
		_actionSelectMapProvider = new ActionSelectMap25Provider(this);
		_actionShowEntireTour = new ActionShowEntireTour(this);
		_actionSynchMapWithTour = new ActionSynchMapWithTour(this);
		_actionSynchMapWithChartSlider = new ActionSynchMapWithChartSlider(this);
		_actionTourTrackConfig = new ActionTourTrackConfig();
	}

	private BoundingBox createBoundingBox(final GeoPoint[] geoPoints) {

		// this is optimized for performance by using an array which BoundingBox do no support
		int minLat = Integer.MAX_VALUE;
		int minLon = Integer.MAX_VALUE;
		int maxLat = Integer.MIN_VALUE;
		int maxLon = Integer.MIN_VALUE;

		for (final GeoPoint geoPoint : geoPoints) {
			minLat = Math.min(minLat, geoPoint.latitudeE6);
			minLon = Math.min(minLon, geoPoint.longitudeE6);
			maxLat = Math.max(maxLat, geoPoint.latitudeE6);
			maxLon = Math.max(maxLon, geoPoint.longitudeE6);
		}

		return new BoundingBox(minLat, minLon, maxLat, maxLon);
	}

	@Override
	public void createPartControl(final Composite parent) {

		_parent = parent;

		createActions();
		fillActionBars();

		createUI(parent);

		addPartListener();
		addTourEventListener();
		addSelectionListener();

		showToursFromTourProvider();
	}

	private void createUI(final Composite parent) {

		_swtContainer = new Composite(parent, SWT.EMBEDDED | SWT.NO_BACKGROUND);
		final Frame awtContainer = SWT_AWT.new_Frame(_swtContainer);

		final Canvas awtCanvas = new Canvas();
		awtContainer.setLayout(new BorderLayout());
		awtCanvas.setIgnoreRepaint(true);

		awtContainer.add(awtCanvas);
		awtCanvas.setFocusable(true);
		awtCanvas.requestFocus();

		_mapApp = Map25App.createMap(this, _state, awtCanvas);
	}

	@Override
	public void dispose() {

		if (_partListener != null) {

			getViewSite().getPage().removePartListener(_partListener);

			_mapApp.stop();
		}

		super.dispose();
	}

	private void enableActions() {

	}

	private void fillActionBars() {

		/*
		 * fill view toolbar
		 */
		final IToolBarManager tbm = getViewSite().getActionBars().getToolBarManager();

		tbm.add(_actionTourTrackConfig);
		tbm.add(_actionShowEntireTour);
		tbm.add(_actionSynchMapWithTour);
		tbm.add(_actionSynchMapWithChartSlider);

		tbm.add(new Separator());

		tbm.add(_actionMap25Options);
		tbm.add(_actionSelectMapProvider);

		/*
		 * fill view menu
		 */
//		final IMenuManager menuMgr = getViewSite().getActionBars().getMenuManager();

//		fillMapContextMenu(menuMgr);
	}

	public Map25App getMapApp() {
		return _mapApp;
	}

	public void onModifyConfig() {
		// TODO Auto-generated method stub

	}

	private void onSelectionChanged(final ISelection selection) {

//		System.out.println((UI.timeStampNano() + " [" + getClass().getSimpleName() + "] ")
//				+ ("\tonSelectionChanged: " + selection));
//		// TODO remove SYSTEM.OUT.PRINTLN

		if (_isPartVisible == false) {

			if (selection instanceof SelectionTourData
					|| selection instanceof SelectionTourId
					|| selection instanceof SelectionTourIds) {

				// keep only selected tours
				_selectionWhenHidden = selection;
			}
			return;
		}

		if (selection instanceof SelectionTourData) {

			final SelectionTourData selectionTourData = (SelectionTourData) selection;
			final TourData tourData = selectionTourData.getTourData();

			paintTour(tourData);

		} else if (selection instanceof SelectionTourId) {

			final SelectionTourId tourIdSelection = (SelectionTourId) selection;
			final TourData tourData = TourManager.getInstance().getTourData(tourIdSelection.getTourId());

			paintTour(tourData);

		} else if (selection instanceof SelectionTourIds) {

			// paint all selected tours

			final ArrayList<Long> tourIds = ((SelectionTourIds) selection).getTourIds();
			if (tourIds.size() == 0) {

				// history tour (without tours) is displayed

			} else if (tourIds.size() == 1) {

				// only 1 tour is displayed, synch with this tour !!!

				final TourData tourData = TourManager.getInstance().getTourData(tourIds.get(0));

				paintTour(tourData);

			} else {

				// paint multiple tours

				paintTours(tourIds);

			}

		} else if (selection instanceof SelectionChartInfo) {

//			if (isTrackSliderVisible == false) {
//				return;
//			}

			final SelectionChartInfo chartInfo = (SelectionChartInfo) selection;

			final ChartDataModel chartDataModel = chartInfo.chartDataModel;
			if (chartDataModel != null) {

				final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
				if (tourId instanceof Long) {

					syncMapWith_ChartSlider(chartInfo, (Long) tourId);
				}
			}

		} else if (selection instanceof SelectionChartXSliderPosition) {

			final SelectionChartXSliderPosition xSliderPos = (SelectionChartXSliderPosition) selection;
			final Chart chart = xSliderPos.getChart();
			if (chart == null) {
				return;
			}

			final ChartDataModel chartDataModel = chart.getChartDataModel();

			final Object tourId = chartDataModel.getCustomData(Chart.CUSTOM_DATA_TOUR_ID);
			if (tourId instanceof Long) {

				final TourData tourData = TourManager.getInstance().getTourData((Long) tourId);
				if (tourData != null) {

					final int leftSliderValueIndex = xSliderPos.getLeftSliderValueIndex();
					int rightSliderValueIndex = xSliderPos.getRightSliderValueIndex();

					rightSliderValueIndex =
							rightSliderValueIndex == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
									? leftSliderValueIndex
									: rightSliderValueIndex;

					syncMapWith_ChartSlider(//
							tourData,
							leftSliderValueIndex,
							rightSliderValueIndex,
							leftSliderValueIndex);

					enableActions();
				}
			}

		} else if (selection instanceof SelectionTourMarker) {

//			final SelectionTourMarker markerSelection = (SelectionTourMarker) selection;
//
//			onSelectionChanged_TourMarker(markerSelection, true);

//		} else if (selection instanceof SelectionMapPosition) {
//
//			final SelectionMapPosition mapPositionSelection = (SelectionMapPosition) selection;
//
//			final int valueIndex1 = mapPositionSelection.getSlider1ValueIndex();
//			int valueIndex2 = mapPositionSelection.getSlider2ValueIndex();
//
//			valueIndex2 = valueIndex2 == SelectionChartXSliderPosition.IGNORE_SLIDER_POSITION
//					? valueIndex1
//					: valueIndex2;
//
//			positionMapTo_TourSliders(//
//					mapPositionSelection.getTourData(),
//					valueIndex1,
//					valueIndex2,
//					valueIndex1,
//					null);
//
//			enableActions();

		} else if (selection instanceof PointOfInterest) {

//			_isTourOrWayPoint = false;
//
//			clearView();
//
//			final PointOfInterest poi = (PointOfInterest) selection;
//
//			_poiPosition = poi.getPosition();
//			_poiName = poi.getName();
//
//			final String boundingBox = poi.getBoundingBox();
//			if (boundingBox == null) {
//				_poiZoomLevel = _map.getZoom();
//			} else {
//				_poiZoomLevel = _map.getZoom(boundingBox);
//			}
//
//			if (_poiZoomLevel == -1) {
//				_poiZoomLevel = _map.getZoom();
//			}
//
//			_map.setPoi(_poiPosition, _poiZoomLevel, _poiName);
//
//			_actionShowPOI.setChecked(true);
//
//			enableActions();

		} else if (selection instanceof StructuredSelection) {

//			final StructuredSelection structuredSelection = (StructuredSelection) selection;
//			final Object firstElement = structuredSelection.getFirstElement();
//
//			if (firstElement instanceof TVICatalogComparedTour) {
//
//				final TVICatalogComparedTour comparedTour = (TVICatalogComparedTour) firstElement;
//				final long tourId = comparedTour.getTourId();
//
//				final TourData tourData = TourManager.getInstance().getTourData(tourId);
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TVICompareResultComparedTour) {
//
//				final TVICompareResultComparedTour compareResultItem = (TVICompareResultComparedTour) firstElement;
//				final TourData tourData = TourManager.getInstance().getTourData(
//						compareResultItem.getComparedTourData().getTourId());
//				paintTours_20_One(tourData, false);
//
//			} else if (firstElement instanceof TourWayPoint) {
//
//				final TourWayPoint wp = (TourWayPoint) firstElement;
//
//				final TourData tourData = wp.getTourData();
//
//				paintTours_20_One(tourData, false);
//
//				_map.setPOI(_wayPointToolTipProvider, wp);
//
//				enableActions();
//			}
//
//			enableActions();

		} else if (selection instanceof PhotoSelection) {

//			paintPhotos(((PhotoSelection) selection).galleryPhotos);
//
//			enableActions();

		} else if (selection instanceof SelectionTourCatalogView) {

//			// show reference tour
//
//			final SelectionTourCatalogView tourCatalogSelection = (SelectionTourCatalogView) selection;
//
//			final TVICatalogRefTourItem refItem = tourCatalogSelection.getRefItem();
//			if (refItem != null) {
//
//				final TourData tourData = TourManager.getInstance().getTourData(refItem.getTourId());
//
//				paintTours_20_One(tourData, false);
//
//				enableActions();
//			}

		} else if (selection instanceof SelectionDeletedTours) {

			clearView();
		}
	}

	private void paintTour(final TourData tourData) {

		_allTourData.clear();
		_allTourData.add(tourData);

		paintTours_AndUpdateMap();
	}

	private void paintTours(final ArrayList<Long> tourIdList) {

		/*
		 * TESTING if a map redraw can be avoided, 15.6.2015
		 */
		final int tourIdsHashCode = tourIdList.hashCode();
		final int allToursHashCode = _allTourData.hashCode();
		if (tourIdsHashCode == _hashTourId && allToursHashCode == _hashTourData) {
			// skip redrawing
			return;
		}

		if (tourIdList.hashCode() != _hashTourId || _allTourData.hashCode() != _hashTourData) {

			// tour data needs to be loaded

			TourManager.loadTourData(tourIdList, _allTourData, true);

			_hashTourId = tourIdList.hashCode();
			_hashTourData = _allTourData.hashCode();
		}

		paintTours_AndUpdateMap();
	}

	private void paintTours_AndUpdateMap() {

		enableActions();
		updateActionsState();

		final TourLayer tourLayer = _mapApp.getTourLayer();
		if (tourLayer == null) {

			// tour layer is not yet created, this happened
			return;
		}

		int geoSize = 0;

		for (final TourData tourData : _allTourData) {

			// check if GPS data are available
			if (tourData.latitudeSerie != null) {
				geoSize += tourData.latitudeSerie.length;
			}
		}

		// use array to optimize performance when millions of points are created
		_allGeoPoints = new GeoPoint[geoSize];
		_allTourStarts.clear();

		int tourIndex = 0;
		int geoIndex = 0;

		for (final TourData tourData : _allTourData) {

			// check if GPS data are available
			if (tourData.latitudeSerie == null) {
				continue;
			}

			_allTourStarts.add(tourIndex);

			final double[] latitudeSerie = tourData.latitudeSerie;
			final double[] longitudeSerie = tourData.longitudeSerie;

			// create vtm geo points
			for (int serieIndex = 0; serieIndex < latitudeSerie.length; serieIndex++, tourIndex++) {
				_allGeoPoints[geoIndex++] = (new GeoPoint(latitudeSerie[serieIndex], longitudeSerie[serieIndex]));
			}
		}

		tourLayer.setPoints(_allGeoPoints, _allTourStarts);

		final Map map25 = _mapApp.getMap();

		map25.post(new Runnable() {

			@Override
			public void run() {

				// create outside isSynch that data are available when map is zoomed to show the whole tour
				_allBoundingBox = createBoundingBox(_allGeoPoints);

				if (_isSynchMapWithTour) {

					final Animator animator = map25.animator();

					animator.cancel();
					animator.animateTo(//
							2000,
							_allBoundingBox,
							Easing.Type.SINE_INOUT,
							Animator.ANIM_MOVE | Animator.ANIM_SCALE);
				}

				map25.updateMap(true);
			}
		});
	}

	void restoreState() {

		// tour
		final boolean isTourVisible = Util.getStateBoolean(_state, STATE_IS_TOUR_VISIBLE, true);
		_actionTourTrackConfig.setSelection(isTourVisible);
		_mapApp.getTourLayer().setEnabled(isTourVisible);

		// checkbox: synch map with tour
		final boolean isSynchTour = Util.getStateBoolean(_state, STATE_IS_SYNCH_MAP_WITH_TOUR, true);
		_actionSynchMapWithTour.setChecked(isSynchTour);
		_isSynchMapWithTour = isSynchTour;

		// checkbox: synch map with chart slider
		final boolean isSynchWithSlider = Util.getStateBoolean(_state, STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER, true);
		_actionSynchMapWithChartSlider.setChecked(isSynchWithSlider);
		_isSynchMapWithChartSlider = isSynchWithSlider;

		updateActionsState();
	}

	private void saveState() {

		final TourLayer tourLayer = _mapApp.getTourLayer();

		_state.put(STATE_IS_SYNCH_MAP_WITH_CHART_SLIDER, _actionSynchMapWithChartSlider.isChecked());
		_state.put(STATE_IS_SYNCH_MAP_WITH_TOUR, _actionSynchMapWithTour.isChecked());

		_state.put(STATE_IS_TOUR_VISIBLE, tourLayer.isEnabled());

		tourLayer.saveState(_state);
	}

	@Override
	public void setFocus() {

//		_swtContainer.setFocus();
	}

	private void showToursFromTourProvider() {

		Display.getCurrent().asyncExec(new Runnable() {
			@Override
			public void run() {

				// validate widget
				if (_swtContainer.isDisposed()) {
					return;
				}

				final ArrayList<TourData> tourDataList = TourManager.getSelectedTours();
				if (tourDataList != null) {

					_allTourData.clear();
					_allTourData.addAll(tourDataList);

					paintTours_AndUpdateMap();
				}

				updateActionsState();
			}
		});
	}

	private void syncMapWith_ChartSlider(final SelectionChartInfo chartInfo, final Long tourId) {

//		final TrackSliderLayer chartSliderLayer = getLayerTrackSlider();
//		if (chartSliderLayer == null) {
//			return;
//		}

		TourData tourData = TourManager.getInstance().getTourData(tourId);
		if (tourData == null) {

			// tour is not in the database, try to get it from the raw data manager

			final HashMap<Long, TourData> rawData = RawDataManager.getInstance().getImportedTours();
			tourData = rawData.get(tourId);
		}

		if (tourData == null || tourData.latitudeSerie == null) {

//			chartSliderLayer.setSliderVisible(false);

		} else {

			// sync map with chart slider

			final int valuesIndex = chartInfo.selectedSliderValuesIndex;

			syncMapWith_SliderPosition(tourData, /* chartSliderLayer, */ valuesIndex);

//			// update slider UI
//			updateTrackSlider_10_Position(//
//					tourData,
//					chartInfo.leftSliderValuesIndex,
//					chartInfo.rightSliderValuesIndex);

			updateActionsState();
		}
	}

	private void syncMapWith_ChartSlider(	final TourData tourData,
											final int leftSliderValuesIndex,
											final int rightSliderValuesIndex,
											final int selectedSliderIndex) {

//		final TrackSliderLayer chartSliderLayer = getLayerTrackSlider();
//		if (chartSliderLayer == null) {
//			return;
//		}

		if (tourData == null || tourData.latitudeSerie == null) {

//			chartSliderLayer.setSliderVisible(false);

		} else {

			// sync map with chart slider

			syncMapWith_SliderPosition(tourData, /* chartSliderLayer, */ selectedSliderIndex);

//			// update slider UI
//			updateTrackSlider_10_Position(//
//					tourData,
//					leftSliderValuesIndex,
//					rightSliderValuesIndex);

			updateActionsState();
		}
	}

	private void syncMapWith_SliderPosition(final TourData tourData,
//											final TrackSliderLayer chartSliderLayer,
											int valuesIndex) {

		final double[] latitudeSerie = tourData.latitudeSerie;

		// check bounds
		if (valuesIndex >= latitudeSerie.length) {
			valuesIndex = latitudeSerie.length;
		}

		final double latitude = latitudeSerie[valuesIndex];
		final double longitude = tourData.longitudeSerie[valuesIndex];

		final Map map25 = _mapApp.getMap();
		final MapPosition currentMapPos = new MapPosition();

		// get current position
		map25.viewport().getMapPosition(currentMapPos);

		// set new position
		currentMapPos.setPosition(latitude, longitude);

		// update map
		map25.setMapPosition(currentMapPos);
		map25.render();
	}

	/**
	 * Enable actions according to the available tours in {@link #_allTours}.
	 */
	void updateActionsState() {

		final TourLayer tourLayer = _mapApp.getTourLayer();

		final boolean isTourAvailable = _allTourData.size() > 0;
		final boolean isTrackLayerVisible = tourLayer == null ? false : tourLayer.isEnabled();
		final boolean isTrackSliderVisible = true;

		final boolean canShowTour = isTourAvailable && isTrackLayerVisible;

		_actionSynchMapWithChartSlider.setEnabled(canShowTour);
		_actionSynchMapWithTour.setEnabled(canShowTour);
		_actionShowEntireTour.setEnabled(canShowTour);

		_actionTourTrackConfig.setSelection(isTrackLayerVisible);
		_actionTourTrackConfig.setEnabled(isTourAvailable);

	}

	void updateUI_SelectedMapProvider(final Map25Provider selectedMapProvider) {

		_actionSelectMapProvider.updateUI_SelectedMapProvider(selectedMapProvider);
	}

}
