<div class="row">

  <!-- Main content -->
  <div role="main" class="col-sm-12 main">
    <h1 class="hidden" property="name">${folder.path}</h1>
    <section>
      <h2 property="name">
      <#if isRoot>
        ROOT
      <#else>
        <button id="more_details" name="more_details" class="btn btn-info btn-xs"
                data-placement="bottom" data-toggle="popover"
                role="button">
          <span class="glyphicon glyphicon-eye-open"></span>&nbsp;<span class="caret"></span></button>
        ${folder.path}
      </#if>
      </h2>
    </section>

    <#flush>

    <section id="main_section">
      <div id="fileinfo" class="table-responsive">
        <nav class="navbar navbar-default">
          <div class="container-fluid">
            <div class="collapse navbar-collapse" id="navbar-functions">
              <ul class="nav navbar-nav">
                  <li class="dataTables_filter">
                      <form class="navbar-form navbar-left" role="search">
                          <input id="beacon_filter" class="form-control dataTables_filter"
                                 aria-controls="beacon" placeholder="Search Name..." type="search" />
                      </form>
                  </li>
                <li class="dropdown divider-vertical">
                  <a title="Select VOSpace Service" class="dropdown-toggle" role="button" id="svcdropdown" name="svcdropdown"
                     aria-expanded="false" data-toggle="dropdown">
                    <span class="glyphicon glyphicon-hdd"></span> ${vospaceSvcName} <span class="caret"></span></a>
                  <ul class="dropdown-menu">
                      <#list vospaceServices as vospaceSvc>
                        <li>
                          <a id="vos_${vospaceSvc}" href="${contextPath}${vospaceSvc}/list" role="button">
                              ${vospaceSvc}</a>
                        </li>
                      </#list>
                  </ul>
                </li>
<#if homeDir??>
  <!-- homeDir is populated if the home directory for that user actually exists -->
  <#assign homeURL = '${contextPath}${vospaceSvcPath}list${homeDir}'>

                  <li>
                      <a id="homeDir" name="homeDir" type="button" title="Navigate to home directory." href="${homeURL}">
                          <span class="glyphicon glyphicon-home"></span>&nbsp;Home</a></li>
</#if>
<#if !isRoot>
                <li>
                  <a id="level-up" name="level-up" href="${contextPath}${vospaceSvcPath}list${folder.parentPath}" role="button" title="Up one level">
                    <span class="glyphicon glyphicon-level-up"></span>&nbsp;Up</a></li>
                <li>
                  <a id="root" name="root" type="button" title="Navigate to main root." href="${contextPath}${vospaceSvcPath}list/">
                    <span class="glyphicon glyphicon-folder-close"></span>&nbsp;Root</a></li>

                <li class="dropdown divider-vertical<#if !folderWritable> disabled</#if>">
                  <a title="Add" class="dropdown-toggle<#if !folderWritable> disabled</#if>" <#if !folderWritable>disabled="disabled"</#if>role="button" id="newdropdown" name="newdropdown" aria-expanded="false" data-toggle="dropdown">
                    <span class="glyphicon glyphicon-plus"></span>&nbsp;Add&nbsp;<span class="caret"></span></a>
                  <ul class="dropdown-menu">

                    <li>
                      <a id="newfolder" name="newfolder">
                        <span class="glyphicon glyphicon-folder-open"></span>&nbsp;Folder</a></li>
                    <li>
                      <a id="upload">
                        <span class="glyphicon glyphicon-cloud-upload"></span>&nbsp;Upload File</a>
                      <form id="uploader" method="post" class="hidden">
                        <input id="currentpath" name="currentpath" type="hidden" value="${folder.path}"/>
                        <input id="currentFolderName" name="currentFolderName" type="hidden" value="${folder.name}"/>
                      </form>
                    </li>
<#if features.batchUpload>
                    <li>
                      <a id="uploadfolder">
                        <span class="glyphicon glyphicon-cloud-upload"></span>&nbsp;Upload Folder</a></li>
</#if>
                    <li>
                      <a id="new_vospace_link"><span class="glyphicon glyphicon-link"></span>&nbsp;VOSpace Link</a></li>
<#if features.externalLinks>
                    <li>
                      <a id="new_external_link"><span class="glyphicon glyphicon-link"></span>&nbsp;External Link</a></li>
</#if>
                  </ul>
                </li>
<#if features.batchDownload>
                <li class="disabled dropdown multi-select-function-container">
                  <a disabled="disabled" class="disabled dropdown-toggle multi-select-function" id="download" role="button" name="download" type="button" aria-expanded="false" data-toggle="dropdown" title="Download selected items.">
                    <span class="glyphicon glyphicon-cloud-download"></span>&nbsp;Download&nbsp;<span class="caret"></span></a>
                  <ul class="dropdown-menu download-dropdown-menu">
                    <li><a class="download-url-list"><span class="glyphicon glyphicon-list"></span>&nbsp;URL List</a></li>
                    <li><a class="download-shell-script"><span class="glyphicon glyphicon-list"></span>&nbsp;Shell Script</a></li>
                    <li><a class="download-html-list"><span class="glyphicon glyphicon-list-alt"></span>&nbsp;HTML List</a></li>
                    <li><a class="download-download-manager"><span class="glyphicon glyphicon-download-alt"></span>&nbsp;Download Manager</a></li>
                  </ul>
                </li>
</#if>
</#if>
<#if !isRoot>
                <#-- Disabled by default -->
                <li class="disabled multi-select-function-container-writable">
                  <a disabled="disabled" class="disabled multi-select-function-writable" id="move" name="move" role="button" title="Move selected items">
                    <span class="glyphicon glyphicon-move"></span>&nbsp;Move</a></li>
                <#-- Disabled by default -->
                <li class="disabled multi-select-function-container-writable">
                  <a disabled="disabled" class="disabled multi-select-function-writable" id="delete" name="delete" role="button" title="Delete selected items">
                    <span class="glyphicon glyphicon-trash"></span>&nbsp;Delete</a></li>
</#if>
              </ul>
            </div>
          </div>
        </nav>
        <!-- The width style here MUST exist in this tag, rather than in the CSS file. -->
        <table id="beacon" class="table table-striped table-condensed table-hover" style="width: 100%;">
          <thead>
            <tr>
              <th></th>
              <th>Name</th>
              <th>Size</th>
              <th>Last Modified (UTC)</th>
              <th>Read/Write</th>
              <th>Read</th>
              <th>Owner</th>
            </tr>
          </thead>
          <tbody>
          </tbody>
        </table>
      </div>
    </section>
  </div>
</div>
