/*
 * (c) Copyright IBM Corp. 2000, 2002.
 * All Rights Reserved.
 */
package org.eclipse.jdt.internal.ui.jarpackager;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.jar.Attributes;
import java.util.jar.Manifest;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import org.eclipse.jface.dialogs.DialogPage;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;

import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.dialogs.SaveAsDialog;
import org.eclipse.ui.dialogs.SelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;

import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.IType;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.search.IJavaSearchScope;

import org.eclipse.jdt.ui.JavaElementLabelProvider;
import org.eclipse.jdt.ui.JavaUI;
import org.eclipse.jdt.ui.StandardJavaElementContentProvider;

import org.eclipse.jdt.ui.jarpackager.JarPackageData;

import org.eclipse.jdt.internal.ui.IJavaHelpContextIds;
import org.eclipse.jdt.internal.ui.JavaPlugin;

import org.eclipse.jdt.internal.ui.dialogs.StatusInfo;
import org.eclipse.jdt.internal.ui.filters.EmptyInnerPackageFilter;

import org.eclipse.jdt.internal.ui.search.JavaSearchScopeFactory;
import org.eclipse.jdt.internal.ui.util.ExceptionHandler;
import org.eclipse.jdt.internal.ui.util.MainMethodSearchEngine;
import org.eclipse.jdt.internal.ui.util.SWTUtil;
import org.eclipse.jdt.internal.ui.viewsupport.LibraryFilter;

/**
 *	Page 3 of the JAR Package wizard
 */
class JarManifestWizardPage extends WizardPage implements IJarPackageWizardPage {

	// Untyped listener
	private class UntypedListener implements Listener {
		/*
		 * Implements method from Listener
		 */	
		public void handleEvent(Event e) {
			if (getControl() == null)
				return;
			update();
		}
	}
	private UntypedListener fUntypedListener= new UntypedListener();

	// Model
	private JarPackageData fJarPackage;
	
	// Cache for main types
	private IType[] fMainTypes;
	
	// Widgets
	private Composite	fManifestGroup;
	private Button		fGenerateManifestRadioButton;
	private Button		fSaveManifestCheckbox;
	private Button		fReuseManifestCheckbox;
	private Text		fNewManifestFileText;
	private Label		fNewManifestFileLabel;
	private Button		fNewManifestFileBrowseButton;
	private Button		fUseManifestRadioButton;
	private Text		fManifestFileText;
	private Label		fManifestFileLabel;
	private Button		fManifestFileBrowseButton;
	
	private Label		fSealingHeaderLabel;
	private Button		fSealJarRadioButton;
	private Label		fSealJarLabel;
	private Button		fSealedPackagesDetailsButton;
	private Button		fSealPackagesRadioButton;	
	private Label		fSealPackagesLabel;
	private Button		fUnSealedPackagesDetailsButton;
	
	private Label		fMainClassHeaderLabel;
	private Label		fMainClassLabel;
	private Text		fMainClassText;
	private Button		fMainClassBrowseButton;
	
	// Dialog store id constants
	private final static String PAGE_NAME= "JarManifestWizardPage"; //$NON-NLS-1$
	
	// Manifest creation
	private final static String STORE_GENERATE_MANIFEST= PAGE_NAME + ".GENERATE_MANIFEST"; //$NON-NLS-1$
	private final static String STORE_SAVE_MANIFEST= PAGE_NAME + ".SAVE_MANIFEST"; //$NON-NLS-1$
	private final static String STORE_REUSE_MANIFEST= PAGE_NAME + ".REUSE_MANIFEST"; //$NON-NLS-1$
	private final static String STORE_MANIFEST_LOCATION= PAGE_NAME + ".MANIFEST_LOCATION"; //$NON-NLS-1$
	
	// Sealing
	private final static String STORE_SEAL_JAR= PAGE_NAME + ".SEAL_JAR"; //$NON-NLS-1$
	
	/**
	 *	Create an instance of this class
	 */
	public JarManifestWizardPage(JarPackageData jarPackage) {
		super(PAGE_NAME);
		setTitle(JarPackagerMessages.getString("JarManifestWizardPage.title")); //$NON-NLS-1$
		setDescription(JarPackagerMessages.getString("JarManifestWizardPage.description")); //$NON-NLS-1$
		fJarPackage= jarPackage;
	}

	// ----------- Widget creation  -----------

	/*
	 * Method declared on IDialogPage.
	 */
	public void createControl(Composite parent) {
		
		initializeDialogUnits(parent);
		
		Composite composite= new Composite(parent, SWT.NULL);
		composite.setLayout(new GridLayout());
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		createLabel(composite, JarPackagerMessages.getString("JarManifestWizardPage.manifestSource.label"), false); //$NON-NLS-1$
		createManifestGroup(composite);

		createSpacer(composite);

		fSealingHeaderLabel= createLabel(composite, JarPackagerMessages.getString("JarManifestWizardPage.sealingHeader.label"), false); //$NON-NLS-1$
		createSealingGroup(composite);

		createSpacer(composite);

		fMainClassHeaderLabel= createLabel(composite, JarPackagerMessages.getString("JarManifestWizardPage.mainClassHeader.label"), false); //$NON-NLS-1$
		createMainClassGroup(composite);

		setEqualButtonSizes();
		
		restoreWidgetValues();

		setControl(composite);
		update();

		WorkbenchHelp.setHelp(composite, IJavaHelpContextIds.JARMANIFEST_WIZARD_PAGE);								
			
	}
	/**
	 *	Create the export options specification widgets.
	 *
	 *	@param parent org.eclipse.swt.widgets.Composite
	 */
	protected void createManifestGroup(Composite parent) {
		fManifestGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		fManifestGroup.setLayout(layout);		
		fManifestGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		fGenerateManifestRadioButton= new Button(fManifestGroup, SWT.RADIO | SWT.LEFT);
		fGenerateManifestRadioButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.genetateManifest.text")); //$NON-NLS-1$
		fGenerateManifestRadioButton.addListener(SWT.Selection, fUntypedListener);

			Composite saveOptions= new Composite(fManifestGroup, SWT.NONE);
			GridLayout saveOptionsLayout= new GridLayout();
			saveOptionsLayout.marginWidth= 0;
			saveOptions.setLayout(saveOptionsLayout);

			GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			data.horizontalIndent=20;
			saveOptions.setLayoutData(data);
	
			fSaveManifestCheckbox= new Button(saveOptions, SWT.CHECK | SWT.LEFT);
			fSaveManifestCheckbox.setText(JarPackagerMessages.getString("JarManifestWizardPage.saveManifest.text")); //$NON-NLS-1$
			fSaveManifestCheckbox.addListener(SWT.MouseUp, fUntypedListener);
	
			fReuseManifestCheckbox= new Button(saveOptions, SWT.CHECK | SWT.LEFT);
			fReuseManifestCheckbox.setText(JarPackagerMessages.getString("JarManifestWizardPage.reuseManifest.text")); //$NON-NLS-1$
			fReuseManifestCheckbox.addListener(SWT.MouseUp, fUntypedListener);
			
			createNewManifestFileGroup(saveOptions);

		fUseManifestRadioButton= new Button(fManifestGroup, SWT.RADIO | SWT.LEFT);
		fUseManifestRadioButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.useManifest.text")); //$NON-NLS-1$
		
		fUseManifestRadioButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
			Composite existingManifestGroup= new Composite(fManifestGroup, SWT.NONE);
			GridLayout existingManifestLayout= new GridLayout();
			existingManifestLayout.marginWidth= 0;
			existingManifestGroup.setLayout(existingManifestLayout);
			data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
			data.horizontalIndent=20;
			existingManifestGroup.setLayoutData(data);
			createManifestFileGroup(existingManifestGroup);
	}

	protected void createNewManifestFileGroup(Composite parent) {
		// destination specification group
		Composite manifestFileGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.marginWidth= 0;
		layout.numColumns= 3;
		manifestFileGroup.setLayout(layout);
		manifestFileGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		fNewManifestFileLabel= new Label(manifestFileGroup, SWT.NONE);
		fNewManifestFileLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.newManifestFile.text")); //$NON-NLS-1$

		// entry field
		fNewManifestFileText= new Text(manifestFileGroup, SWT.SINGLE | SWT.BORDER);
		fNewManifestFileText.addListener(SWT.Modify, fUntypedListener);
		
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(40);
		fNewManifestFileText.setLayoutData(data);

		// browse button
		fNewManifestFileBrowseButton= new Button(manifestFileGroup, SWT.PUSH);
		fNewManifestFileBrowseButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.newManifestFileBrowseButton.text")); //$NON-NLS-1$
		fNewManifestFileBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fNewManifestFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleNewManifestFileBrowseButtonPressed();
			}
		});
	}

	protected void createManifestFileGroup(Composite parent) {
		// destination specification group
		Composite manifestFileGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		layout.marginWidth= 0;
		
		manifestFileGroup.setLayout(layout);
		manifestFileGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));

		fManifestFileLabel= new Label(manifestFileGroup, SWT.NONE);
		fManifestFileLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.manifestFile.text")); //$NON-NLS-1$

		// entry field
		fManifestFileText= new Text(manifestFileGroup, SWT.SINGLE | SWT.BORDER);
		fManifestFileText.addListener(SWT.Modify, fUntypedListener);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(40);
		fManifestFileText.setLayoutData(data);

		// browse button
		fManifestFileBrowseButton= new Button(manifestFileGroup, SWT.PUSH);
		fManifestFileBrowseButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.manifestFileBrowse.text")); //$NON-NLS-1$
		fManifestFileBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fManifestFileBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleManifestFileBrowseButtonPressed();
			}
		});
	}
	/**
	 * Creates the JAR sealing specification controls.
	 *
	 * @param parent the parent control
	 */
	protected void createSealingGroup(Composite parent) {
		// destination specification group
		Composite sealingGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		layout.horizontalSpacing += 3;
		sealingGroup.setLayout(layout);
		sealingGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));
		createSealJarGroup(sealingGroup);
		createSealPackagesGroup(sealingGroup);
		
	}
	/**
	 * Creates the JAR sealing specification controls to seal the whole JAR.
	 *
	 * @param parent the parent control
	 */
	protected void createSealJarGroup(Composite sealGroup) {
		fSealJarRadioButton= new Button(sealGroup, SWT.RADIO);
		fSealJarRadioButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.sealJar.text")); //$NON-NLS-1$
		fSealJarRadioButton.addListener(SWT.Selection, fUntypedListener);

		fSealJarLabel= new Label(sealGroup, SWT.RIGHT);
		fSealJarLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		fSealJarLabel.setText(""); //$NON-NLS-1$
		
		fUnSealedPackagesDetailsButton= new Button(sealGroup, SWT.PUSH);
		fUnSealedPackagesDetailsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fUnSealedPackagesDetailsButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.unsealPackagesButton.text")); //$NON-NLS-1$
		fUnSealedPackagesDetailsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleUnSealPackagesDetailsButtonPressed();
			}
		});
		
	}
	/**
	 * Creates the JAR sealing specification controls to seal packages.
	 *
	 * @param parent the parent control
	 */
	protected void createSealPackagesGroup(Composite sealGroup) {
		fSealPackagesRadioButton= new Button(sealGroup, SWT.RADIO);
		fSealPackagesRadioButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.sealPackagesButton.text")); //$NON-NLS-1$

		fSealPackagesLabel= new Label(sealGroup, SWT.RIGHT);
		fSealPackagesLabel.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL));
		fSealPackagesLabel.setText(""); //$NON-NLS-1$

		fSealedPackagesDetailsButton= new Button(sealGroup, SWT.PUSH);
		fSealedPackagesDetailsButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fSealedPackagesDetailsButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.sealedPackagesDetailsButton.text")); //$NON-NLS-1$
		fSealedPackagesDetailsButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleSealPackagesDetailsButtonPressed();
			}
		});
	}

	protected void createMainClassGroup(Composite parent) {
		// main type group
		Composite mainClassGroup= new Composite(parent, SWT.NONE);
		GridLayout layout= new GridLayout();
		layout.numColumns= 3;
		mainClassGroup.setLayout(layout);
		mainClassGroup.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.VERTICAL_ALIGN_FILL));

		fMainClassLabel= new Label(mainClassGroup, SWT.NONE);
		fMainClassLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.mainClass.label")); //$NON-NLS-1$

		// entry field
		fMainClassText= new Text(mainClassGroup, SWT.SINGLE | SWT.BORDER);
		fMainClassText.addListener(SWT.Modify, fUntypedListener);
		GridData data= new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL);
		data.widthHint= convertWidthInCharsToPixels(40);
		fMainClassText.setLayoutData(data);
		fMainClassText.addKeyListener(new KeyAdapter() {
			/*
			 * @see KeyListener#keyReleased(KeyEvent)
			 */
			public void keyReleased(KeyEvent e) {
				fJarPackage.setManifestMainClass(findMainMethodByName(fMainClassText.getText()));
				update();
			}
		});

		// browse button
		fMainClassBrowseButton= new Button(mainClassGroup, SWT.PUSH);
		fMainClassBrowseButton.setText(JarPackagerMessages.getString("JarManifestWizardPage.mainClassBrowseButton.text")); //$NON-NLS-1$
		fMainClassBrowseButton.setLayoutData(new GridData(GridData.HORIZONTAL_ALIGN_FILL));
		fMainClassBrowseButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				handleMainClassBrowseButtonPressed();
			}
		});
	}
	
	// ----------- Event handlers  -----------
		
	private void update() {
		updateModel();
		updateEnableState();
		updatePageCompletion();
	}
	
	/**
	 *	Open an appropriate dialog so that the user can specify a manifest
	 *	to save
	 */
	protected void handleNewManifestFileBrowseButtonPressed() {
		// Use Save As dialog to select a new file inside the workspace
		SaveAsDialog dialog= new SaveAsDialog(getContainer().getShell());
		dialog.create();
		dialog.getShell().setText(JarPackagerMessages.getString("JarManifestWizardPage.saveAsDialog.title")); //$NON-NLS-1$
		dialog.setMessage(JarPackagerMessages.getString("JarManifestWizardPage.saveAsDialog.message")); //$NON-NLS-1$
		dialog.setOriginalFile(createFileHandle(fJarPackage.getManifestLocation()));
		if (dialog.open() == SaveAsDialog.OK) {
			fJarPackage.setManifestLocation(dialog.getResult());
			fNewManifestFileText.setText(dialog.getResult().toString());
		}
	}

	protected void handleManifestFileBrowseButtonPressed() {
		ElementTreeSelectionDialog dialog= createWorkspaceFileSelectionDialog(JarPackagerMessages.getString("JarManifestWizardPage.manifestSelectionDialog.title"), JarPackagerMessages.getString("JarManifestWizardPage.manifestSelectionDialog.message")); //$NON-NLS-2$ //$NON-NLS-1$
		if (fJarPackage.isManifestAccessible())
			dialog.setInitialSelections(new IResource[] {fJarPackage.getManifestFile()});
		if (dialog.open() ==  ElementTreeSelectionDialog.OK) {
			Object[] resources= dialog.getResult();
			if (resources.length != 1)
				setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.onlyOneManifestMustBeSelected")); //$NON-NLS-1$
			else {
				setErrorMessage(""); //$NON-NLS-1$
				fJarPackage.setManifestLocation(((IResource)resources[0]).getFullPath());
				fManifestFileText.setText(fJarPackage.getManifestLocation().toString());
			}
		}
	}

	private IType findMainMethodByName(String name) {
		if (fMainTypes == null) {
			List resources= JarPackagerUtil.asResources(fJarPackage.getElements());
			if (resources == null)
				setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.noResourceSelected")); //$NON-NLS-1$
			IJavaSearchScope searchScope= JavaSearchScopeFactory.getInstance().createJavaSearchScope((IResource[])resources.toArray(new IResource[resources.size()]));
			MainMethodSearchEngine engine= new MainMethodSearchEngine();
			try {
				fMainTypes= engine.searchMainMethods(getContainer(), searchScope, 0);
			} catch (InvocationTargetException ex) {
				// null
			} catch (InterruptedException e) {
				// null
			}
		}
		for (int i= 0; i < fMainTypes.length; i++) {
			if (fMainTypes[i].getFullyQualifiedName().equals(name))
			 return fMainTypes[i];
		}
		return null;
	}

	protected void handleMainClassBrowseButtonPressed() {
		List resources= JarPackagerUtil.asResources(fJarPackage.getElements());
		if (resources == null) {
			setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.noResourceSelected")); //$NON-NLS-1$
			return;
		}
		IJavaSearchScope searchScope= JavaSearchScopeFactory.getInstance().createJavaSearchScope((IResource[])resources.toArray(new IResource[resources.size()]));
		SelectionDialog dialog= JavaUI.createMainTypeDialog(getContainer().getShell(), getContainer(), searchScope, 0, false, ""); //$NON-NLS-1$
		dialog.setTitle(JarPackagerMessages.getString("JarManifestWizardPage.mainTypeSelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(JarPackagerMessages.getString("JarManifestWizardPage.mainTypeSelectionDialog.message")); //$NON-NLS-1$
		if (fJarPackage.getManifestMainClass() != null)
			dialog.setInitialSelections(new Object[] {fJarPackage.getManifestMainClass()});

		if (dialog.open() == SelectionDialog.OK) {
			fJarPackage.setManifestMainClass((IType)dialog.getResult()[0]);
			fMainClassText.setText(JarPackagerUtil.getMainClassName(fJarPackage));
		} else if (!fJarPackage.isMainClassValid(getContainer())) {
			// user did not cancel: no types were found
			fJarPackage.setManifestMainClass(null);
			fMainClassText.setText(JarPackagerUtil.getMainClassName(fJarPackage));
		}
	}

	protected void handleSealPackagesDetailsButtonPressed() {
		SelectionDialog dialog= createPackageDialog(getPackagesForSelectedResources(fJarPackage));
		dialog.setTitle(JarPackagerMessages.getString("JarManifestWizardPage.sealedPackagesSelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(JarPackagerMessages.getString("JarManifestWizardPage.sealedPackagesSelectionDialog.message")); //$NON-NLS-1$
		dialog.setInitialSelections(fJarPackage.getPackagesToSeal());
		if (dialog.open() == SelectionDialog.OK)
			fJarPackage.setPackagesToSeal(getPackagesFromDialog(dialog));
		updateSealingInfo();
	}

	protected void handleUnSealPackagesDetailsButtonPressed() {
		SelectionDialog dialog= createPackageDialog(getPackagesForSelectedResources(fJarPackage));
		dialog.setTitle(JarPackagerMessages.getString("JarManifestWizardPage.unsealedPackagesSelectionDialog.title")); //$NON-NLS-1$
		dialog.setMessage(JarPackagerMessages.getString("JarManifestWizardPage.unsealedPackagesSelectionDialog.message")); //$NON-NLS-1$
		dialog.setInitialSelections(fJarPackage.getPackagesToUnseal());
		if (dialog.open() == SelectionDialog.OK)
			fJarPackage.setPackagesToUnseal(getPackagesFromDialog(dialog));
		updateSealingInfo();
	}
	/**
	 * Updates the enable state of this page's controls. Subclasses may extend.
	 */
	protected void updateEnableState() {
		boolean generate= fGenerateManifestRadioButton.getSelection();

		boolean save= generate && fSaveManifestCheckbox.getSelection();
		fSaveManifestCheckbox.setEnabled(generate);
		fReuseManifestCheckbox.setEnabled(fJarPackage.isDescriptionSaved() && save);
		fNewManifestFileText.setEnabled(save);
		fNewManifestFileLabel.setEnabled(save);
		fNewManifestFileBrowseButton.setEnabled(save);

		fManifestFileText.setEnabled(!generate);
		fManifestFileLabel.setEnabled(!generate);
		fManifestFileBrowseButton.setEnabled(!generate);

		fSealingHeaderLabel.setEnabled(generate);
		boolean sealState= fSealJarRadioButton.getSelection();
		fSealJarRadioButton.setEnabled(generate);
		fSealJarLabel.setEnabled(generate);
		fUnSealedPackagesDetailsButton.setEnabled(sealState && generate);
		fSealPackagesRadioButton.setEnabled(generate);
		fSealPackagesLabel.setEnabled(generate);
		fSealedPackagesDetailsButton.setEnabled(!sealState && generate);

		fMainClassHeaderLabel.setEnabled(generate);
		fMainClassLabel.setEnabled(generate);
		fMainClassText.setEnabled(generate);
		fMainClassBrowseButton.setEnabled(generate);

		updateSealingInfo();
	}
		
	protected void updateSealingInfo() {
		if (fJarPackage.isJarSealed()) {
			fSealPackagesLabel.setText(""); //$NON-NLS-1$
			int i= fJarPackage.getPackagesToUnseal().length;
			if (i == 0)
				fSealJarLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.jarSealed")); //$NON-NLS-1$
			else if (i == 1)
				fSealJarLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.jarSealedExceptOne")); //$NON-NLS-1$
			else
				fSealJarLabel.setText(JarPackagerMessages.getFormattedString("JarManifestWizardPage.jarSealedExceptSome", new Integer(i))); //$NON-NLS-1$
				
		}
		else {
			fSealJarLabel.setText(""); //$NON-NLS-1$
			int i= fJarPackage.getPackagesToSeal().length;
			if (i == 0)
				fSealPackagesLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.nothingSealed")); //$NON-NLS-1$
			else if (i == 1)
				fSealPackagesLabel.setText(JarPackagerMessages.getString("JarManifestWizardPage.onePackageSealed")); //$NON-NLS-1$
			else
				fSealPackagesLabel.setText(JarPackagerMessages.getFormattedString("JarManifestWizardPage.somePackagesSealed", new Integer(i))); //$NON-NLS-1$
		}
	}
	/*
	 * Implements method from IJarPackageWizardPage
	 */
	public boolean isPageComplete() {
		boolean isPageComplete= true;
		setMessage(null);
		
		if (!fJarPackage.areClassFilesExported())
			return true;
		
		if (fJarPackage.isManifestGenerated() && fJarPackage.isManifestSaved()) {
			if (fJarPackage.getManifestLocation().toString().length() == 0)
					isPageComplete= false;
			else {
				IPath location= fJarPackage.getManifestLocation();
				if (!location.toString().startsWith("/")) { //$NON-NLS-1$
					setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.manifestPathMustBeAbsolute")); //$NON-NLS-1$
					return false;
				}			
				IResource resource= findResource(location);
				if (resource != null && resource.getType() != IResource.FILE) {
					setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.manifestMustNotBeExistingContainer")); //$NON-NLS-1$
					return false;
				}
				resource= findResource(location.removeLastSegments(1));
				if (resource == null || resource.getType() == IResource.FILE) {
					setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.manifestContainerDoesNotExist")); //$NON-NLS-1$
					return false;
				}
			}
		}
		if (!fJarPackage.isManifestGenerated()) {
			if (fJarPackage.isManifestAccessible()) {
				Manifest manifest= null;
				try {
					manifest= fJarPackage.getManifestProvider().create(fJarPackage);
				} catch (CoreException ex) {
					// nothing reported in the wizard
				}
				if (manifest != null && manifest.getMainAttributes().getValue(Attributes.Name.MANIFEST_VERSION) == null)
					setMessage(JarPackagerMessages.getString("JarManifestWizardPage.warning.noManifestVersion"), DialogPage.WARNING); //$NON-NLS-1$
			} else {
				if (fJarPackage.getManifestLocation().toString().length() == 0)
					setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.noManifestFile")); //$NON-NLS-1$
				else
					setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.invalidManifestFile")); //$NON-NLS-1$
				return false;
			}
		}
		Set selectedPackages= getPackagesForSelectedResources(fJarPackage);
		if (fJarPackage.isJarSealed()
				&& !selectedPackages.containsAll(Arrays.asList(fJarPackage.getPackagesToUnseal()))) {
			setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.unsealedPackagesNotInSelection")); //$NON-NLS-1$
			return false;
		}
		if (!fJarPackage.isJarSealed()
				&& !selectedPackages.containsAll(Arrays.asList(fJarPackage.getPackagesToSeal()))) {
			setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.sealedPackagesNotInSelection")); //$NON-NLS-1$
			return false;
		}
		if (!fJarPackage.isMainClassValid(getContainer()) || (fJarPackage.getManifestMainClass() == null && fMainClassText.getText().length() > 0)) {
			setErrorMessage(JarPackagerMessages.getString("JarManifestWizardPage.error.invalidMainClass")); //$NON-NLS-1$
			return false;
		}

		setErrorMessage(null);
		return isPageComplete;
	}

	/* 
	 * Implements method from IWizardPage.
	 */
	public void setPreviousPage(IWizardPage page) {
		super.setPreviousPage(page);
		fMainTypes= null;
		updateEnableState();
		if (getContainer() != null)
			updatePageCompletion();
	}

	/* 
	 * Implements method from IJarPackageWizardPage.
	 */
	public void finish() {
		saveWidgetValues();
	}

	// ----------- Model handling -----------

	/**
	 * Persists resource specification control setting that are to be restored
	 * in the next instance of this page. Subclasses wishing to persist
	 * settings for their controls should extend the hook method 
	 * <code>internalSaveWidgetValues</code>.
	 */
	public final void saveWidgetValues() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			// Manifest creation
			settings.put(STORE_GENERATE_MANIFEST, fJarPackage.isManifestGenerated());
			settings.put(STORE_SAVE_MANIFEST, fJarPackage.isManifestSaved());
			settings.put(STORE_REUSE_MANIFEST, fJarPackage.isManifestReused());
			settings.put(STORE_MANIFEST_LOCATION, fJarPackage.getManifestLocation().toString());

			// Sealing
			settings.put(STORE_SEAL_JAR, fJarPackage.isJarSealed());
			}

		// Allow subclasses to save values
		internalSaveWidgetValues();
	}
	/**
	 * Hook method for subclasses to persist their settings.
	 */
	protected void internalSaveWidgetValues() {
	}
	/**
	 *	Hook method for restoring widget values to the values that they held
	 *	last time this wizard was used to completion.
	 */
	protected void restoreWidgetValues() {
		if (!((JarPackageWizard)getWizard()).isInitializingFromJarPackage())
			initializeJarPackage();

		// Manifest creation
		if (fJarPackage.isManifestGenerated())
			fGenerateManifestRadioButton.setSelection(true);
		else
			fUseManifestRadioButton.setSelection(true);
		fSaveManifestCheckbox.setSelection(fJarPackage.isManifestSaved());
		fReuseManifestCheckbox.setSelection(fJarPackage.isManifestReused());
		fManifestFileText.setText(fJarPackage.getManifestLocation().toString());
		fNewManifestFileText.setText(fJarPackage.getManifestLocation().toString());	

		// Sealing
		if (fJarPackage.isJarSealed())
			fSealJarRadioButton.setSelection(true);
		else
			fSealPackagesRadioButton.setSelection(true);
		
		// Main-Class
		fMainClassText.setText(JarPackagerUtil.getMainClassName(fJarPackage));
	}
	/**
	 *	Initializes the JAR package from last used wizard page values.
	 */
	protected void initializeJarPackage() {
		IDialogSettings settings= getDialogSettings();
		if (settings != null) {
			// Manifest creation
			fJarPackage.setGenerateManifest(settings.getBoolean(STORE_GENERATE_MANIFEST));
			fJarPackage.setSaveManifest(settings.getBoolean(STORE_SAVE_MANIFEST));
			fJarPackage.setReuseManifest(settings.getBoolean(STORE_REUSE_MANIFEST));
			String pathStr= settings.get(STORE_MANIFEST_LOCATION);
			if (pathStr == null)
				pathStr= ""; //$NON-NLS-1$
			fJarPackage.setManifestLocation(new Path(pathStr));

			// Sealing
			fJarPackage.setSealJar(settings.getBoolean(STORE_SEAL_JAR));
		}
	}
	/**
	 *	Stores the widget values in the JAR package.
	 */
	protected void updateModel() {
		if (getControl() == null)
			return;
		
		// Manifest creation
		fJarPackage.setGenerateManifest(fGenerateManifestRadioButton.getSelection());
		fJarPackage.setSaveManifest(fSaveManifestCheckbox.getSelection());
		fJarPackage.setReuseManifest(fReuseManifestCheckbox.getSelection());
		String path;
		if (fJarPackage.isManifestGenerated())
			path= fNewManifestFileText.getText();
		else
			path= fManifestFileText.getText();
		if (path == null)
			path= ""; //$NON-NLS-1$
		fJarPackage.setManifestLocation(new Path(path));

		// Sealing
		fJarPackage.setSealJar(fSealJarRadioButton.getSelection());
	}
	/**
	 * Determine if the page is complete and update the page appropriately. 
	 */
	protected void updatePageCompletion() {
		boolean pageComplete= isPageComplete();
		setPageComplete(pageComplete);
		if (pageComplete) {
			setErrorMessage(null);
		}
	}

	// ----------- Utility methods -----------

	/**
	 * Creates a file resource handle for the file with the given workspace path.
	 * This method does not create the file resource; this is the responsibility
	 * of <code>createFile</code>.
	 *
	 * @param filePath the path of the file resource to create a handle for
	 * @return the new file resource handle
	 * @see #createFile
	 */
	protected IFile createFileHandle(IPath filePath) {
		if (filePath.isValidPath(filePath.toString()) && filePath.segmentCount() >= 2)
			return JavaPlugin.getWorkspace().getRoot().getFile(filePath);
		else
			return null;
	}
	/**
	 * Creates a new label with a bold font.
	 *
	 * @param parent the parent control
	 * @param text the label text
	 * @return the new label control
	 */
	protected Label createLabel(Composite parent, String text, boolean bold) {
		Label label= new Label(parent, SWT.NONE);
		if (bold)
			label.setFont(JFaceResources.getBannerFont());
		label.setText(text);
		GridData data= new GridData();
		data.verticalAlignment= GridData.FILL;
		data.horizontalAlignment= GridData.FILL;
		label.setLayoutData(data);
		return label;
	}
	/**
	 * Sets the size of a control.
	 *
	 * @param control the control for which to set the size
	 * @param width the new  width of the control
	 * @param height the new height of the control
	 */
	protected void setSize(Control control, int width, int height) {
		GridData gd= new GridData(GridData.END);
		gd.widthHint= width ;
		gd.heightHint= height;
		control.setLayoutData(gd);
	}
	/**
	 * Makes the size of all buttons equal.
	 */
	protected void setEqualButtonSizes() {
		int width= SWTUtil.getButtonWidthHint(fManifestFileBrowseButton);
		int height= SWTUtil.getButtonHeigthHint(fManifestFileBrowseButton);
		int width2= SWTUtil.getButtonWidthHint(fNewManifestFileBrowseButton);
		int height2= SWTUtil.getButtonHeigthHint(fNewManifestFileBrowseButton);
		width= Math.max(width, width2);
		height= Math.max(height, height2);

		width2= SWTUtil.getButtonWidthHint(fSealedPackagesDetailsButton);
		height2= SWTUtil.getButtonHeigthHint(fSealedPackagesDetailsButton);
		width= Math.max(width, width2);
		height= Math.max(height, height2);

		width2= SWTUtil.getButtonWidthHint(fUnSealedPackagesDetailsButton);
		height2= SWTUtil.getButtonHeigthHint(fUnSealedPackagesDetailsButton);
		width= Math.max(width, width2);
		height= Math.max(height, height2);

		width2= SWTUtil.getButtonWidthHint(fMainClassBrowseButton);
		height2= SWTUtil.getButtonHeigthHint(fMainClassBrowseButton);
		width= Math.max(width, width2);
		height= Math.max(height, height2);

		setSize(fManifestFileBrowseButton, width, height);
		setSize(fNewManifestFileBrowseButton, width, height);
		setSize(fSealedPackagesDetailsButton, width, height);
		setSize(fUnSealedPackagesDetailsButton, width, height);
		setSize(fMainClassBrowseButton, width, height);		
	}	
	
	/**
	 * Creates a horizontal spacer line that fills the width of its container.
	 *
	 * @param parent the parent control
	 */
	protected void createSpacer(Composite parent) {
		Label spacer= new Label(parent, SWT.NONE);
		GridData data= new GridData();
		data.horizontalAlignment= GridData.FILL;
		data.verticalAlignment= GridData.BEGINNING;
		spacer.setLayoutData(data);
	}
	/**
	 * Returns the resource for the specified path.
	 *
	 * @param path	the path for which the resource should be returned
	 * @return the resource specified by the path or <code>null</code>
	 */
	protected IResource findResource(IPath path) {
		IWorkspace workspace= JavaPlugin.getWorkspace();
		IStatus result= workspace.validatePath(
							path.toString(),
							IResource.ROOT | IResource.PROJECT | IResource.FOLDER | IResource.FILE);
		if (result.isOK() && workspace.getRoot().exists(path))
			return workspace.getRoot().findMember(path);
		return null;
	}

	protected IPath getPathFromString(String text) {
		return new Path(text).makeAbsolute();
	}
	/**
	 * Creates a selection dialog that lists all packages under the given package 
	 * fragment root.
	 * The caller is responsible for opening the dialog with <code>Window.open</code>,
	 * and subsequently extracting the selected packages (of type
	 * <code>IPackageFragment</code>) via <code>SelectionDialog.getResult</code>.
	 * 
	 * @param packageFragments the package fragments
	 * @return a new selection dialog
	 */
	protected SelectionDialog createPackageDialog(Set packageFragments) {
		List packages= new ArrayList(packageFragments.size());
		for (Iterator iter= packageFragments.iterator(); iter.hasNext();) {
			IPackageFragment fragment= (IPackageFragment)iter.next();
			boolean containsJavaElements= false;
			int kind;
			try {
				kind= fragment.getKind();
				containsJavaElements= fragment.getChildren().length > 0;
			} catch (JavaModelException ex) {
				ExceptionHandler.handle(ex, getContainer().getShell(), JarPackagerMessages.getString("JarManifestWizardPage.error.jarPackageWizardError.title"), JarPackagerMessages.getFormattedString("JarManifestWizardPage.error.jarPackageWizardError.message", fragment.getElementName())); //$NON-NLS-2$ //$NON-NLS-1$
				continue;
			}
			if (kind != IPackageFragmentRoot.K_BINARY && containsJavaElements)
				packages.add(fragment);
		}
		StandardJavaElementContentProvider cp= new StandardJavaElementContentProvider() {
			public boolean hasChildren(Object element) {
				// prevent the + from being shown in front of packages
				return !(element instanceof IPackageFragment) && super.hasChildren(element);
			}
		};
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getContainer().getShell(), new JavaElementLabelProvider(JavaElementLabelProvider.SHOW_DEFAULT), cp);
		dialog.setDoubleClickSelects(false);
		dialog.setInput(JavaCore.create(JavaPlugin.getWorkspace().getRoot()));
		dialog.addFilter(new EmptyInnerPackageFilter());		
		dialog.addFilter(new LibraryFilter());
		dialog.addFilter(new SealPackagesFilter(packages));
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				StatusInfo res= new StatusInfo();
				for (int i= 0; i < selection.length; i++) {
					if (!(selection[i] instanceof IPackageFragment)) {
						res.setError(JarPackagerMessages.getString("JarManifestWizardPage.error.mustContainPackages")); //$NON-NLS-1$
						return res;
					}
				}
				res.setOK();
				return res;
			}
		});
		return dialog;		
	}
	/**
	 * Converts selection dialog results into an array of IPackageFragments.
	 * An empty array is returned in case of errors.
	 * @throws ClassCastException if results are not IPackageFragments
	 */
	protected IPackageFragment[] getPackagesFromDialog(SelectionDialog dialog) {
		if (dialog.getReturnCode() == SelectionDialog.OK && dialog.getResult().length > 0)
			return (IPackageFragment[])Arrays.asList(dialog.getResult()).toArray(new IPackageFragment[dialog.getResult().length]);
		else
			return new IPackageFragment[0];
	}
	/**
	 * Creates and returns a dialog to choose an existing workspace file.
	 */	
	protected ElementTreeSelectionDialog createWorkspaceFileSelectionDialog(String title, String message) {
		int labelFlags= JavaElementLabelProvider.SHOW_BASICS
						| JavaElementLabelProvider.SHOW_OVERLAY_ICONS
						| JavaElementLabelProvider.SHOW_SMALL_ICONS;
		ITreeContentProvider contentProvider= new StandardJavaElementContentProvider();
		ILabelProvider labelProvider= new JavaElementLabelProvider(labelFlags);
		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(getShell(), labelProvider, contentProvider); 
		dialog.setAllowMultiple(false);
		dialog.setValidator(new ISelectionStatusValidator() {
			public IStatus validate(Object[] selection) {
				StatusInfo res= new StatusInfo();
				// only single selection
				if (selection.length == 1 && (selection[0] instanceof IFile))
					res.setOK();
				else
					res.setError(""); //$NON-NLS-1$
				return res;
			}
		});
		dialog.addFilter(new EmptyInnerPackageFilter());
		dialog.addFilter(new LibraryFilter());
		dialog.setTitle(title);
		dialog.setMessage(message);
		dialog.setStatusLineAboveButtons(true);
		dialog.setInput(JavaCore.create(JavaPlugin.getWorkspace().getRoot()));
		return dialog;
	}

	/**
	 * Returns the minimal set of packages which contain all the selected Java resources.
	 * @return	the Set of IPackageFragments which contain all the selected resources
	 */
	private Set getPackagesForSelectedResources(JarPackageData jarPackage) {
		Set packages= new HashSet();
		int n= fJarPackage.getElements().length;
		for (int i= 0; i < n; i++) {
			Object element= fJarPackage.getElements()[i];
			if (element instanceof ICompilationUnit) {
				IJavaElement pack= JarPackagerUtil.findParentOfKind((IJavaElement)element, org.eclipse.jdt.core.IJavaElement.PACKAGE_FRAGMENT);
				if (pack != null)
					packages.add(pack);
			}
		}
		return packages;
	}
}
