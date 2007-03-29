package org.eclipse.rse.useractions.ui.compile;

/*******************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.SystemAdapterHelpers;
import org.eclipse.rse.internal.ui.GenericMessages;
import org.eclipse.rse.internal.ui.view.SystemTableViewProvider;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.rse.ui.view.ISystemEditableRemoteObject;
import org.eclipse.rse.ui.view.ISystemRemoteElementAdapter;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.AdaptableList;
import org.eclipse.ui.model.WorkbenchContentProvider;

/**
 * This is the action for an individual compile command, either prompted or not prompted.
 * The label for the action is simply the compile command's label. If promptable, then "..." is appended.
 */
public class SystemCompileAction extends SystemBaseAction {
	private SystemCompileCommand compileCmd;
	private boolean isPrompt;

	/**
	 * Constructor 
	 */
	public SystemCompileAction(Shell shell, SystemCompileCommand compileCommand, boolean isPrompt) {
		super(
				isPrompt ? compileCommand.getLabel() + "..." : compileCommand.getLabel(), compileCommand.getLabel(), RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_COMPILE_ID), shell); // null == image //$NON-NLS-1$
		this.compileCmd = compileCommand;
		this.isPrompt = isPrompt;
		SystemCompileManager mgr = compileCommand.getParentType().getParentProfile().getParentManager();
		allowOnMultipleSelection(mgr.isMultiSelectSupported(compileCommand));
		if (isPrompt)
			setHelp(RSEUIPlugin.HELPPREFIX + "scpa0000"); //$NON-NLS-1$
		else
			setHelp(RSEUIPlugin.HELPPREFIX + "scna0000"); //$NON-NLS-1$
		SystemCompileCommand lucc = compileCmd.getParentType().getLastUsedCompileCommand();
		if ((lucc != null) && lucc.getLabel().equals(compileCmd.getLabel())) {
			setChecked(true);
			// if (!isPrompt)
			//	setAccelerator(SWT.CTRL | SWT.SHIFT | 'c');
		} else
			setChecked(false);
	}

	/**
	 * Intercept of parent method that is our first opportunity to enable/disable this action, typically
	 *  by interrogating the current selection, retrievable via getSelection.
	 * <p>
	 * For this compile action, we disable if we are not currently connected.
	 */
	public boolean updateSelection(IStructuredSelection selection) {
		boolean enable = true;
		Object selected = getFirstSelection();
		if (selected == null) return false;
		ISystemRemoteElementAdapter rmtAdapter = SystemAdapterHelpers.getRemoteAdapter(selected);
		if (rmtAdapter == null) enable = false;
		// yantzi:artemis6.0, we need to allow the menu item to show up even if disconnected in order
		// to allow customers to restore the tree view from cache on startup and still have all actions
		// available.  It is up to the subsystme to make sure to connect if required when the compile
		// command is run
		//else
		//	enable = rmtAdapter.getSubSystem(selected).isConnected();
		if (!enable) return false;
		SystemCompileManager mgr = compileCmd.getParentType().getParentProfile().getParentManager();
		while (enable && (selected != null)) {
			enable = mgr.isCompilable(selected);
			selected = getNextSelection();
		}
		return enable;
	}

	/**
	 * Intercept of parent method that is our opportunity to enable/disable this action, typically
	 *  by interrogating the current selection, retrievable via getSelection.
	 * <p>
	 * For this compile action, we disable if we are not currently connected.
	 */
	public boolean checkObjectType(Object selectedObject) {
		ISystemRemoteElementAdapter rmtAdapter = SystemAdapterHelpers.getRemoteAdapter(selectedObject);
		if (rmtAdapter == null)
			return false;
		else
			return rmtAdapter.getSubSystem(selectedObject).isConnected();
	}

	/**
	 * Called by eclipse when the user selects this action. Does the actual running of the action.
	 */
	public void run() {
		if (checkDirtyEditors()) {
			Object element = getFirstSelection();
			boolean ok = true;
			while (ok && (element != null)) {
				/* FIXME - compile actions not coupled with subsystem API anymore
				 ISystemRemoteElementAdapter rmtAdapter = SystemAdapterHelpers.getRemoteAdapter(element);
				 ISubSystem ss = rmtAdapter.getSubSystem(element);
				 ss.getParentSubSystemFactory().getCompileManager().setSystemConnection(ss.getHost());
				 */
				SystemCompileType compType = compileCmd.getParentType();
				compType.setLastUsedCompileCommand(compileCmd);
				compType.getParentProfile().writeToDisk();
				SystemCompilableSource compilableSrc = compType.getParentProfile().getCompilableSourceObject(getShell(), element, compileCmd, isPrompt, viewer);
				ok = compilableSrc.runCompileCommand();
				if (ok) element = getNextSelection();
			}
		}
	}

	protected List getDirtyEditors() {
		IStructuredSelection sel = getSelection();
		List selection = sel.toList();
		List dirtyEditors = new ArrayList();
		for (int i = 0; i < selection.size(); i++) {
			Object selected = selection.get(i);
			if (selected instanceof IAdaptable) {
				ISystemEditableRemoteObject editable = getEditableFor((IAdaptable) selected);
				if (editable != null) {
					try {
						// is the file being edited?
						if (editable.checkOpenInEditor() == 0) {
							// reference the editing editor
							editable.openEditor();
							// file is open in editor - prompt for save
							if (editable.isDirty()) {
								dirtyEditors.add(editable);
							}
						}
					} catch (Exception e) {
					}
				}
			}
		}
		return dirtyEditors;
	}

	protected ISystemEditableRemoteObject getEditableFor(IAdaptable selected) {
		ISystemRemoteElementAdapter adapter = (ISystemRemoteElementAdapter) selected.getAdapter(ISystemRemoteElementAdapter.class);
		if (adapter.canEdit(selected)) {
			ISystemEditableRemoteObject editable = adapter.getEditableRemoteObject(selected);
			try {
				editable.setLocalResourceProperties();
			} catch (Exception e) {
			}
			return editable;
		}
		return null;
	}

	protected boolean checkDirtyEditors() {
		List dirtyEditors = getDirtyEditors();
		if (dirtyEditors.size() > 0) {
			AdaptableList input = new AdaptableList();
			for (int i = 0; i < dirtyEditors.size(); i++) {
				ISystemEditableRemoteObject rmtObj = (ISystemEditableRemoteObject) dirtyEditors.get(i);
				input.add(rmtObj.getRemoteObject());
			}
			WorkbenchContentProvider cprovider = new WorkbenchContentProvider();
			SystemTableViewProvider lprovider = new SystemTableViewProvider(null);
			// TODO: Cannot use WorkbenchMessages -- it's internal
			ListSelectionDialog dlg = new ListSelectionDialog(getShell(), input, cprovider, lprovider, GenericMessages.EditorManager_saveResourcesMessage);
			dlg.setInitialSelections(input.getChildren());
			// TODO: Cannot use WorkbenchMessages -- it's internal
			dlg.setTitle(GenericMessages.EditorManager_saveResourcesTitle);
			int result = dlg.open();
			//Just return false to prevent the operation continuing
			if (result == IDialogConstants.CANCEL_ID) return false;
			Object[] filesToSave = dlg.getResult();
			for (int s = 0; s < filesToSave.length; s++) {
				IAdaptable rmtObj = (IAdaptable) filesToSave[s];
				ISystemEditableRemoteObject editable = getEditableFor(rmtObj);
				editable.doImmediateSaveAndUpload();
			}
		}
		return true;
	}
}
