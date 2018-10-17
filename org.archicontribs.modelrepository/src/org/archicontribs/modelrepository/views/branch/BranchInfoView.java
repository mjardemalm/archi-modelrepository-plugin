/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.branch;

import org.archicontribs.modelrepository.ModelRepositoryPlugin;
import org.archicontribs.modelrepository.actions.AddNewBranchAction;
import org.archicontribs.modelrepository.actions.CheckoutBranchAction;
import org.archicontribs.modelrepository.actions.DeleteBranchAction;
import org.archicontribs.modelrepository.actions.ExtractModelFromCommitAction;
import org.archicontribs.modelrepository.actions.MergeBranchToMasterAction;
import org.archicontribs.modelrepository.actions.ResetToRemoteCommitAction;
import org.archicontribs.modelrepository.actions.RestoreCommitAction;
import org.archicontribs.modelrepository.actions.UndoLastCommitAction;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.grafico.RepositoryListenerManager;
import org.archicontribs.modelrepository.views.branch.Messages;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.IContext;
import org.eclipse.help.IContextProvider;
import org.eclipse.jface.action.ICoolBarManager;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.ViewPart;

import com.archimatetool.editor.ui.components.UpdatingTableColumnLayout;
import com.archimatetool.model.IArchimateModel;


/**
 * History Viewpart
 */
public class BranchInfoView
extends ViewPart
implements IContextProvider, ISelectionListener, IRepositoryListener {

	public static String ID = ModelRepositoryPlugin.PLUGIN_ID + ".branchInfoView"; //$NON-NLS-1$
	public static String HELP_ID = ModelRepositoryPlugin.PLUGIN_ID + ".modelRepositoryViewHelp"; //$NON-NLS-1$
    
    /**
     * The Viewer
     */
    private BranchInfoTableViewer fTableViewer;
//    private CLabel fRepoLabel;
//    private RevisionCommentViewer fCommentViewer;
    
    /*
     * Actions
     */
    private CheckoutBranchAction fActionCheckoutBranch;
    private MergeBranchToMasterAction fActionMergeBranchToMaster;
    private DeleteBranchAction fActionDeleteBranch;
    private AddNewBranchAction fActionAddNewBranch;
    
    /*
     * Selected repository
     */
    private IArchiRepository fSelectedRepository;
    
    @Override
    public void createPartControl(Composite parent) {
        GridLayout layout = new GridLayout();
        layout.marginWidth = 0;
        layout.marginHeight = 0;
        layout.verticalSpacing = 0;
        parent.setLayout(layout);
        
//        fRepoLabel = new CLabel(parent, SWT.NONE);
//        fRepoLabel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
//        fRepoLabel.setText(Messages.HistoryView_0);
        
        SashForm sash = new SashForm(parent, SWT.VERTICAL);
        GridData gd = new GridData(SWT.FILL, SWT.FILL, true, true);
        sash.setLayoutData(gd);
        
        Composite tableComp = new Composite(sash, SWT.NONE);
        
        // This ensures a minumum and equal size and no horizontal size creep
        gd = new GridData(GridData.FILL_BOTH);
        gd.widthHint = 100;
        gd.heightHint = 50;
        tableComp.setLayoutData(gd);

        tableComp.setLayout(new UpdatingTableColumnLayout(tableComp));
        
        // Create the Viewer first
        fTableViewer = new BranchInfoTableViewer(tableComp);
//        
//        // Comments Viewer
//        fCommentViewer = new RevisionCommentViewer(sash);
//        
//        sash.setWeights(new int[] { 80, 20 });
//        
        makeActions();
        hookContextMenu();
//        //makeLocalMenuActions();
        makeLocalToolBarActions();
//        
        // Register us as a selection provider so that Actions can pick us up
        getSite().setSelectionProvider(getViewer());
        
        /*
         * Listen to Selections to update local Actions
         */
        getViewer().addSelectionChangedListener(new ISelectionChangedListener() {
            public void selectionChanged(SelectionChangedEvent event) {
                updateActions(event.getSelection());
            }
        });
        
        /*
         * Listen to Double-click Action
         */
        getViewer().addDoubleClickListener(new IDoubleClickListener() {
            public void doubleClick(DoubleClickEvent event) {
            }
        });
        
        // Listen to workbench selections
        getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);

        // Register Help Context
        PlatformUI.getWorkbench().getHelpSystem().setHelp(getViewer().getControl(), HELP_ID);
        
        // Initialise with whatever is selected in the workbench
        IWorkbenchPart part = getSite().getWorkbenchWindow().getPartService().getActivePart();
        if(part != null) {
            selectionChanged(part, getSite().getWorkbenchWindow().getSelectionService().getSelection());
        }
        
        // Add listener
        RepositoryListenerManager.INSTANCE.addListener(this);
    }
    
    /**
     * Make local actions
     */
    protected void makeActions() {
    	fActionCheckoutBranch = new CheckoutBranchAction(getViewSite().getWorkbenchWindow());
    	fActionCheckoutBranch.setEnabled(true);
    	fActionAddNewBranch = new AddNewBranchAction(getViewSite().getWorkbenchWindow());
    	fActionAddNewBranch.setEnabled(true);
    	fActionMergeBranchToMaster = new MergeBranchToMasterAction(getViewSite().getWorkbenchWindow());
    	fActionMergeBranchToMaster.setEnabled(true);
    	fActionDeleteBranch = new DeleteBranchAction(getViewSite().getWorkbenchWindow());
    	fActionDeleteBranch.setEnabled(true);
    	
        // Register the Keybinding for actions
//        IHandlerService service = (IHandlerService)getViewSite().getService(IHandlerService.class);
//        service.activateHandler(fActionRefresh.getActionDefinitionId(), new ActionHandler(fActionRefresh));
    }

    /**
     * Hook into a right-click menu
     */
    protected void hookContextMenu() {
        MenuManager menuMgr = new MenuManager("#BranchInfoPopupMenu"); //$NON-NLS-1$
        menuMgr.setRemoveAllWhenShown(true);
        
        menuMgr.addMenuListener(new IMenuListener() {
            public void menuAboutToShow(IMenuManager manager) {
                fillContextMenu(manager);
            }
        });
        
        Menu menu = menuMgr.createContextMenu(getViewer().getControl());
        getViewer().getControl().setMenu(menu);
        
        getSite().registerContextMenu(menuMgr, getViewer());
    }
    
    /**
     * Make Any Local Bar Menu Actions
     */
//    protected void makeLocalMenuActions() {
//        IActionBars actionBars = getViewSite().getActionBars();
//
//        // Local menu items go here
//        IMenuManager manager = actionBars.getMenuManager();
//        manager.add(new Action("&View Management...") {
//            public void run() {
//                MessageDialog.openInformation(getViewSite().getShell(),
//                        "View Management",
//                        "This is a placeholder for the View Management Dialog");
//            }
//        });
//    }

    /**
     * Make Local Toolbar items
     */
    protected void makeLocalToolBarActions() {
        IActionBars bars = getViewSite().getActionBars();
        IToolBarManager manager = bars.getToolBarManager();

        manager.add(new Separator(IWorkbenchActionConstants.NEW_GROUP));
        
        manager.add(fActionCheckoutBranch);
        manager.add(fActionAddNewBranch);
        manager.add(fActionDeleteBranch);
        manager.add(new Separator());
        manager.add(fActionMergeBranchToMaster);
        
        manager.add(new Separator());
    }
    
    /**
     * Update the Local Actions depending on the local selection 
     * @param selection
     */
    public void updateActions(ISelection selection) {
    	
    	Ref ref = (Ref)((IStructuredSelection)selection).getFirstElement();
    	if (ref != null) {
    		fActionCheckoutBranch.setBranch(ref.getName());
    		fActionDeleteBranch.setBranch(ref.getName());
    	}
    }
    
    protected void fillContextMenu(IMenuManager manager) {
        manager.add(fActionCheckoutBranch);
        manager.add(fActionAddNewBranch);
        manager.add(fActionDeleteBranch);
        manager.add(new Separator());
        manager.add(fActionMergeBranchToMaster);
    }

    /**
     * @return The Viewer
     */
    public TableViewer getViewer() {
        return fTableViewer;
    }
    
    @Override
    public void setFocus() {
        if(getViewer() != null) {
            getViewer().getControl().setFocus();
        }
    }
    
    @Override
    public void selectionChanged(IWorkbenchPart part, ISelection selection) {
        if(part == this || selection == null) {
            return;
        }
        
        Object selected = ((IStructuredSelection)selection).getFirstElement();
        
        IArchiRepository selectedRepository = null;
        
        // Repository selected
        if(selected instanceof IArchiRepository) {
            selectedRepository = (IArchiRepository)selected;
        }
        // Model selected, but is it in a git repo?
        else {
            IArchimateModel model = part.getAdapter(IArchimateModel.class);
            if(GraficoUtils.isModelInLocalRepository(model)) {
                selectedRepository = new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model));
            }
        }
        
        // Update if selectedRepository is different 
        if(selectedRepository != null && !selectedRepository.equals(fSelectedRepository)) {
            // Set label text
//            fRepoLabel.setText(Messages.HistoryView_0 + " " + selectedRepository.getName()); //$NON-NLS-1$
            getViewer().setInput(selectedRepository);
            
            // Do the table kludge
            ((UpdatingTableColumnLayout)getViewer().getTable().getParent().getLayout()).doRelayout();

            // Update actions
//            fActionExtractCommit.setRepository(selectedRepository);
//            fActionRestoreCommit.setRepository(selectedRepository);
//            fActionUndoLastCommit.setRepository(selectedRepository);
//            fActionResetToRemoteCommit.setRepository(selectedRepository);
            fActionCheckoutBranch.setRepository(selectedRepository);
            
            // Select first row
            Display.getDefault().asyncExec(new Runnable() {
                @Override
                public void run() {
                    if(!getViewer().getTable().isDisposed()) {
                        Object element = getViewer().getElementAt(0);
                        if(element != null) {
                            getViewer().setSelection(new StructuredSelection(element));
                        }
                    }
                }
            });

            // Store last selected
            fSelectedRepository = selectedRepository;
        }
    }
    
    @Override
    public void repositoryChanged(String eventName, IArchiRepository repository) {
    	// Will be fired when a repository is changed since instance
    	// Since instance is registered as a listener.
    
    	// Probably have to reload the list upon change.
    	
    	if(repository.equals(fSelectedRepository)) {
            switch(eventName) {
//                case IRepositoryListener.HISTORY_CHANGED:
////                    fRepoLabel.setText(Messages.HistoryView_0 + " " + repository.getName()); //$NON-NLS-1$
//                    getViewer().setInput(repository);
//                    break;
                    
                case IRepositoryListener.REPOSITORY_DELETED:
//                    fRepoLabel.setText(Messages.HistoryView_0);
                    getViewer().setInput(repository);
                    fSelectedRepository = null; // Reset this
                    break;
                    
                case IRepositoryListener.REPOSITORY_CHANGED:
//                    fRepoLabel.setText(Messages.HistoryView_0 + " " + repository.getName()); //$NON-NLS-1$
                    break;

                default:
                    break;
            }
        }
    }
    
    @Override
    public void dispose() {
        super.dispose();
        getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
        RepositoryListenerManager.INSTANCE.removeListener(this);
    }
    

    // =================================================================================
    //                       Contextual Help support
    // =================================================================================
    
    public int getContextChangeMask() {
        return NONE;
    }

    public IContext getContext(Object target) {
        return HelpSystem.getContext(HELP_ID);
    }

    public String getSearchExpression(Object target) {
        return Messages.BranchInfoView_1;
    }
}
