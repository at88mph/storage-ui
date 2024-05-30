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
  <link rel="stylesheet" href="${canfarWebRoot}/css/canfar.css" media="screen"/>
  <link rel="stylesheet" href="${canfarWebRoot}/css/github-dark.css" media="screen"/>
  <link rel="stylesheet" href="${canfarWebRoot}/css/print.css" media="print"/>

  <!-- jquery ui CSS -->
  <link href="${contextPath}css/jquery-ui.min.css" rel="stylesheet" media="screen">

  <!-- Bootstrap core CSS -->
  <link href="${contextPath}css/bootstrap.min.css" rel="stylesheet" media="screen">
  <link href="${contextPath}css/bootstrap-theme.min.css" rel="stylesheet" media="screen">

  <link rel="stylesheet" href="${contextPath}css/datatables.min.css" media="screen"/>
  <link rel="stylesheet" href="${contextPath}css/storage.css" media="screen"/>
  <link rel="stylesheet" href="${contextPath}css/error.css" media="screen"/>

  <style type="text/css">
    .navbar-header
    {
      margin-top: 0;
    }
  </style>
</head>

<body>

<!-- jQuery needs to be loaded before the application header.  I don't know why.
jenkinsd 2017.04.04
-->
<script src="${contextPath}js/jquery.min.js"></script>

<#include "canfar-application-header">

<div class="container-fluid">
    <div id="errorDisplayDiv" class="errorDisplay" >
        Unable to fulfil request: ${errorMessage}
        <div class="returnTo">
            <br />
            Return to <a title="Root Folder" href='${contextPath}${vospaceSvcPath}list'>User Storage Root folder</a>
        </div>
    </div>
</div>


<!-- Bootstrap core JavaScript
================================================== -->
<!-- Placed at the end of the document so the pages load faster -->
<script src="${contextPath}js/jquery.min.js"></script>
<!-- IE10 viewport hack for Surface/desktop Windows 8 bug -->
<#--<script src="${contextPath}js/ie10-viewport-bug-workaround.js"></script>-->

<script type="text/javascript" src="${contextPath}js/org.opencadc.js"></script>

<!--
 AWAYS ensure the bootstrap.min.js comes last!
 The popover will not work otherwise.

 jenkinsd 2016.06.24
-->
<script src="${contextPath}js/bootstrap.min.js"></script>

<script type="text/javascript">

    $(document).ready(function ()
    {
        $(document).on("click", "a#logout", function ()
        {
            $.ajax({
                url: '${contextPath}ac/authenticate',
                method: 'DELETE'
            })
                    .done(function ()
                    {
                        window.location.reload(true);
                    });
        });
    });
</script>
</body>
</html>
