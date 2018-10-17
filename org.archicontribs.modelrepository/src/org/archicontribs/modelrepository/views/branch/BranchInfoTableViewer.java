/**
 * This program and the accompanying materials
 * are made available under the terms of the License
 * which accompanies this distribution in the file LICENSE.txt
 */
package org.archicontribs.modelrepository.views.branch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.archicontribs.modelrepository.grafico.IArchiRepository;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.CellLabelProvider;
import org.eclipse.jface.viewers.ColumnViewerToolTipSupport;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand.ListMode;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;


/**
 * History Table Viewer
 */
public class BranchInfoTableViewer extends TableViewer {
    
    /**
     * Constructor
     */
    public BranchInfoTableViewer(Composite parent) {
        super(parent, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | SWT.FULL_SELECTION | SWT.VIRTUAL);
        
        setup(parent);
        
        setContentProvider(new BranchContentProvider());
        setLabelProvider(new BranchLabelProvider());
        
        ColumnViewerToolTipSupport.enableFor(this);
    }

    /**
     * Set things up.
     */
    protected void setup(Composite parent) {
        getTable().setHeaderVisible(true);
        getTable().setLinesVisible(false);
        
        TableColumnLayout tableLayout = (TableColumnLayout)parent.getLayout();
        
        TableViewerColumn column = new TableViewerColumn(this, SWT.NONE, 0);
        column.getColumn().setText(Messages.BranchInfoTableViewer_0);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(10, false));
        
        column = new TableViewerColumn(this, SWT.NONE, 1);
        column.getColumn().setText(Messages.BranchInfoTableViewer_1);
        tableLayout.setColumnData(column.getColumn(), new ColumnWeightData(50, false));

    }
    
    // ===============================================================================================
	// ===================================== Table Model ==============================================
	// ===============================================================================================
    
    /**
     * The Model for the Table.
     */
    class BranchContentProvider implements IStructuredContentProvider {
    	List<Ref> branches = new ArrayList<Ref>();
        
        public void inputChanged(Viewer v, Object oldInput, Object newInput) {
        	branches = new ArrayList<Ref>();
            
            if(!(newInput instanceof IArchiRepository)) {
                return;
            }
            
            IArchiRepository repo = (IArchiRepository)newInput;
            
            // Local Repo was deleted
            if(!repo.getLocalRepositoryFolder().exists()) {
                return;
            }
            
            try(Git git = Git.open(repo.getLocalRepositoryFolder())) {                
            	branches = git.branchList().setListMode( ListMode.REMOTE ).call();
            }
            catch(IOException | GitAPIException ex) {
                ex.printStackTrace();
            }
        }
        
        public void dispose() {
        }
        
        public Object[] getElements(Object parent) {
        	return branches.toArray();
        }
    }
    
    // ===============================================================================================
	// ===================================== Label Model ==============================================
	// ===============================================================================================

    class BranchLabelProvider extends CellLabelProvider {
        
        public String getColumnText(Ref branch, int columnIndex) {
            switch(columnIndex) {
                case 0:
//                    return branch.getName();
                    return getUserFriendlyBranchName(branch.getName());
                case 1:
                    return branch.getName();
                    
                default:
                    return null;
            }
        }

    	private String getGitFriendlyBranchName(String str) {
    		String gitFriendlyBranchName = str.replaceAll("([\\d.]).([\\d.])", "$1_$2").replace(" ", "_").toLowerCase();
    		return gitFriendlyBranchName.equalsIgnoreCase("accepted_architecture_landscape") ? "master" : gitFriendlyBranchName;
    	}        
        
    	private String capitalizeWord(String str){  
    	    String words[] = str.split("\\s");  
    	    String capitalizeWord = "";  
    	    for(String w:words) {  
    	        String first = w.substring(0,1);  
    	        String afterfirst = w.substring(1);  
    	        capitalizeWord += first.toUpperCase() + afterfirst + " ";  
    	    }  
    	    return capitalizeWord.trim();  
    	}       
    	
    	private String getUserFriendlyBranchName(String name) {
    		// Just the substring after the last / (slash)
    		// Translate underscore between two digits with . (dot)
    		// Translate underscore to space
    		// The first letter in every word should be uppercase
    		// 'master' got a special translation

    		String niceName = name.replaceAll("([\\d.])_([\\d.])", "$1.$2").substring(name.lastIndexOf("/") + 1, name.length()).replace("_", " ");
    		return niceName.equalsIgnoreCase("master") ? "Accepted Architecture Landscape" : capitalizeWord(niceName);
    	}
    	
    	@Override
    	public void update(ViewerCell cell) {
    		if(cell.getElement() instanceof Ref) {
    			Ref branch = (Ref)cell.getElement();
    			cell.setText(getColumnText(branch, cell.getColumnIndex()));
    		}
    	}
        
        @Override
        public String getToolTipText(Object element) {
            if(element instanceof RevCommit) {
                RevCommit commit = (RevCommit)element;
                
                String s = ""; //$NON-NLS-1$
                
//                if(commit.equals(localMasterCommit) && commit.equals(originMasterCommit)) {
//                    s += Messages.HistoryTableViewer_4 + " "; //$NON-NLS-1$
//                }
//                else if(commit.equals(localMasterCommit)) {
//                    s += Messages.HistoryTableViewer_5 + " "; //$NON-NLS-1$
//                }
//
//                else if(commit.equals(originMasterCommit)) {
//                    s += Messages.HistoryTableViewer_6 + " "; //$NON-NLS-1$
//                }
//                
//                s += commit.getFullMessage().trim();
                
                return s;
            }
            
            return null;
        }
    }
}
