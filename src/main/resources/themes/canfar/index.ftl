<!DOCTYPE html>

<#-- Set this appropriately -->
<#assign canfarWebRoot = "https://www.canfar.net">

<html lang="en">
<head>
  <meta charset="utf-8">
  <meta http-equiv="X-UA-Compatible" content="IE=edge">
  <meta name="viewport" content="width=device-width, initial-scale=1">

  <!-- The above 3 meta tags *must* come first in the head; any other head content must come *after* these tags -->
  <meta name="description" content="User storage system.">
  <meta name="author" content="Canadian Astronomy Data Centre">
  <meta name="keywords"
        content="VOSpace, IVOA, CADC, Canadian Astronomy Data Centre">

  <title>User Storage</title>

  <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
  <!--[if lt IE 9]>
  <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
  <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
  <![endif]-->

  <!-- Custom CSS -->
  <link rel="stylesheet" href="${canfarWebRoot}/css/github-dark.css" media="screen"/>
  <link rel="stylesheet" href="${canfarWebRoot}/css/print.css" media="print"/>
  <link rel="stylesheet" href="${canfarWebRoot}/css/canfar.css?version=3" media="screen"/>

  <!-- jquery ui CSS -->
  <link href="${contextPath}css/jquery-ui.min.css" rel="stylesheet" media="screen">

  <!-- Bootstrap core CSS -->
  <link href="${contextPath}css/bootstrap.min.css" rel="stylesheet" media="screen">
  <link href="${contextPath}css/bootstrap-theme.min.css" rel="stylesheet" media="screen">

  <link rel="stylesheet" href="${contextPath}css/datatables.min.css" media="screen"/>
  <link rel="stylesheet" href="${contextPath}css/storage.css?version=4" media="screen"/>
  <link rel="icon" type="image/x-icon"  href="${canfarWebRoot}/favicon.ico?v=2" />

  <style type="text/css">
    .navbar-header
    {
      margin-top: 0;
    }
  </style>
</head>

<body>

<#assign startTime = .now?time>
<#assign isRoot = folder.root>

<!-- was username. homeDir is populated if the home
directory for that user actually exists -->
<#if homeDir??>
  <#assign homeURL = '${contextPath}list/${username}'>
</#if>

<!-- jQuery needs to be loaded before the application header.  I don't know why.
jenkinsd 2017.04.04
-->
<script type="application/javascript" src="${contextPath}js/jquery.min.js"></script>

<#include "canfar-application-header">

<div class="container-fluid">

<#-- Obtained from root library. -->
<#include "_main.ftl">

</div><!-- /.container -->


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<#--<script src="${contextPath}js/ie10-viewport-bug-workaround.js"></script>-->

<script type="text/javascript"
        src="${contextPath}js/jquery.csv-0.71.min.js"></script>
<script type="text/javascript" src="${contextPath}js/datatables.min.js"></script>
<script type="text/javascript" src="${contextPath}js/file-size.js"></script>
<script type="text/javascript" src="${contextPath}js/jquery-browser.js"></script>
<script type="text/javascript" src="${contextPath}js/jquery.form-3.24.js"></script>
<script type="text/javascript"
        src="${contextPath}js/jquery.contextmenu/jquery.contextMenu-1.01.js"></script>
<script type="text/javascript"
        src="${contextPath}js/jquery.tablesorter-2.7.2.min.js"></script>
<script type="text/javascript"
        src="${contextPath}js/jquery-impromptu.min.js"></script>
<script type="text/javascript" src="${contextPath}js/jquery-ui.min.js"></script>
<script type="text/javascript" src="${contextPath}js/CollapsibleLists.compressed.js"></script>
<script type="text/javascript" src="${contextPath}js/filemanager.js"></script>


<!--
 AWAYS ensure the bootstrap.min.js comes last!
 The popover will not work otherwise.

 jenkinsd 2016.06.24
-->
<script src="${contextPath}js/bootstrap.min.js"></script>

<script type="text/javascript">

  $(document).ready(function ()
                    {
                    <#-- Intercept the JavaScript here for the Folder Details button. -->
                    <#if !isRoot>
                      // Activate the Details button.
                      $('[data-toggle="popover"]').popover(
                          {
                            html: true,
                            title: "<strong>${folder.name}</strong>",
                            content: function ()
                            {
                              return '<table class="table table-condensed table-bordered">'
                                     +
                                     '<tbody><tr><td>Owned by</td><td class="info"><strong>${folder.ownerCN}</strong></td></tr>'
                                     +
                                     '<tr><td>Last used</td><td class="info">${folder.lastModifiedHumanReadable}</td></tr>'
                                     +
                                     '<tr><td colspan="2">Is <#if !folderWritable><span class="text-danger">not </span></#if>writable by you.</td></tr>'
                                     + '</tbody></table>';
                            }
                          });
                    </#if>

                      // Override CSS for search filter field.
                      $.fn.DataTable.ext.oStdClasses.sFilter =
                          "dataTables_filter";

                      // For quick pre-load.
                      const rows = [];
                      <#list initialRows as row>
                        <#if row != "">
                          rows.push([${row}])
                        </#if>
                      </#list>

                      $.getJSON('${contextPath}scripts/languages/' +
                                $("html").attr("lang") + '.json')
                          .done(function (json)
                                {
                                  // Row count is 100 by default to just show
                                  // a moving barber pole progress.
                                  fileManager(rows, $("#beacon"), "<#if startURI??>${startURI}</#if>", "${folder.path}",
                                              ${folderWritable?c}, 100 , json, "${contextPath}", false,
                                              "${vospaceSvcPath}", "${vospaceNodePrefixURI}");
                                })
                          .fail(function (request, textStatus, errorThrown)
                                {
                                  console.log("Error (" + request.status + "): "
                                              + textStatus + "\n"
                                              + errorThrown);
                                });

                      $('<div class="early-access text-warning">Early access with limited functionality</div>')
                          .after("#top-nav > div.container > div.navbar-header");
                    });
</script>
</body>
</html>
