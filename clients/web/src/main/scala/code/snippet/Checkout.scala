package code
package snippet

import code.lib.{UserClient, CartViewer}
import net.liftweb.common.Full
import net.liftweb.http.js.JsCmds._
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.{RequestVar, S, SHtml}

import scala.xml.NodeSeq

import com.kitchenfantasy.model._
import net.liftweb.util.Helpers.strToCssBindPromoter
import java.util.Calendar

class Checkout extends CartViewer {
  private lazy val pageUrl = "/checkout"

  private object checkoutConfirmation extends RequestVar[(Boolean)](false)

  private def renderNotice(msg: String) = <div class='register-req'><p>{msg}</p></div>
  private def renderError(msg: String) = <label>{msg}</label>

  lazy val currentYear = Calendar.getInstance().get(Calendar.YEAR)

  private lazy val cc_types: List[String] = List("-- CC Type --", "Visa", "Amex", "Mastercard", "Discover")
  private lazy val states: Map[String, String] = Map("-- State --" -> "", "Alabama" -> "AL", "Alaska" -> "AK",
    "Arizona" -> "AZ", "Arkansas" -> "AK", "California" -> "CA", "Colorado" -> "CO", "Connecticut" -> "CT",
    "Delaware" -> "DE", "District of Columbia" -> "DC", "Florida" -> "FL", "Georgia" -> "GA",
    "Hawaii" -> "HI", "Idaho" -> "ID", "Illinois" -> "IL", "Indiana" -> "IN", "Iowa" -> "IA",
    "Kansas" -> "KS", "Kentucky" -> "KY", "Louisiana" -> "LA", "Maine" -> "MA", "Maryland" -> "MD",
    "Massachusetts" -> "MA", "Michigan" -> "MI", "Minnesota" -> "MN", "Mississippi" -> "MS",
    "Missouri" -> "MO", "Montana" -> "MT", "Nebraska" -> "NE", "Nevada" -> "NV", "New Hampshire" -> "NH",
    "New Jersey" -> "NJ", "New Mexico" -> "NM", "New York" -> "NY", "North Carolina" -> "NC",
    "North Dakota" -> "ND", "Ohio" -> "OH", "Oklahoma" -> "OK", "Oregon" -> "OR", "Pennsylvania" -> "PA",
    "Rhode Island" -> "RI", "South Carolina" -> "SC", "South Dakota" -> "SD", "Tennessee" -> "TN",
    "Texas" -> "TX", "Utah" -> "UT", "Vermont" -> "VT", "Virginia" -> "VA", "Washington" -> "WA",
    "West Virginia" -> "WV", "Wisconsin" -> "WI", "Wyoming" -> "WY")

  private lazy val expiry_months: Map[String, Int] = Map("-- Expiration Month --" -> 0, "January" -> 1,
    "February" -> 2, "March" -> 3, "April" -> 4, "May" -> 5, "June" -> 6, "July" -> 7, "August" -> 8,
    "September" -> 9, "October" -> 10, "November" -> 11, "December" -> 12)

  private lazy val expiry_years: Map[String, Int] =  Map("-- Expiration Year --" -> 0) ++
    ((currentYear to (currentYear+10)).map(i => (i.toString, i))).toMap

  private val user_cc_info = UserClient.getUserBillingInfo
  private val user_address = UserClient.getUserAddress

  private var last_name = user_cc_info.last_name
  private var first_name = user_cc_info.first_name
  private var cc_type = cc_types.find(_.toLowerCase == user_cc_info.cc_type) match {
    case Some(cc) => cc
    case _=> "-- CC Type --"
  }
  private var cc_no = user_cc_info.cc_number

  private var cc_expiry_month = expiry_months.find(_._2 == user_cc_info.cc_expiry_month) match {
    case Some(m) => m._1
    case _ => "-- Expiration Month --"
  }

  private var cc_expiry_year = expiry_years.find(_._2 == user_cc_info.cc_expiry_year) match {
    case Some(y) => y._1
    case _ => "-- Expiration Year --"
  }

  private var address1 = user_address.line1
  private var address2 = user_address.line2
  private var city = user_address.city
  private var zip = user_address.postalCode
  private var state = states.find(_._2 == user_address.state) match {
    case Some(s) => s._1
    case _ => "-- State --"
  }

  private var notes = user_address.notes.getOrElse("")

  def showCheckout = !checkoutConfirmation.get

  def showConfirmation = checkoutConfirmation.get

  def checkout (in: NodeSeq) : NodeSeq = {
    def processCheckout: JsCmd = {
      val a = Address(address1, address2, city,
        if (state.isEmpty) "" else states(state), zip, Some("USA"), Some(notes))
      val ccInfo = CCInfo(cc_type.toLowerCase, cc_no,
        (if (cc_expiry_month.isEmpty) 0 else expiry_months(cc_expiry_month)),
        (if (cc_expiry_year.isEmpty) 0 else expiry_years(cc_expiry_year)), first_name, last_name)
      val errorList = AddressValidator.validateAddress (a) ++ CCValidator.validateCC (ccInfo)

      if (!errorList.filter(!_._2.isEmpty).isEmpty) {
        errorList.foreach {
          case (error_id, error_value) => {
            S.error (error_id + "_err", renderError(error_value))
          }
          case _ => Noop }
      }
      else
        UserClient.updateUserInfo(a, ccInfo) match {
          case Some (u) =>
            S.redirectTo(pageUrl, () => {
              S.notice(renderNotice("Updated user information."))
              checkoutConfirmation(true) })
          case _ => {
            S.notice("Error updating account info.")
            Noop
          }
        }
      JsCmds.Noop
    }

    if (showCheckout) {
      val cssSel = "#last_name" #> SHtml.text(last_name, last_name = _) &
        "#first_name" #> SHtml.text(first_name, first_name = _) &
        "#cc_type" #> SHtml.select(cc_types.map(t => ((if (t.toString == "-- CC Type --") ""
        else
          t.toString) -> t.toString)), Full(cc_type), cc_type = _) &
        "#cc_no" #> SHtml.text(cc_no, cc_no = _) &
        "#cc_expiry_month" #> SHtml.select(expiry_months.toSeq.sortBy(_._2).map(m => (m._1 -> m._1)),
          Full(cc_expiry_month), cc_expiry_month = _) &
        "#cc_expiry_year" #> SHtml.select(expiry_years.toSeq.sortBy(_._2).map(y => (y._1 -> y._1)),
          Full(cc_expiry_year), cc_expiry_year = _) &
        "#address1" #> SHtml.text(address1, address1 = _) &
        "#address2" #> SHtml.text(address2, address2 = _) &
        "#city" #> SHtml.text(city, city = _) &
        "#zip_code" #> SHtml.text(zip, zip = _) &
        "#notes" #> SHtml.textarea(notes, notes = _) &
        "#states" #> SHtml.select(states.toSeq.sortBy(_._2).map(s => (s._1 -> s._1)), Full(state), state = _) &
        "#process_checkout" #> (SHtml.hidden(() => processCheckout))
      cssSel (in)
    }
    else NodeSeq.Empty
  }

  def confirm (in: NodeSeq): NodeSeq = {
    if (showConfirmation)
      renderCart(in)
    else NodeSeq.Empty
  }
}