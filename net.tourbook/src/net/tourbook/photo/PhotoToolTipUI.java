/*******************************************************************************
 * Copyright (C) 2005, 2012  Wolfgang Schramm and Contributors
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
package net.tourbook.photo;

import java.util.ArrayList;

import net.tourbook.ui.tourChart.ChartPhoto;

import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Shell;

/**
 * Photo tooltip UI for a tour chart or map or something unknown
 * 
 * @author Wolfgang Schramm, created 3.8.2012
 */

public abstract class PhotoToolTipUI extends PhotoToolTipShell {

	private ArrayList<ChartPhoto>			_hoveredPhotos;

	private int								_hoveredPhotosHash;
	private int								_displayedPhotosHash;

	private final ArrayList<PhotoWrapper>	_photoWrapperList	= new ArrayList<PhotoWrapper>();

	private PhotoToolTipImageGallery		_imageGallery;

	private Color							_fgColor;
	private Color							_bgColor;

	/*
	 * UI controls
	 */

	private final class PhotoGalleryProvider implements IPhotoGalleryProvider {
		@Override
		public IStatusLineManager getStatusLineManager() {
			return null;
		}

		@Override
		public IToolBarManager getToolBarManager() {
			return null;
		}

		@Override
		public void registerContextMenu(final String menuId, final MenuManager menuManager) {}

		@Override
		public void setSelection(final PhotoSelection photoSelection) {}
	}

	public class PhotoToolTipImageGallery extends ImageGallery {

		public PhotoToolTipImageGallery(final Composite parent,
										final int style,
										final PhotoGalleryProvider photoGalleryProvider) {
			super(parent, style, photoGalleryProvider);
		}
	}

	public PhotoToolTipUI(final Control ownerControl) {

		super(ownerControl);

		initUI(ownerControl);
	}

	@Override
	protected Composite createToolTipContentArea(final Event event, final Composite shell) {

		final Composite container = createUI(shell);

		updateUI_Colors(shell);

		_imageGallery.showInfo(false, null, true, true);

		super.setImageGallery(_imageGallery);

		return container;
	}

	private Composite createUI(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().numColumns(1).spacing(2, 2).applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_BLUE));
		{
//			createUI_50_Info(container);
			createUI_10_Gallery(container);
		}

		return container;
	}

	private void createUI_10_Gallery(final Composite parent) {

		final Composite container = new Composite(parent, SWT.NONE);
		GridDataFactory.fillDefaults()//
//				.hint(SWT.DEFAULT, 150)
				.hint(330, 100)
//				.hint(1000, 70)
				.grab(true, true)
				.applyTo(container);
		GridLayoutFactory.fillDefaults()//
				.numColumns(1)
				.extendedMargins(1, 1, 0, 0)
				.applyTo(container);
//		container.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GREEN));
		{
			_imageGallery = new PhotoToolTipImageGallery(
					container,
					SWT.H_SCROLL | SWT.MULTI,
					new PhotoGalleryProvider());

			_imageGallery.showInfo(false, null, true, true);
		}

	}

	public abstract int getHideCounter();

	public abstract ArrayList<ChartPhoto> getHoveredPhotos();



	@Override
	protected Object getToolTipArea(final Event event) {

		final int hideCounter = getHideCounter();

		return hideCounter;
	}

	public abstract void incrementHideCounter();

	private void initUI(final Control control) {

//		_pc = new PixelConverter(control);
	}

	protected void restoreState(final IDialogSettings state) {

		_imageGallery.restoreState(state);
	}

	protected void saveState(final IDialogSettings state) {

	}

	@Override
	protected boolean shouldCreateToolTip(final Event event) {

		_hoveredPhotos = getHoveredPhotos();

		if (super.shouldCreateToolTip(event) == false) {
			return false;
		}

		final boolean isPhotoHovered = _hoveredPhotos != null && _hoveredPhotos.size() > 0;

		return isPhotoHovered;
	}

	protected void showPhotoToolTip(final Point devPositionHoveredValue) {

		final boolean isPhotoHovered = _hoveredPhotos != null && _hoveredPhotos.size() > 0;
		_hoveredPhotosHash = isPhotoHovered ? _hoveredPhotos.hashCode() : 0;

		/*
		 * show/locate shell
		 */
		boolean isNewImages;
		final Shell ttShell = getToolTipShell();
		if (ttShell == null || ttShell.isDisposed()) {

			if (show(devPositionHoveredValue) == false) {
				return;
			}

			isNewImages = true;

		} else {

			isNewImages = false;

			// move tooltip
			setTTShellLocation();

			// check if new images should be displayed
			if (_displayedPhotosHash == _hoveredPhotosHash) {
				return;
			}
		}

		updateUI(isNewImages);

		_displayedPhotosHash = _hoveredPhotosHash;
	}

	/**
	 * update UI
	 * 
	 * @param isNewImages
	 */
	private void updateUI(final boolean isNewImages) {

		/*
		 * display photo images
		 */
		// create list containing all images
		_photoWrapperList.clear();
		for (final ChartPhoto chartPhoto : _hoveredPhotos) {
			_photoWrapperList.add(chartPhoto.photoWrapper);
		}

		final String galleryPositionKey = _hoveredPhotosHash + "_PhotoToolTipUI";//$NON-NLS-1$

		if (isNewImages) {
			_imageGallery.showImages(_photoWrapperList, galleryPositionKey, false);
		} else {
			// do not hide already displayed images
			_imageGallery.showImages(_photoWrapperList, galleryPositionKey);
		}
	}

	private void updateUI_Colors(final Composite parent) {

		final ColorRegistry colorRegistry = JFaceResources.getColorRegistry();
		_fgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_FOREGROUND);
		_bgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_BACKGROUND);
		final Color selectionFgColor = colorRegistry.get(IPhotoPreferences.PHOTO_VIEWER_COLOR_SELECTION_FOREGROUND);

		final Color noFocusSelectionFgColor = Display.getCurrent().getSystemColor(SWT.COLOR_TITLE_INACTIVE_BACKGROUND);

		_imageGallery.updateColors(_fgColor, _bgColor, selectionFgColor, noFocusSelectionFgColor, true);

		updateUI_Colors_ChildColors(parent);
	}

	/**
	 * !!! This is recursive !!!
	 * 
	 * @param child
	 */
	private void updateUI_Colors_ChildColors(final Control child) {

		child.setBackground(_bgColor);
		child.setForeground(_fgColor);

		if (child instanceof Composite) {

			final Control[] children = ((Composite) child).getChildren();

			for (final Control element : children) {

				if (element != null && element.isDisposed() == false) {
					updateUI_Colors_ChildColors(element);
				}
			}
		}
	}
}
