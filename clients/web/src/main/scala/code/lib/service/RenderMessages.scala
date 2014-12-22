package code.lib.service

trait RenderMessages {
  def renderNotice(msg: String) = <div class='register-req'><p>{msg}</p></div>
  def renderError(msg: String) = <label>{msg}</label>
  def renderValidation(msg: String) = (if (msg.length > 0)
    <span class="blank-arrow"><label>{msg}</label></span>
  else <span></span>)
}
