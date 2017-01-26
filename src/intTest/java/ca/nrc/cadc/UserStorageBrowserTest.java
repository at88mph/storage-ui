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
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.Select;
import ca.nrc.cadc.web.selenium.AbstractWebApplicationIntegrationTest;


public class UserStorageBrowserTest extends AbstractWebApplicationIntegrationTest
{
	static final String STORAGE_ENDPOINT = "storage/list";

    @Test
    public void browseUserStorage() throws Exception
    {

    	UserStorageBrowserPage userStoragePage =
                goTo(STORAGE_ENDPOINT, null, UserStorageBrowserPage.class);

		String testFolderName = "CADCtest";

    	// TODO: First landing -  check:
    	// - basic elements are on page
		// - not logged in
    	// - Name is primary sort
		// Test default sort on Name column
		verifyTrue(userStoragePage.isDefaultSort());


    	// Scenario 1:
    	// enter search(filter) value
    	// check that rows of table are shorted correctly
    	// verify entry is correct

    	// Should short to one entry, verify the name and quit.
    	userStoragePage.enterSearch(testFolderName);

		// Verify filter worked correctly
    	int rowCount = userStoragePage.getTableRowCount();
//    	System.out.println("Rowcount: " + rowCount);
    	verifyTrue(rowCount < 3);
    	verifyTrue(userStoragePage.verifyFolderName(rowCount-1, testFolderName));

		// Next tests to run after this (as part of page clickthrough test)
		// click on the row entry

		// click through to CADCtest folder
		userStoragePage.clickFolder(testFolderName);
		// Verify sub folder page state
		verifyTrue(userStoragePage.isSubFolder(testFolderName));

		// Check permissions on page
		// TODO: best way to determine write access is???
		verifyTrue(userStoragePage.isReadAccess());



    	// Scenario 2: Login test - credentials should be in the gradle build file.
		userStoragePage.doLogin("CADCtest","sywymUL4");
		verifyTrue(userStoragePage.isLoggedIn());
		System.out.println("logged in");

		rowCount = userStoragePage.getTableRowCount();

		System.out.println("Rowcount: " + rowCount);
		verifyTrue(rowCount > 2);

		// Check access to page
		verifyTrue(userStoragePage.isReadAccess());



		// Scenario 3: Test navigation buttons
		// TODO: better test here is to have two levels to navigate through,

		// Test state is currently in a subfolder: Start at Root
		userStoragePage.navToRoot();
		// Verify in Root Folder
		verifyTrue(userStoragePage.isRootFolder());

		int startRow = 1;

		System.out.println("Starting navigation tests");
		// click through to first folder
		int firstPageRowClicked = userStoragePage.getNextAvailabileFolderRow(startRow);
		String subFolder1 = userStoragePage.getFolderName(firstPageRowClicked);
		userStoragePage.clickFolderForRow(firstPageRowClicked);
		verifyTrue(userStoragePage.isSubFolder(subFolder1));

		// Go down one more level
		int secondPageRowCilcked = userStoragePage.getNextAvailabileFolderRow(startRow);
		String subFolder2 = userStoragePage.getFolderName(secondPageRowCilcked);
		userStoragePage.clickFolderForRow(secondPageRowCilcked);
		verifyTrue(userStoragePage.isSubFolder(subFolder2));

		// Navigate up one level (should be up one level)
		userStoragePage.navUpLevel();
		verifyTrue(userStoragePage.isSubFolder(subFolder1));

		// Go back down one folder
		userStoragePage.clickFolderForRow(secondPageRowCilcked);
		verifyTrue(userStoragePage.isSubFolder(subFolder2));

		// Go up to root
		userStoragePage.navToRoot();
		// Verify in Root Folder
		verifyTrue(userStoragePage.isRootFolder());


		System.out.println("Test logout");
		// Scenario 4: logout
		userStoragePage.doLogout();
		verifyFalse(userStoragePage.isLoggedIn());


		System.out.println("testing file actions");

		// Scenario 5: test selecting a file
		userStoragePage.clickFolderForRow(firstPageRowClicked);
		userStoragePage.clickCheckboxForRow(startRow);
		verifyTrue(userStoragePage.isFileSelectedMode(startRow));

		userStoragePage.clickCheckboxForRow(startRow);
		verifyFalse(userStoragePage.isFileSelectedMode(startRow));



    	
    	// Scenario TODO:
    	// downloading a file
		// create new folder/resource
		// delete folder/resource
		// check permissions on folder/resource
		// toggle Public attribute of folder/resource

   
    	System.out.println("UserStorageBrowserTest completed");
    	
    }
}