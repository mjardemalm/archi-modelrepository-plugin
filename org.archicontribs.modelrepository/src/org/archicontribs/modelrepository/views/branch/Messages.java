package org.archicontribs.modelrepository.views.branch;

import org.eclipse.osgi.util.NLS;

public class Messages extends NLS {

    private static final String BUNDLE_NAME = "org.archicontribs.modelrepository.views.branch.messages"; //$NON-NLS-1$

    public static String BranchInfoTableViewer_0;

    public static String BranchInfoTableViewer_1;
    
    public static String BranchInfoView_1;

    static {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages() {
    }
}
