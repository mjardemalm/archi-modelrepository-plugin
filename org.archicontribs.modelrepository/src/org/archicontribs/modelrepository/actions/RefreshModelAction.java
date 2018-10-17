/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.ProxyAuthenticater;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.archicontribs.modelrepository.merge.MergeConflictHandler;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jgit.api.MergeResult.MergeStatus;
import org.eclipse.jgit.api.PullResult;
import org.eclipse.jgit.api.errors.CanceledException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.RefNotAdvertisedException;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IProgressService;

import com.archimatetool.editor.model.IEditorModelManager;
import com.archimatetool.model.IArchimateModel;

/**
 * Refresh model action
 * 
 * 1. Offer to save the model
 * 2. If there are changes offer to Commit
 * 3. Get credentials for Pull
 * 4. Check Proxy
 * 5. Pull from Remote
 * 6. Handle Merge conflicts
 * 7. Reload temp file from Grafico files
 * 
 * @author Jean-Baptiste Sarrodie
 * @author Phillip Beauvoir
 */
public class RefreshModelAction extends AbstractModelAction {
    
    protected int PULL_STATUS_ERROR = -1;
    protected int PULL_STATUS_OK = 0;
    protected int PULL_STATUS_UP_TO_DATE = 1;
    protected int PULL_STATUS_MERGE_CANCEL = 2;
    
    public RefreshModelAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH));
        setText(Messages.RefreshModelAction_0);
        setToolTipText(Messages.RefreshModelAction_0);
    }
    
    public RefreshModelAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }
    
    @Override
    public void run() {
        try {
            UsernamePassword up = init();
            if(up != null) {
                int status = pull(up);
                if(status == PULL_STATUS_UP_TO_DATE) {
                    MessageDialog.openInformation(fWindow.getShell(), Messages.RefreshModelAction_0, Messages.RefreshModelAction_2);
                }
            }
        }
        catch(Exception ex) {
            displayErrorDialog(Messages.RefreshModelAction_0, ex);
        }
    }
    
    /**
     * Secures synchronization between model temporary file and
     * GRAFICO files.
     *  - Saves model (e.g. force a commit)
     *  - Export to GRAFICO files
     *  
     *  Collects credentials for synchronized model repository.
     * 
     * @return Model repository user credentials object.  
     * @throws IOException
     * @throws GitAPIException
     */
    protected UsernamePassword init() throws IOException, GitAPIException {
        // Offer to save the model if open and dirty
        // We need to do this to keep grafico and temp files in sync
        IArchimateModel model = getRepository().locateModel();
        if(model != null && IEditorModelManager.INSTANCE.isModelDirty(model)) {
            if(!offerToSaveModel(model)) {
                return null;
            }
        }
        
        // Do the Grafico Export first
        getRepository().exportModelToGraficoFiles();
        
        // Then offer to Commit
        if(getRepository().hasChangesToCommit()) {
            if(!offerToCommitChanges()) {
                return null;
            }
            notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
        }
        
        // Get User Credentials first
        UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(fWindow.getShell());
        if(up == null) {
            return null;
        }
        
        // Proxy update
        ProxyAuthenticater.update(getRepository().getOnlineRepositoryURL());

        return up;
    }
    
    /**
     * Pulls model repository from remote and calculate status.
     * 
     * Initiates a merge handler if status is merge conflict.
     * 
     * Reloads the model temporary files from the GRAFICO files.
     * 
     * Offers user to commit merged work. 
     * 
     * Notify listeners for changed model.
     * 
     * @param up User Credentials object
     * @return Status from the pull operation.
     * @throws Exception
     */
    protected int pull(UsernamePassword up) throws Exception {
        PullResult[] pullResult = new PullResult[1];
        Exception[] exception = new Exception[1];
        
        IProgressService ps = PlatformUI.getWorkbench().getProgressService();
        ps.busyCursorWhile(new IRunnableWithProgress() {
            public void run(IProgressMonitor pm) {
                try {
                    pullResult[0] = getRepository().pullFromRemote(up.getUsername(), up.getPassword(), new ProgressMonitorWrapper(pm));
                }
                catch(GitAPIException | IOException ex) {
                    exception[0] = ex;
                }
            }
        });
        
        if(exception[0] != null) {
            // If this exception is thrown then the remote is empty with no master ref, so quietly absorb this and return
            if(exception[0] instanceof RefNotAdvertisedException) {
                return PULL_STATUS_OK;
            }
            
            throw exception[0];
        }

        // Nothing more to do
        if(pullResult[0].getMergeResult().getMergeStatus() == MergeStatus.ALREADY_UP_TO_DATE) {
            return PULL_STATUS_UP_TO_DATE;
        }

        // Merge failure
        if(!pullResult[0].isSuccessful() && pullResult[0].getMergeResult().getMergeStatus() == MergeStatus.CONFLICTING) {
            // Try to handle the merge conflict
            MergeConflictHandler handler = new MergeConflictHandler(pullResult[0].getMergeResult(), getRepository(), fWindow.getShell());
            
            ps.busyCursorWhile(new IRunnableWithProgress() {
                public void run(IProgressMonitor pm) {
                    try {
                        handler.init(pm);
                    }
                    catch(IOException | CanceledException ex) {
                        exception[0] = ex;
                    }
                }
            });
            
            if(exception[0] != null) {
                handler.resetToLocalState(); // Clean up

                if(exception[0] instanceof CanceledException) {
                    return PULL_STATUS_MERGE_CANCEL;
                }
                
                throw exception[0];
            }
            
            boolean result = handler.openConflictsDialog();
            if(result) {
                handler.merge();
            }
            // User cancelled - we assume they committed all changes so we can reset
            else {
                handler.resetToLocalState();
                notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);
                return PULL_STATUS_MERGE_CANCEL;
            }
        }
        
        // Reload the model from the Grafico XML files
        GraficoModelLoader loader = new GraficoModelLoader(getRepository());
        loader.loadModel();
        
        // Do a commit if needed
        if(getRepository().hasChangesToCommit()) {
            String commitMessage = Messages.RefreshModelAction_1;
            
            // Did we restore any missing objects?
            String restoredObjects = loader.getRestoredObjectsAsString();
            
            // Add to commit message
            if(restoredObjects != null) {
                commitMessage += "\n\n" + Messages.RefreshModelAction_3 + "\n" + restoredObjects; //$NON-NLS-1$ //$NON-NLS-2$
            }

            getRepository().commitChanges(commitMessage, true);
        }
        
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);

        return PULL_STATUS_OK;
    }
}
