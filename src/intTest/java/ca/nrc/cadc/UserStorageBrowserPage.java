/*
 ************************************************************************
 *******************  CANADIAN ASTRONOMY DATA CENTRE  *******************
 **************  CENTRE CANADIEN DE DONNÉES ASTRONOMIQUES  **************
 *
 *  (c) 2016.                            (c) 2016.
 *  Government of Canada                 Gouvernement du Canada
 *  National Research Council            Conseil national de recherches
 *  Ottawa, Canada, K1A 0R6              Ottawa, Canada, K1A 0R6
 *  All rights reserved                  Tous droits réservés
 *
 *  NRC disclaims any warranties,        Le CNRC dénie toute garantie
 *  expressed, implied, or               énoncée, implicite ou légale,
 *  statutory, of any kind with          de quelque nature que ce
 *  respect to the software,             soit, concernant le logiciel,
 *  including without limitation         y compris sans restriction
 *  any warranty of merchantability      toute garantie de valeur
 *  or fitness for a particular          marchande ou de pertinence
 *  purpose. NRC shall not be            pour un usage particulier.
 *  liable in any event for any          Le CNRC ne pourra en aucun cas
 *  damages, whether direct or           être tenu responsable de tout
 *  indirect, special or general,        dommage, direct ou indirect,
 *  consequential or incidental,         particulier ou général,
 *  arising from the use of the          accessoire ou fortuit, résultant
 *  software.  Neither the name          de l'utilisation du logiciel. Ni
 *  of the National Research             le nom du Conseil National de
 *  Council of Canada nor the            Recherches du Canada ni les noms
 *  names of its contributors may        de ses  participants ne peuvent
 *  be used to endorse or promote        être utilisés pour approuver ou
 *  products derived from this           promouvoir les produits dérivés
 *  software without specific prior      de ce logiciel sans autorisation
 *  written permission.                  préalable et particulière
 *                                       par écrit.
 *
 *  This file is part of the             Ce fichier fait partie du projet
 *  OpenCADC project.                    OpenCADC.
 *
 *  OpenCADC is free software:           OpenCADC est un logiciel libre ;
 *  you can redistribute it and/or       vous pouvez le redistribuer ou le
 *  modify it under the terms of         modifier suivant les termes de
 *  the GNU Affero General Public        la “GNU Affero General Public
 *  License as published by the          License” telle que publiée
 *  Free Software Foundation,            par la Free Software Foundation
 *  either version 3 of the              : soit la version 3 de cette
 *  License, or (at your option)         licence, soit (à votre gré)
 *  any later version.                   toute version ultérieure.
 *
 *  OpenCADC is distributed in the       OpenCADC est distribué
 *  hope that it will be useful,         dans l’espoir qu’il vous
 *  but WITHOUT ANY WARRANTY;            sera utile, mais SANS AUCUNE
 *  without even the implied             GARANTIE : sans même la garantie
 *  warranty of MERCHANTABILITY          implicite de COMMERCIALISABILITÉ
 *  or FITNESS FOR A PARTICULAR          ni d’ADÉQUATION À UN OBJECTIF
 *  PURPOSE.  See the GNU Affero         PARTICULIER. Consultez la Licence
 *  General Public License for           Générale Publique GNU Affero
 *  more details.                        pour plus de détails.
 *
 *  You should have received             Vous devriez avoir reçu une
 *  a copy of the GNU Affero             copie de la Licence Générale
 *  General Public License along         Publique GNU Affero avec
 *  with OpenCADC.  If not, see          OpenCADC ; si ce n’est
 *  <http://www.gnu.org/licenses/>.      pas le cas, consultez :
 *                                  best ide for debugging gradle projects     <http://www.gnu.org/licenses/>.
 *
 *
 ************************************************************************
 */

package ca.nrc.cadc;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;
import org.openqa.selenium.support.PageFactory;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.openqa.selenium.support.ui.Select;
import ca.nrc.cadc.web.selenium.AbstractTestWebPage;
import org.openqa.selenium.NoSuchElementException;


import java.util.List;


public class UserStorageBrowserPage extends AbstractTestWebPage
{
    private static final String ROOT_FOLDER_NAME = "ROOT";
    // Define in here what elements are mode indicators

    // Elements always on the page
    @FindBy(id = "beacon_filter")
    private WebElement searchFilter;

    @FindBy(id = "beacon")
    private WebElement beaconTable;

    // element 'Showing x to y of z entries' line
    @FindBy(id = "beacon_info")
    private WebElement statusMessage;

    @FindBy(className="beacon-progress")
    private WebElement progressBar;

    // header displaying name of current folder
    @FindBy(xpath="//h2[@property='name']")
    private WebElement folderNameHeader;

    @FindBy(xpath="//*[@id=\"navbar-functions\"]/ul")
    private WebElement navbarButtonList;

    @FindBy(xpath="//*[@id=\"beacon\"]/tbody/tr[1]")
    private WebElement firstTableRow;

    // Elements present once user has navigated away from ROOT folder
    // Toobar buttons
    @FindBy(id="level-up")
    private WebElement leveUpButton;

    @FindBy(id="root")
    private WebElement rootButton;

    // class has 'disabled' in it for base case.
    @FindBy(id="newdropdown")
    private WebElement newdropdownButton;

    @FindBy(id="download")
    private WebElement downloadButton;

    @FindBy(id="delete")
    private WebElement deleteButton;

    @FindBy(id="more_details")
    private WebElement moredetailsButton;


    // Login form elements
    // TODO: put this in it's own pojo so it can be made more generic
//    LoginFormPageObject loginForm;
    // May be issues with leveraging PageFactory with @FindBy in the subclass?
    // Login Form elements
    @FindBy(id="username")
    private WebElement loginUsername;

    @FindBy(id="password")
    private WebElement loginPassword;

    @FindBy(id="submitLogin")
    private WebElement submitLoginButton;

    @FindBy(id = "logout")
    private WebElement logoutButton;


    private WebDriver driver = null;


    public UserStorageBrowserPage(final WebDriver driver) throws Exception
    {
        super(driver);
        this.driver = driver;

        // The beacon-progress bar displays "Transferring Data" while it's loading
        // the page. Firefox doesn't display whole list until the bar is green, and
        // that text is gone. Could be this test isn't sufficient but it works
        // to have intTestFirefox not fail.
        WebDriverWait wait = new WebDriverWait(driver, 10);
        wait.until(ExpectedConditions.textToBe(By.className("beacon-progress"), ""));

        PageFactory.initElements(driver, this);
    }


    // Transition functions
    public void enterSearch(final String searchString) throws Exception {
    	sendKeys(searchFilter, searchString);
    }

    public void doLogin(String username, String password) throws Exception {
        sendKeys(loginUsername, username);
        sendKeys(loginPassword, password);
        click(submitLoginButton);
        waitForElementPresent(By.id("logout"));
    }

    public void doLogout() throws Exception {
        click(logoutButton);
    }

    public void clickFolder(String folderName)
    {
        WebElement folder = beaconTable.findElement(
                By.xpath("//*[@id=\"beacon\"]/tbody/tr/td/a[text()[contains(.,'" + folderName  + "')]]"));
        System.out.println("Folder to be clicked: " + folder.getText());
        folder.click();
    }

    public void clickFolderForRow(int rowNum) throws Exception
    {
        WebElement firstCheckbox  = (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(
                        By.xpath("//*[@id=\"beacon\"]/tbody/tr[" + rowNum + "]/td[2]/a")));
        click(firstCheckbox);
    }

    public int getNextAvailabileFolderRow(int startRow) throws Exception {
        //   not all folders are clickable, go down the rows to find one
        boolean found = false;
        int rowNum = startRow;
        WebElement firstCheckbox = null;

        while (!found) {
            // This method throws an exception if the element is not found
            try {
                firstCheckbox = beaconTable.findElement(
                        By.xpath("//*[@id=\"beacon\"]/tbody/tr[" + rowNum + "]/td[2]/a"));
            } catch (Exception e) {
                rowNum++;
                continue;
            }
            found = true;
        }
        return rowNum;
    }

    public void clickCheckboxForRow(int rowNum) throws Exception
    {

        WebElement firstCheckbox  = (new WebDriverWait(driver, 10))
                .until(ExpectedConditions.elementToBeClickable(
                By.xpath("//*[@id=\"beacon\"]/tbody/tr[" + rowNum + "]/td[1]")));
        click(firstCheckbox);
    }

    public void navToRoot() throws Exception {
        click(rootButton);
    }

    public void navUpLevel() throws Exception {
        click(leveUpButton);
    }
    


    // Inspection functions
    public WebElement getProgressBar() throws Exception
    {
        System.out.println(progressBar.getText());
        return progressBar;
    }

    int getTableRowCount() throws Exception
    {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        return tableRows.size();
    }

    boolean verifyFolderName(int rowNum, String expectedValue) throws Exception
    {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        WebElement selectedRow = tableRows.get(rowNum);
        WebElement namecolumn = selectedRow.findElement(By.cssSelector("a:nth-of-type(1)"));
        System.out.println(namecolumn.getText());
        return expectedValue.equals(namecolumn.getText());

    }

    String getFolderName(int rowNum) throws Exception
    {
        List<WebElement> tableRows = beaconTable.findElements(By.tagName("tr"));
        WebElement selectedRow = tableRows.get(rowNum);
        WebElement namecolumn = selectedRow.findElement(By.cssSelector("a:nth-of-type(1)"));
        System.out.println("Foldername to be returned: " + namecolumn.getText());
        return namecolumn.getText();
    }

    String getHeaderText() throws Exception
    {
        System.out.println("Header text: " + folderNameHeader.getText());
        return folderNameHeader.getText();
    }

    boolean isLoggedIn() {
        try {
            logoutButton.isDisplayed();
            return true;
        } catch (NoSuchElementException e) {
            return false;
        }
    }

    private boolean isDisabled(WebElement webEl) {
        return webEl.getAttribute("class").contains("disabled");
    }

    public boolean isReadAccess() {
        // id = download, , newdropdown, delete are disabled
        // id = search, level-up, root are enabled

        // need to check class of these buttons, look for 'disabled' in there
        if (isDisabled(downloadButton) &&
                isDisabled(newdropdownButton) &&
                !isDisabled(searchFilter) &&
                !isDisabled(leveUpButton) &&
                !isDisabled(rootButton)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean isSubFolder(String folderName) throws Exception {

        // Verify folder name
        if (!(getHeaderText().contains(folderName)) ){
                return false;
        }

        // Check number of elements in button bar
        if (navbarButtonList.findElements(By.xpath("//*[@id=\"navbar-functions\"]/ul")).size() == 6 ) {
            return true;
        }
        // Check state of buttons
        if (leveUpButton.isDisplayed() &&
                deleteButton.isDisplayed() &&
                rootButton.isDisplayed() &&
                newdropdownButton.isDisplayed() &&
                moredetailsButton.isDisplayed()) {
            return true;
        }
        else {
            return false;
        }
    }

    public boolean isRootFolder() throws Exception {

        // Verify folder name
        if (!(getHeaderText().contains(ROOT_FOLDER_NAME)) ){
            return false;
        }
        // navigation buttons are NOT displayed in root
        // folder. This will change as functionality is added
        // Currently the navbar only has one child, and it's ID is

        if (navbarButtonList.findElements(By.xpath("//*[@id=\"navbar-functions\"]/ul")).size() == 1) {
            return true;
        }
        return false;
    }

    public boolean isFileSelectedMode(int rowNumber) {
        // Class of selected row is different:
        // visually it will be different, but for now the change
        // in css class is enough to check
        //*[@id="beacon"]/tbody/tr[1]
        WebElement selectedRow = beaconTable.findElement(
                By.xpath("//*[@id=\"beacon\"]/tbody/tr["+rowNumber+"]"));

        if (!selectedRow.getAttribute("class").contains("selected")) {
            return false;
        }

        // Behaviour is different if person is logged in or not
        if (isLoggedIn()) {
            if ( ! (isDisabled(deleteButton) && isDisabled(downloadButton)) ) {
                return true;
            }
        } else {
            // There will need to be a check for publicly available for download or not?
            if (isDisabled(deleteButton) && !isDisabled(downloadButton)) {
                return true;
            }
        }

        return false;
    }


    public boolean isDefaultSort() {
        // Name column asc is default sort when page loads
        WebElement nameColHeader = beaconTable.findElement(
                By.xpath("//*[@id=\"beacon_wrapper\"]/div[2]/div/div[1]/div[1]/div/table/thead/tr/th[2]")
        );

        if (nameColHeader.getAttribute("class").equals("sorting_asc")) {
            return true;
        }

        return false;
    }


}