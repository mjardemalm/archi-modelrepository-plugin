/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.actions;

import org.archicontribs.modelrepository.IModelRepositoryImages;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * Undo the last commit
 */
public class MergeBranchToMasterAction extends AbstractModelAction {
    
    public MergeBranchToMasterAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_UNDO_COMMIT));
        setText(Messages.MergeBranchToMasterAction_0);
        setToolTipText(Messages.MergeBranchToMasterAction_0);
    }

    @Override
    public void run() {
    }
    
    @Override
    protected boolean shouldBeEnabled() {
//        if(!super.shouldBeEnabled()) {
//            return false;
//        }
        
        // Should check that not current selected branch is the branch
        // And that there is no uncommitted changes tracked
        
        return false;
    }

}
