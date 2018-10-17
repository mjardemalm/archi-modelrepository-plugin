/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import java.io.IOException;
import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.archicontribs.modelrepository.authentication.UsernamePassword;
import org.archicontribs.modelrepository.grafico.ArchiRepository;
import org.archicontribs.modelrepository.grafico.GraficoModelLoader;
import org.archicontribs.modelrepository.grafico.GraficoUtils;
import org.archicontribs.modelrepository.grafico.IRepositoryListener;
import org.eclipse.ui.IWorkbenchWindow;
import com.archimatetool.model.IArchimateModel;

/**
 * Checkout branch
 */
public class CheckoutBranchAction extends AbstractModelAction {
    
	private String fBranch;
	
    public CheckoutBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_REFRESH));
        setText(Messages.CheckoutBranchAction_0);
        setToolTipText(Messages.CheckoutBranchAction_0);
    }

    public CheckoutBranchAction(IWorkbenchWindow window, IArchimateModel model) {
        this(window);
        if(model != null) {
            setRepository(new ArchiRepository(GraficoUtils.getLocalRepositoryFolderForModel(model)));
        }
    }    
    
    @Override
    public void run() {
    	
        // Get User Credentials first
        UsernamePassword up = getUserNameAndPasswordFromCredentialsFileOrDialog(fWindow.getShell());    	
    	
        // Fetch and checkout
        // Reload the model from the Grafico XML files
    	try {
			getRepository().fetchFromRemote(up.getUsername(), up.getPassword(), null, false);
			getRepository().checkoutBranch(getBranch());
			
	        GraficoModelLoader loader = new GraficoModelLoader(getRepository());
	        loader.loadModel();
    	} catch (Exception e) {
            displayErrorDialog(Messages.CheckoutBranchAction_0, e);
		}
    	
        notifyChangeListeners(IRepositoryListener.HISTORY_CHANGED);

    }
    
    @Override
    protected boolean shouldBeEnabled() {
        if(!super.shouldBeEnabled()) {
            return false;
        }
        return true;
        // Should check that not current selected branch is the branch
        // And that there is no uncommitted changes tracked
        
//        return false;
    }

	public void setBranch(String name) {
		fBranch = name.substring(name.lastIndexOf("/") + 1, name.length());		
	}
	
	private String getBranch() {
		return fBranch;
	}

}
