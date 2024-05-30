<#assign redirectPath = "">
<#if folder??>
  <#assign redirectPath = folder.path>
</#if>

<#if requestedFolder??>
  <#assign redirectPath = requestedFolder>
</#if>

<#if username??>
<a title="User actions." class="dropdown-toggle access-actions user-actions" role="button" aria-haspopup="true" aria-expanded="false" data-toggle="dropdown">
${username} <span class="caret"></span></a>
<ul class="dropdown-menu list-unstyled">
  <li class="disabled" disabled="disabled">
    <a title="Update profile." href="#" class="disabled" disabled="disabled">My profile</a></li>
  <li class="disabled" disabled="disabled">
    <a title="Change password." href="#" class="disabled" disabled="disabled">Change password</a></li>
  <li>
    <a id="logout" title="Logout."><span class="glyphicon glyphicon-log-out"></span> Logout</a></li>
</ul>
<#else>
<a title="Login form" class="dropdown-toggle access-actions login-form" role="button" aria-haspopup="true" aria-expanded="false" data-toggle="dropdown">Login <span class="caret"></span></a>
<ul class="dropdown-menu list-unstyled pull-right login-container">
  <li>
    <form class="form-inline" id="loginForm" role="form" method="post"
          action="${contextPath}ac/authenticate">
      <span id="login_fail" class="help-block text-danger pull-left"></span>
      <div class="form-group">
        <label for="username" class="hidden" id="usernameLabel">Username</label>
        <input type="text" id="username" name="username" class="form-control" tabindex="1" required="required" placeholder="Username" />
      </div>
      <div class="form-group">
        <label for="password" class="hidden" id="passwordLabel">Password</label>
        <input type="password" id="password" name="password" class="form-control" tabindex="2" required="required" placeholder="Password" />
      </div>
      <input type="hidden" id="redirectPath" name="redirectPath" value="${redirectPath}" />
      <button type="submit" id="submitLogin" class="btn btn-success"><span class="glyphicon glyphicon-log-in"></span> Login</button>
    </form>
  </li>
</ul>
</#if>
