<nav class="navbar navbar-fixed-top navbar-branding">
  <div class="container">
    <div class="navbar-header">
      <button type="button" class="navbar-toggle collapsed" data-toggle="collapse" data-target="#navbar" aria-expanded="false" aria-controls="navbar">
        <span class="sr-only">Toggle navigation</span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
        <span class="icon-bar"></span>
      </button>
      <span class="navbar-brand">OpenCADC User Storage</span>
    </div>
    <div id="navbar" class="navbar-collapse collapse">
      <ul class="nav navbar-nav navbar-right">
        <li>
          <#setting url_escaping_charset='ISO-8859-1'>
          <#if username??>
          <span class="display-name">${username}</span>
          <#else>
          <a class="btn btn-primary" href="${contextPath}oidc-login">Sign In to OpenID Connect</a>
          </#if>            
        </li>
      </ul>
    </div><!--/.nav-collapse -->
  </div>
</nav>
