/********************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view.team;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.model.ISystemProfile;
import org.eclipse.rse.core.model.ISystemProfileManager;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.internal.model.SystemProfileManager;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemIconConstants;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.actions.SystemBaseAction;
import org.eclipse.swt.widgets.Shell;


/**
 * The action allows users to de-activate all selected profiles
 */
public class SystemTeamViewMakeInActiveProfileAction extends SystemBaseAction 
                                 
{
	
	/**
	 * Constructor 
	 */
	public SystemTeamViewMakeInActiveProfileAction(Shell parent) 
	{
		super(SystemResources.ACTION_PROFILE_MAKEINACTIVE_LABEL,SystemResources.ACTION_PROFILE_MAKEINACTIVE_TOOLTIP,
		      RSEUIPlugin.getDefault().getImageDescriptor(ISystemIconConstants.ICON_SYSTEM_MAKEPROFILEINACTIVE_ID),
		      parent);
        allowOnMultipleSelection(true);
		setContextMenuGroup(ISystemContextMenuConstants.GROUP_CHANGE);
		setHelp(RSEUIPlugin.HELPPREFIX+"ActionMakeInactive"); //$NON-NLS-1$
	}

	/**
	 * Here we decide whether to enable ths action or not. We enable it
	 * if every selected object is a profile, and if its not the case
	 * that every selected profile is already inactive.
	 * @see SystemBaseAction#updateSelection(IStructuredSelection)
	 */
	public boolean updateSelection(IStructuredSelection selection)
	{
		Object currsel = getFirstSelection();
		if (!(currsel instanceof ISystemProfile))
			return false;
		ISystemProfile profile = (ISystemProfile)currsel;
		ISystemProfileManager mgr = SystemProfileManager.getSystemProfileManager();
		boolean allInActive = true;
		while (profile != null)
		{
			if (mgr.isSystemProfileActive(profile.getName()))
				allInActive = false;
			currsel = getNextSelection();
			if ((currsel!=null) && !(currsel instanceof ISystemProfile))
				return false;
			profile = (ISystemProfile)currsel;
		}			
		return !allInActive;
	}

	/**
	 * This is the method called when the user selects this action.
	 * It walks through all the selected profiles and make them all inactive
	 */
	public void run() 
	{
		// TODO: test if attempting to disable all profiles, and issue an error
		//  that at least one needs to be active. Or, at least a warning.
		ISystemRegistry sr = RSEUIPlugin.getTheSystemRegistry();
		ISystemProfile profile = (ISystemProfile)getFirstSelection();
		while (profile != null)
		{
			sr.setSystemProfileActive(profile, false);
			profile = (ISystemProfile)getNextSelection();
		}		
	}		
}