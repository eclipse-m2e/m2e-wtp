/*******************************************************************************
 * Copyright (c) 2008 Sonatype, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.eclipse.m2e.wtp.internal.preferences;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jst.j2ee.project.JavaEEProjectUtilities;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.IMavenProjectRegistry;
import org.eclipse.m2e.core.project.IProjectConfigurationManager;
import org.eclipse.m2e.wtp.JEEPackaging;
import org.eclipse.m2e.wtp.MavenWtpPlugin;
import org.eclipse.m2e.wtp.internal.Messages;
import org.eclipse.m2e.wtp.preferences.ConfiguratorEnabler;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferences;
import org.eclipse.m2e.wtp.preferences.IMavenWtpPreferencesManager;
import org.eclipse.m2e.wtp.preferences.MavenWtpPreferencesConstants;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.dialogs.PreferencesUtil;
import org.eclipse.ui.dialogs.PropertyPage;


public class MavenWtpPreferencePage extends PropertyPage implements IWorkbenchPreferencePage {

  //Override elements
  private Composite overrideComp;

  private Button overrideButton;

  private Link fChangeWorkspaceSettings;

  //EAR elements
  private Group earPrefGroup;

  private Button genApplicationXmlButton;

  //WAR elements
  private Group warPrefGroup;

  private Button warMavenArchiverButton;


  private Group configuratorEnablerGroup;

  private List<ConfiguratorEnablerComposite> enablersComposites;
  
  public MavenWtpPreferencePage() {
    setTitle(Messages.MavenWtpPreferencePage_JavaEE_Integration_Settings);
  }

  @Override
protected Control createContents(Composite parent) {
    Composite main = new Composite(parent, SWT.NONE);
    GridLayout gl = new GridLayout(1, false);
    main.setLayout(gl);
    IProject project = getProject();
    createOverridePrefs(main, project);
    if (project == null || JavaEEProjectUtilities.isEARProject(project)) {
      createEarPrefs(main);
    }
    if (project == null || JavaEEProjectUtilities.isDynamicWebProject(project)) {
      createWarPrefs(main);
    }
    IMavenWtpPreferencesManager prefManager = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager();
    fillValues(prefManager.getPreferences(project));
    
    if (project == null) {
      createJavaeeConfiguratorActivation(main, prefManager.getConfiguratorEnablers());
    }
    
    return main;
  }

  private void createJavaeeConfiguratorActivation(Composite main, ConfiguratorEnabler[] configuratorEnablers) {
    if (configuratorEnablers == null || configuratorEnablers.length == 0) {
      return;
    }
    configuratorEnablerGroup = new Group(main, SWT.NONE);
    configuratorEnablerGroup.setText(Messages.MavenWtpPreferencePage_Select_Active_JavaEE_Configurators);
    GridDataFactory.fillDefaults().applyTo(configuratorEnablerGroup);
    GridLayoutFactory.fillDefaults().margins(5, 0).applyTo(configuratorEnablerGroup);
    enablersComposites = new ArrayList<ConfiguratorEnablerComposite>(configuratorEnablers.length);
        
    for (ConfiguratorEnabler configuratorEnabler : configuratorEnablers) {
      ConfiguratorEnablerComposite enablerComposite = new ConfiguratorEnablerComposite(configuratorEnablerGroup, 
          configuratorEnabler, SWT.NONE);
      GridLayoutFactory.fillDefaults().margins(20, 0).applyTo(enablerComposite);
      enablersComposites.add(enablerComposite);
    }
  }

  /**
   * @param main
   */
  private void createEarPrefs(Composite main) {
    earPrefGroup = new Group(main, SWT.NONE);
    earPrefGroup.setText(Messages.MavenWtpPreferencePage_EAR_Project_Preferences);
    earPrefGroup.setLayout(new GridLayout(1, false));
    earPrefGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    genApplicationXmlButton = new Button(earPrefGroup, SWT.CHECK);
    genApplicationXmlButton.setText(Messages.MavenWtpPreferencePage_Generate_ApplicationXml_Under_Build_Dir);
  }

  private void createOverridePrefs(Composite main, IProject project) {
    if(project != null) {
      overrideComp = new Composite(main, SWT.NONE);
      overrideComp.setLayout(new FormLayout());
      overrideComp.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

      overrideButton = new Button(overrideComp, SWT.CHECK);
      overrideButton.setText(Messages.MavenWtpPreferencePage_Enable_Project_Specific_Settings);

      overrideButton.addSelectionListener(new SelectionListener() {
        @Override
		public void widgetDefaultSelected(SelectionEvent e) {
          widgetSelected(e);
        }

        @Override
		public void widgetSelected(SelectionEvent e) {
          setWidgetsEnabled(overrideButton.getSelection());
        }
      });
      FormData fd = new FormData();
      fd.top = new FormAttachment(0, 5);
      fd.left = new FormAttachment(0, 5);
      overrideButton.setLayoutData(fd);

      fd = new FormData();
      fd.top = new FormAttachment(0, 0);
      fd.left = new FormAttachment(overrideButton, 5);
      fd.right = new FormAttachment(100, -5);
      fd.right.alignment = SWT.RIGHT;
      Composite tmp = new Composite(overrideComp, SWT.NONE);
      tmp.setLayoutData(fd);
      tmp.setLayout(new GridLayout(1, true));
      fChangeWorkspaceSettings = createLink(tmp, "Configure Workspace Settings..."); //$NON-NLS-1$
      fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
    }
  }

  private void createWarPrefs(Composite main) {
    warPrefGroup = new Group(main, SWT.NONE);
    warPrefGroup.setText(Messages.MavenWtpPreferencePage_WAR_Project_Preferences);
    warPrefGroup.setLayout(new GridLayout(1, false));
    warPrefGroup.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

    warMavenArchiverButton = new Button(warPrefGroup, SWT.CHECK);
    warMavenArchiverButton.setText(Messages.MavenWtpPreferencePage_Generate_MavenArchiver_Files_Under_Build_Dir);
    warMavenArchiverButton.setToolTipText(Messages.MavenWtpPreferencePage_Using_Build_Directory);
  }

  private Link createLink(Composite composite, String text) {
    Link link = new Link(composite, SWT.BORDER);
    link.setFont(composite.getFont());
    link.setText("<A>" + text + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
    link.addSelectionListener(new SelectionListener() {
      @Override
	public void widgetSelected(SelectionEvent e) {
        openGlobalPrefs();
      }

      @Override
	public void widgetDefaultSelected(SelectionEvent e) {
        openGlobalPrefs();
      }
    });
    return link;
  }

  private void openGlobalPrefs() {
    String id = MavenWtpPreferencesConstants.MAVEN_WTP_PREFERENCE_PAGE;
    PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] {id}, getElement()).open();
  }

  protected void setWidgetsEnabled(boolean isEnabled) {
    if (genApplicationXmlButton != null) {
      genApplicationXmlButton.setEnabled(isEnabled);
    }
    if (warMavenArchiverButton != null) {
      warMavenArchiverButton.setEnabled(isEnabled);
    }
  }

  private void fillValues(IMavenWtpPreferences preferences) {
    IProject project = getProject();

    if(project != null) {
      overrideButton.setSelection(preferences.isEnabledProjectSpecificSettings());
      setWidgetsEnabled(overrideButton.getSelection());
    }
    //read from stored preferences
    if (genApplicationXmlButton != null) {
      genApplicationXmlButton.setSelection(preferences.isApplicationXmGeneratedInBuildDirectory());
    }
    if (warMavenArchiverButton != null) {
      warMavenArchiverButton.setSelection(preferences.isWebMavenArchiverUsesBuildDirectory());
    }
  }

  public IProject getProject() {
    IAdaptable el = getElement();
    IProject p = (el == null) ? null : ((IProject) el.getAdapter(IProject.class));
    return p;
  }

  /**
   * @see org.eclipse.ui.IWorkbenchPreferencePage#init(org.eclipse.ui.IWorkbench)
   */
  @Override
public void init(IWorkbench workbench) {
  }

  /**
   * @see org.eclipse.jface.preference.PreferencePage#performOk()
   */
  @Override
public boolean performOk() {

    IProject project = getProject();
    IMavenWtpPreferencesManager preferencesManager = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager();
    IMavenWtpPreferences preferences = preferencesManager.getPreferences(project);

    IMavenWtpPreferences newPreferences = preferencesManager.createNewPreferences();

    if(project != null) {
      newPreferences.setEnabledProjectSpecificSettings(overrideButton.getSelection());
    }
    if (genApplicationXmlButton != null) {
      newPreferences.setApplicationXmGeneratedInBuildDirectory(genApplicationXmlButton.getSelection());
    }
    if (warMavenArchiverButton != null) {
      newPreferences.setWebMavenArchiverUsesBuildDirectory(warMavenArchiverButton.getSelection());
    }

    if (enablersComposites != null) {
      for (ConfiguratorEnablerComposite enablerComposite : enablersComposites) {
        enablerComposite.savePreferences();
      }
    }
    
    if(!newPreferences.equals(preferences)) {
      preferencesManager.savePreferences(newPreferences, getProject());

      boolean res = MessageDialog.openQuestion(getShell(), Messages.MavenWtpPreferencePage_Maven_JavaEE_Integration_Settings, //
          Messages.MavenWtpPreferencePage_Update_Projects_After_Preference_Changes);
      if(res) {
        updateImpactedProjects();
      }
    }

    return super.performOk();
  }

  /**
   * @see org.eclipse.jface.preference.PreferencePage#performDefaults()
   */
  @Override
protected void performDefaults() {

    IProject project = getProject();
    IMavenWtpPreferencesManager preferencesManager = MavenWtpPlugin.getDefault().getMavenWtpPreferencesManager();
    IMavenWtpPreferences workspacePreferences = preferencesManager.getWorkspacePreferences();

    if(project == null) {
      workspacePreferences.setApplicationXmGeneratedInBuildDirectory(true);
      workspacePreferences.setWebMavenArchiverUsesBuildDirectory(true);
    }

    fillValues(workspacePreferences);

    if (enablersComposites != null) {
      for (ConfiguratorEnablerComposite enablerComposite : enablersComposites) {
        enablerComposite.setDefaultValue();
      }
    }
    
    super.performDefaults();
  }

  /**
   * Update the configuration of maven projects impacted by the configuration change.
   */
  private void updateImpactedProjects() {

    final IMavenProjectRegistry projectManager = MavenPlugin.getMavenProjectRegistry();

    final List<IMavenProjectFacade> facades = getImpactedProjects(projectManager);

    if(facades.isEmpty())
      return;
    
    final IProjectConfigurationManager configurationManager = MavenPlugin.getProjectConfigurationManager();

    WorkspaceJob job = new WorkspaceJob(Messages.MavenWtpPreferencePage_Updating_Maven_Projects_Job) {
  
      @Override
	public IStatus runInWorkspace(IProgressMonitor monitor) {
        try {
          SubMonitor progress = SubMonitor.convert(monitor, Messages.MavenWtpPreferencePage_Updating_Maven_Projects_Monitor, 100);
          SubMonitor subProgress = SubMonitor.convert(progress.newChild(5), facades.size() * 100);
          //projectManager.sortProjects(facades, progress.newChild(5));
          for(IMavenProjectFacade facade : facades) {
            if(progress.isCanceled()) {
              throw new OperationCanceledException();
            }
            IProject project = facade.getProject();
            subProgress.subTask(NLS.bind(Messages.MavenWtpPreferencePage_Updating_Configuration_For_Project, project.getName()));

            configurationManager.updateProjectConfiguration(project, subProgress);
          }

        } catch(CoreException ex) {
          return ex.getStatus();
        }
        return Status.OK_STATUS;
      }
    };
    job.setRule(configurationManager.getRule());
    job.schedule();
  }

  /**
   * Returns the list of Maven projects impacted by the configuration change.
   * 
   * @param projectManager
   * @return
   */
  private List<IMavenProjectFacade> getImpactedProjects(final IMavenProjectRegistry projectManager) {
    final List<IMavenProjectFacade> facades = new ArrayList<IMavenProjectFacade>();
    IProject project = getProject();
    if(project == null) {
      //Get all workspace projects that might be impacted by the configuration change 
      for(IMavenProjectFacade facade : projectManager.getProjects()) {
        if(isImpacted(facade)) {
          facades.add(facade);
        }
      }
    } else {
      facades.add(projectManager.getProject(project));
    }
    return facades;
  }

  /**
   * Checks if the project is impacted by the configuration change.
   * 
   * @param facade
   * @return
   */
  private boolean isImpacted(IMavenProjectFacade facade) {
    //We simply check if the project is an EAR for now
    switch(JEEPackaging.getValue(facade.getPackaging())) {
      case EAR:
      case WAR:
        return true;
      default:
       	return false;
    }
  }
}
