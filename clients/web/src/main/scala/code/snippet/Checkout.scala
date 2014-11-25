package code
package snippet

import code.lib.{UserClient, ApiClient, ProductClient}
import net.liftweb.common.{Empty, Full}
import net.liftweb.http.{RequestVar, SHtml, TransientRequestVar}
import net.liftweb.http.js.{JsCmd, JsCmds}
import net.liftweb.http.js.JsCmds._
import net.liftweb.util.CssSel

import net.liftweb.http.S
import scala.xml.NodeSeq

import com.kitchenfantasy.model._
import net.liftweb.util.Helpers.strToCssBindPromoter

import java.util.Calendar

class Checkout {

  private def renderNotice(msg: String) = <div class='register-req'><p>{msg}</p></div>
  private def renderError(msg: String) = <label>{msg}</label>

  lazy val currentYear = Calendar.getInstance().get(Calendar.YEAR)

  private val cc_types: List[String] = List("-- CC Type --", "Visa", "Amex", "Mastercard", "Discover")
  private val states: Map[String, String] = Map("-- State --" -> "", "Alabama" -> "AL", "Alaska" -> "AK",
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

  private val expiry_months: Map[String, Int] = Map("-- Expiration Month --" -> 0, "January" -> 1,
    "February" -> 2, "March" -> 3, "April" -> 4, "May" -> 5, "June" -> 6, "July" -> 7, "August" -> 8,
    "September" -> 9, "October" -> 10, "November" -> 11, "December" -> 12)

  private val expiry_years: Map[String, Int] =  Map("-- Expiration Year --" -> 0) ++ ((currentYear to (currentYear+10)).map(i => (i.toString, i))).toMap

  private var last_name = ""
  private var first_name = ""
  private var cc_type = ""
  private var cc_no = ""
  private var cc_expiry_month = ""
  private var cc_expiry_year = ""

  private var address1 = ""
  private var address2 = ""
  private var city = ""
  private var zip = ""
  private var state = ""
  private var notes = ""

  def checkout = {

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
            S.notice("updated info...")
          case _ => {
            S.notice("Error updating account info.")
            Noop
          }
        }
      JsCmds.Noop
    }

    "#last_name" #> SHtml.text(last_name, last_name = _) &
      "#first_name" #> SHtml.text(first_name, first_name = _) &
      "#cc_type" #> SHtml.select(cc_types.map(t => ((if (t.toString == "-- CC Type --") "" else
        t.toString) -> t.toString)), Empty, cc_type = _) &
      "#cc_no" #> SHtml.text(cc_no, cc_no = _) &
      "#cc_expiry_month" #> SHtml.select(expiry_months.toSeq.sortBy(_._2).map (m=>(m._1 -> m._1)),
        Empty, cc_expiry_month = _) &
      "#cc_expiry_year" #> SHtml.select(expiry_years.toSeq.sortBy(_._2).map(y=>(y._1 -> y._1)),
        Empty, cc_expiry_year = _) &
      "#address1" #> SHtml.text(address1, address1 = _) &
      "#address2" #> SHtml.text(address2, address2 = _) &
      "#city" #> SHtml.text(city, city = _) &
      "#zip_code" #> SHtml.text(zip, zip = _) &
      "#notes" #> SHtml.textarea(notes, notes = _) &
      "#states" #> SHtml.select(states.toSeq.sortBy(_._2).map(s => (s._1 -> s._1)), Empty, state = _) &
      "#process_checkout" #> (SHtml.hidden(() => processCheckout))
  }
}