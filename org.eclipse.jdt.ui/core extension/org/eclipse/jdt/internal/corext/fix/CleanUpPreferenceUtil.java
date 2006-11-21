/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.internal.corext.fix;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;

import org.eclipse.core.resources.ProjectScope;

import org.eclipse.jdt.ui.JavaUI;

import org.eclipse.jdt.internal.ui.JavaPlugin;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpMessages;
import org.eclipse.jdt.internal.ui.preferences.cleanup.CleanUpProfileVersioner;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileStore;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.BuiltInProfile;
import org.eclipse.jdt.internal.ui.preferences.formatter.ProfileManager.Profile;

public class CleanUpPreferenceUtil {

	public static Map loadOptions(IScopeContext context) {
    	return loadOptions(context, CleanUpConstants.CLEANUP_PROFILE, CleanUpConstants.DEFAULT_PROFILE);
    }

	public static Map loadSaveParticipantOptions(IScopeContext context) {
    	return loadOptions(context, CleanUpConstants.CLEANUP_ON_SAVE_PROFILE, CleanUpConstants.DEFAULT_SAVE_PARTICIPANT_PROFILE);
    }

	private static Map loadOptions(IScopeContext context, String profileIdKey, String defaultProfileId) {
    	IEclipsePreferences contextNode= context.getNode(JavaUI.ID_PLUGIN);
    	String id= contextNode.get(profileIdKey, null);
    	InstanceScope instanceScope= new InstanceScope();
    	if (id == null) {
    		if (ProjectScope.SCOPE.equals(context.getName())) {
    			id= instanceScope.getNode(JavaUI.ID_PLUGIN).get(profileIdKey, null);
    		}
    		if (id == null) {
    			id= new DefaultScope().getNode(JavaUI.ID_PLUGIN).get(profileIdKey, defaultProfileId);
    		}
    	}
    	
    	List builtInProfiles= getBuiltInProfiles();
    	for (Iterator iterator= builtInProfiles.iterator(); iterator.hasNext();) {
    		Profile profile= (Profile)iterator.next();
            if (id.equals(profile.getID()))
            	return profile.getSettings();
        }
		
    	CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
        ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
        
        List list= null;
        try {
            list= profileStore.readProfiles(instanceScope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null)
        	return null;
        
    	for (Iterator iterator= list.iterator(); iterator.hasNext();) {
            Profile profile= (Profile)iterator.next();
            if (id.equals(profile.getID()))
            	return profile.getSettings();
        }
    	
    	return null;
    }

	/**
	 * Returns a list of {@link ProfileManager.Profile} stored in the <code>scope</code>
	 * including the built in profiles.
	 * @param scope the context from which to retrieve the profiles
	 * @return list of profiles, not null
	 * @since 3.3
	 */
	public static List loadProfiles(IScopeContext scope) {
    	
        CleanUpProfileVersioner versioner= new CleanUpProfileVersioner();
    	ProfileStore profileStore= new ProfileStore(CleanUpConstants.CLEANUP_PROFILES, versioner);
    	
    	List list= null;
        try {
            list= profileStore.readProfiles(scope);
        } catch (CoreException e1) {
            JavaPlugin.log(e1);
        }
        if (list == null) {
        	list= getBuiltInProfiles();
        } else {
        	list.addAll(getBuiltInProfiles());
        }
        
        return list;
    }

	/**
	 * Returns a list of built in clean up profiles
	 * @return the list of built in profiles, not null
	 * @since 3.3
	 */
	public static List getBuiltInProfiles() {
    	ArrayList result= new ArrayList();
    	
    	final Profile eclipseProfile= new BuiltInProfile(CleanUpConstants.ECLIPSE_PROFILE, CleanUpMessages.CleanUpProfileManager_ProfileName_EclipseBuildIn, CleanUpConstants.getEclipseDefaultSettings(), 2, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
    	result.add(eclipseProfile);
    	
    	final Profile saveParticipantProfile= new BuiltInProfile(CleanUpConstants.SAVE_PARTICIPANT_PROFILE, CleanUpMessages.CleanUpProfileManager_save_participant_profileName, CleanUpConstants.getSaveParticipantSettings(), 1, CleanUpProfileVersioner.CURRENT_VERSION, CleanUpProfileVersioner.PROFILE_KIND);
    	result.add(saveParticipantProfile);
    	
    	return result;
    }

}