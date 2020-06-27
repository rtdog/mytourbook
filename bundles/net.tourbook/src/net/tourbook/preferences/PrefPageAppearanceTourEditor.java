/*******************************************************************************
 * Copyright (C) 2005, 2013  Wolfgang Schramm and Contributors
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
package net.tourbook.preferences;

import net.tourbook.Messages;
import net.tourbook.application.TourbookPlugin;
import net.tourbook.common.UI;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import net.tourbook.common.time.TimeTools;

public class PrefPageAppearanceTourEditor extends FieldEditorPreferencePage implements IWorkbenchPreferencePage {

	private final IPreferenceStore	_prefStore		= TourbookPlugin.getPrefStore();
	
	private int	fOldDescriptionLines;
	private boolean fOldTimezoneFormatShort;

	private Button _btnTimezoneFormatShort;
	private Button _btnTimezoneFormatLong;
	
	@Override
	protected void createFieldEditors() {

		final Composite parent = getFieldEditorParent();
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createUI(parent);

	}

	private void createUI(final Composite parent) {

		final Group group = new Group(parent, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(group);

		// text: description height
		final IntegerFieldEditor fieldEditor = new IntegerFieldEditor(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT,
				Messages.pref_tour_editor_description_height,
				group);
		fieldEditor.setValidRange(2, 100);
		fieldEditor.getLabelControl(group).setToolTipText(Messages.pref_tour_editor_description_height_tooltip);
		UI.setFieldWidth(group, fieldEditor, UI.DEFAULT_FIELD_WIDTH);
		addField(fieldEditor);

		final Group timezoneFormatGroup = new Group(parent, SWT.NONE);
		timezoneFormatGroup.setText(Messages.pref_tour_editor_group_timezone_format);  
		GridLayoutFactory.swtDefaults().applyTo(timezoneFormatGroup);
		
		_btnTimezoneFormatShort = new Button(timezoneFormatGroup, SWT.RADIO);
		_btnTimezoneFormatLong = new Button(timezoneFormatGroup, SWT.RADIO);		
		
		_btnTimezoneFormatShort.setText("Short Format - e.g. \"" + TimeTools.getTimeZone_ByIndex(TimeTools.getTimeZoneIndex_Default()).zoneId + "\"");
		_btnTimezoneFormatLong.setText("Long Format  - e.g. \"" + TimeTools.getTimeZone_ByIndex(TimeTools.getTimeZoneIndex_Default()).label + "\"");
                
		// set margins after the field editors are added
		final GridLayout groupLayout = (GridLayout) group.getLayout();
		groupLayout.marginWidth = 5;
		groupLayout.marginHeight = 5;
		
		restoreState();
		
		timezoneFormatGroup.layout();
		
	}

	public void init(final IWorkbench workbench) {
		final IPreferenceStore prefStore = TourbookPlugin.getDefault().getPreferenceStore();
		setPreferenceStore(prefStore);

		fOldDescriptionLines = prefStore.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
		fOldTimezoneFormatShort = prefStore.getBoolean(ITourbookPreferences.TOUR_EDITOR_TIMEZONE_SHORT_FORMAT);
	}
	
	
	@Override
	protected void performDefaults() {
		_btnTimezoneFormatShort.setSelection(_prefStore.getDefaultBoolean(ITourbookPreferences.TOUR_EDITOR_TIMEZONE_SHORT_FORMAT));
		_btnTimezoneFormatLong.setSelection(!_prefStore.getDefaultBoolean(ITourbookPreferences.TOUR_EDITOR_TIMEZONE_SHORT_FORMAT));		
		
		super.performDefaults();
	}

	
	@Override
	protected void performApply() {		
		
		super.performApply();
	}

	@Override
	public boolean performOk() {
		final boolean isOK = super.performOk();
		
		final int newDescriptionLines = _prefStore.getInt(ITourbookPreferences.TOUR_EDITOR_DESCRIPTION_HEIGHT);
		
		if (fOldDescriptionLines != newDescriptionLines) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(),
					Messages.pref_tour_editor_dlg_desc_height_title,
					Messages.pref_tour_editor_dlg_desc_height_message);
			fOldDescriptionLines = newDescriptionLines;
		}
		
		if (fOldTimezoneFormatShort != _btnTimezoneFormatShort.getSelection()) {
			MessageDialog.openInformation(Display.getCurrent().getActiveShell(), //
					Messages.pref_tour_editor_dlg_desc_timezone_format_title,
					Messages.pref_tour_editor_dlg_desc_timezone_format_message);
			fOldTimezoneFormatShort = _btnTimezoneFormatShort.getSelection();
		}
		
		saveState();
		
		return isOK;
	}
	
	private void restoreState() {
		final boolean isShortFormat = _prefStore.getBoolean(ITourbookPreferences.TOUR_EDITOR_TIMEZONE_SHORT_FORMAT);
		
		_btnTimezoneFormatShort.setSelection(isShortFormat);
		_btnTimezoneFormatLong.setSelection(!isShortFormat);	
	}

	private void saveState() {

		_prefStore.setValue(ITourbookPreferences.TOUR_EDITOR_TIMEZONE_SHORT_FORMAT, _btnTimezoneFormatShort.getSelection());		
	}
	
}
