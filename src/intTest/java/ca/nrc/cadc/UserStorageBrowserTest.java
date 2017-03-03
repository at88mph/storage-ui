/*
 ************************************************************************
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 *
 * (c) 2014.                         (c) 2014.
 * National Research Council            Conseil national de recherches
 * Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 * All rights reserved                  Tous droits reserves
 *
 * NRC disclaims any warranties         Le CNRC denie toute garantie
 * expressed, implied, or statu-        enoncee, implicite ou legale,
 * tory, of any kind with respect       de quelque nature que se soit,
 * to the software, including           concernant le logiciel, y com-
 * without limitation any war-          pris sans restriction toute
 * ranty of merchantability or          garantie de valeur marchande
 * fitness for a particular pur-        ou de pertinence pour un usage
 * pose.  NRC shall not be liable       particulier.  Le CNRC ne
 * in any event for any damages,        pourra en aucun cas etre tenu
 * whether direct or indirect,          responsable de tout dommage,
 * special or general, consequen-       direct ou indirect, particul-
 * tial or incidental, arising          ier ou general, accessoire ou
 * from the use of the software.        fortuit, resultant de l'utili-
 *                                      sation du logiciel.
 *
 *
 * @author jenkinsd
 * 15/05/14 - 1:19 PM
 *
 *
 *
 ****  C A N A D I A N   A S T R O N O M Y   D A T A   C E N T R E  *****
 ************************************************************************
 */
package ca.nrc.cadc;


import org.junit.Test;


public class UserStorageBrowserTest extends AbstractBrowserTest
{
    private static final String STORAGE_ENDPOINT = "/storage/list";


    @Test
    public void browseUserStorage() throws Exception
    {
        System.out.println("Visiting: " + getWebURL() + STORAGE_ENDPOINT);

        final String workingDirectoryName = UserStorageBrowserTest.class
                                                    .getSimpleName() + "_"
                                            + generateAlphaNumeric(16);

        UserStorageBrowserPage userStoragePage =
                goTo(STORAGE_ENDPOINT, null,
                     UserStorageBrowserPage.class);

        final String testFolderName = "CADCtest";

        verifyTrue(userStoragePage.isDefaultSort());

        // Scenario 1:
        // enter search(filter) value
        // check that rows of table are shorted correctly
        // verify entry is correct

        userStoragePage.enterSearch(testFolderName);
        int rowCount = userStoragePage.getTableRowCount();
        verifyTrue(rowCount < 3);
        verifyTrue(userStoragePage
                           .verifyFolderName(rowCount - 1, testFolderName));
        verifyTrue(userStoragePage.verifyFolderSize(rowCount - 1));


        // Verify page permissions prior to logging in
        // click through to CADCtest folder
        userStoragePage = userStoragePage.clickFolder(testFolderName);

        // Verify sub folder page state
        verifyTrue(userStoragePage.isSubFolder(testFolderName));

        // Check permissions on page
        verifyTrue(userStoragePage.isReadAccess());


        // Scenario 2: Login test - credentials should be in the gradle build file.
        userStoragePage = userStoragePage.doLogin("CADCtest", "sywymUL4");
        verifyTrue(userStoragePage.isLoggedIn());
        System.out.println("logged in");

        rowCount = userStoragePage.getTableRowCount();

        System.out.println("Rowcount: " + rowCount);
        verifyTrue(rowCount > 2);

        // Check access to page: should be write accessible
        verifyFalse(userStoragePage.isReadAccess());


        // Scenario 3: Test navigation buttons
        // Test state is currently in a subfolder: Start at Root
        System.out.println("navigating to root...");
        userStoragePage = userStoragePage.navToRoot();
        // Verify in Root Folder
        verifyTrue(userStoragePage.isRootFolder());

        int startRow = 1;

        System.out.println("Starting navigation tests");
        // click through to first folder
        int firstPageRowClicked = userStoragePage
                .getNextAvailabileFolderRow(startRow);
        String subFolder1 = userStoragePage.getFolderName(firstPageRowClicked);
        userStoragePage.clickFolderForRow(firstPageRowClicked);
        verifyTrue(userStoragePage.isSubFolder(subFolder1));
        verifyTrue(userStoragePage.quotaIsDisplayed());

        // Go down one more level
        int secondPageRowCilcked = userStoragePage
                .getNextAvailabileFolderRow(startRow);
        String subFolder2 = userStoragePage.getFolderName(secondPageRowCilcked);
        userStoragePage.clickFolderForRow(secondPageRowCilcked);
        verifyTrue(userStoragePage.isSubFolder(subFolder2));

        // Navigate up one level (should be up one level)
        userStoragePage = userStoragePage.navUpLevel();
        verifyTrue(userStoragePage.isSubFolder(subFolder1));

        // Go back down one folder
        userStoragePage.clickFolderForRow(secondPageRowCilcked);
        verifyTrue(userStoragePage.isSubFolder(subFolder2));

        // Go up to root
        userStoragePage = userStoragePage.navToRoot();

        // Verify in Root Folder
        verifyTrue(userStoragePage.isRootFolder());
        verifyFalse(userStoragePage.quotaIsDisplayed());


        // Scenario 4: test file actions
        System.out.println("testing file actions");
        userStoragePage.clickFolderForRow(firstPageRowClicked);
        userStoragePage.clickCheckboxForRow(startRow);
        verifyTrue(userStoragePage.isFileSelectedMode(startRow));

        userStoragePage.clickCheckboxForRow(startRow);
        verifyFalse(userStoragePage.isFileSelectedMode(startRow));

        // Go up to root
        userStoragePage = userStoragePage.navToRoot();
        verifyTrue(userStoragePage.isRootFolder());
        //  click through to CADCtest folder
        userStoragePage = userStoragePage.clickFolder(testFolderName);
        // Verify sub folder page state
        verifyTrue(userStoragePage.isSubFolder(testFolderName));


        // navigate to automated test folder
        String autoTestFolder = "automated_test";

        // Get Write and Read group permissions for this folder
        userStoragePage.enterSearch(autoTestFolder);

        // For whatever reason the automated test folder has been deleted.
        // Recreate it.
        if (userStoragePage.isTableEmpty())
        {
            userStoragePage = userStoragePage.createNewFolder(autoTestFolder);
            userStoragePage.enterSearch(autoTestFolder);
        }

        String parentWriteGroup = userStoragePage.getValueForRowCol(1, 5);
        String parentReadGroup = userStoragePage.getValueForRowCol(1, 6);
        userStoragePage.clickFolder(autoTestFolder);

        // Create a context group, and run tests in there
        userStoragePage = userStoragePage.createNewFolder(workingDirectoryName);
        userStoragePage.enterSearch(workingDirectoryName);
        userStoragePage = userStoragePage.clickFolder(workingDirectoryName);

        // Create second test folder
        // This will be deleted at the end of this test suite
        String tempTestFolder = "vosui_automated_test_tobedeleted_"
                                + generateAlphaNumeric(8);
        userStoragePage = userStoragePage.createNewFolder(tempTestFolder);
        final boolean isPublic = parentReadGroup.equals("Public");

        // Test that permissions are same as the parent to start
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, parentReadGroup, isPublic));

        // Edit permissions on the form
        String currentReadGroup =
                userStoragePage.getValueForRowCol(1, 6);

        // Clearly only works for English test suite. :/
        // Toggle the Public attribute to get the underlying read group (if any)
        if (currentReadGroup.equals("Public"))
        {
            userStoragePage = userStoragePage.togglePublicAttributeForRow();
            userStoragePage.getValueForRowCol(1, 6);
        }

        String readGroupName = "cadcsw";
        String writeGroupName = "cadc-dev";
        String invalidGroupName = "invalid-group";

        // Don't change anything, verify that the correct message is displayed
        userStoragePage.clickEditIconForFirstRow();
        userStoragePage.clickButton(UserStorageBrowserPage.SAVE);
//        userStoragePage.confirmJQIMessageText(UserStorageBrowserPage.NOT_MODIFIED);
//        userStoragePage.confirmJqiMsg(UserStorageBrowserPage.NOT_MODIFIED);
//        userStoragePage.waitForPromptFinish();
        userStoragePage.clickButton(UserStorageBrowserPage.CANCEL);

        PermissionsFormData formData = userStoragePage.getValuesFromEditIcon();
        Boolean isModifyNode = true;
        // Set read group to blank (owner access only)
        // Depending on whether the permissions on automated_test parent folder have been changed,
        // the readGroup may not be set initially.
        // Read group may be displayed as 'public', where the read group itself may not be that.
        // The element grabbd here is not visible, but is a reflection of the input to the
        // permissions editing form - attached to the edit icon (the glyphicon-pencil)
        if (formData.getReadGroup().equals(""))
        {
            isModifyNode = false;
        }
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.READ_GROUP_INPUT, "", isModifyNode);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, "", false));
        userStoragePage.waitForPromptFinish();

        isModifyNode = true;
        // Set read group to selected group
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.READ_GROUP_INPUT, readGroupName, true);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, parentWriteGroup, readGroupName, false));

        // Set write group to blank
        if (formData.getWriteGroup().equals(""))
        {
            isModifyNode = false;
        }
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.WRITE_GROUP_INPUT, "", isModifyNode);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, "", readGroupName, false));
        userStoragePage = userStoragePage
                .setGroup(UserStorageBrowserPage.WRITE_GROUP_INPUT, writeGroupName, true);
        verifyTrue(userStoragePage
                           .isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // last parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT, invalidGroupName, false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.READ_GROUP_DIV));

        readGroupName = "CHIMPS";
        // Enter correct one in order to close the prompt
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.READ_GROUP_INPUT, readGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Test response to invalid autocomplete selection
        userStoragePage.clickEditIconForFirstRow();
        // second parameter says 'don't confirm anything'
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT, invalidGroupName, false);
        verifyTrue(userStoragePage.isGroupError(UserStorageBrowserPage.WRITE_GROUP_DIV));

        // Enter correct one in order to close the prompt
        writeGroupName = "CHIMPS";
        userStoragePage = userStoragePage.setGroupOnly(UserStorageBrowserPage.WRITE_GROUP_INPUT, writeGroupName, true);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        // Toggle public permissions to set them
        // Group name displayed in table should read "Public"
        userStoragePage = userStoragePage.togglePublicAttributeForRow();

        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, "Public", true));
        System.out.println("Set read group to public");

        // Toggle public permission to unset
        userStoragePage = userStoragePage.togglePublicAttributeForRow();
        verifyTrue(userStoragePage.isPermissionDataForRow(1, writeGroupName, readGroupName, false));

        userStoragePage.enterSearch(tempTestFolder);
        userStoragePage = userStoragePage.clickFolder(tempTestFolder);

        String recursiveTestFolder = "recursive_tobedeleted"; // + generateAlphaNumeric(8);
        userStoragePage = userStoragePage.createNewFolder(recursiveTestFolder);

        userStoragePage = userStoragePage.navUpLevel();

        userStoragePage.applyRecursivePermissions(UserStorageBrowserPage.WRITE_GROUP_INPUT, "cadcsw");
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));

        userStoragePage = userStoragePage.clickFolder(tempTestFolder);
        verifyTrue(userStoragePage.isPermissionDataForRow(1, "cadcsw", readGroupName, false));


        // Test Delete and clean up
		// Delete folder just created

        userStoragePage.enterSearch(recursiveTestFolder);
        userStoragePage.clickCheckboxForRow(1);
        userStoragePage = userStoragePage.deleteFolder();
        userStoragePage = userStoragePage.navUpLevel();

        userStoragePage.enterSearch(tempTestFolder);
        userStoragePage.clickCheckboxForRow(1);

        userStoragePage = userStoragePage.deleteFolder();

		// verify the folder is no longer there
		userStoragePage.enterSearch(tempTestFolder);
		verifyTrue(userStoragePage.isTableEmpty());

		// Nav up one level & delete working folder as well
		userStoragePage = userStoragePage.navUpLevel();
		userStoragePage.enterSearch(workingDirectoryName);
		userStoragePage.clickCheckboxForRow(1);
        userStoragePage = userStoragePage.deleteFolder();

		// Scenario 5: logout
		System.out.println("Test logout");
        userStoragePage = userStoragePage.doLogout();
		verifyFalse(userStoragePage.isLoggedIn());
   
    	System.out.println("UserStorageBrowserTest completed");
    }
}
