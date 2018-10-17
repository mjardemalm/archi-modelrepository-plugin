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
public class AddNewBranchAction extends AbstractModelAction {
    
    public AddNewBranchAction(IWorkbenchWindow window) {
        super(window);
        setImageDescriptor(IModelRepositoryImages.ImageFactory.getImageDescriptor(IModelRepositoryImages.ICON_CLONE));
        setText(Messages.AddNewBranchAction_0);
        setToolTipText(Messages.AddNewBranchAction_0);
    }

    @Override
    public void run() {
    	// Adding new branch will always be made from the current source.
    	// This means that there is no need of reload the temporary 
    	// .archimate file since it's alrady up-to-date.
    	
    	// 1. create branch (git branch branch_name)
    	// 2. checkout branch (git checkout branch_name)
    	// 3. summarized by create and checkout branch (git checkout -b branch_name)
    	
    	// It seems like its possible to push an empty new branch which 
    	// will automatically sets tracking. In that case push will never
    	// need to create a new branch only do a git push.
    	
    	// Are there any drawbacks in always creating from the current source?
    	// Should the branch always start from latest commit in origin master?
    	// If the latter case we need to fetch master from origin master
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
