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

  lazy val currentYear = Calendar.getInstance().get(Calendar.YEAR)

  private val cc_types: List[String] = List("-- CC Type --", "Visa", "Amex", "Mastercard", "Discover")
  private val states = List("-- State --", "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut",
    "Delaware", "District of Columbia", "Florida", "Georgia", "Hawaii", "Idaho", "Illinois", "Indiana", "Iowa",
    "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi",
    "Missouri", "Montana", "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York",
    "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", "Pennsylvania", "Rhode Island", "South Carolina",
    "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", "Washington", "West Virginia", "Wisconsin",
    "Wyoming")

  private val expiry_months: List[String] = List("-- Expiration Month --", "January", "February", "March", "April",
                "May", "June", "July", "August", "September", "October", "November", "December")
  private val expiry_years: List[String] =  "-- Expiration Year --" :: ((currentYear to (currentYear+10)).map(i => i.toString)).toList

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
      JsCmds.Noop
    }

    "#last_name" #> SHtml.text(last_name, last_name = _) &
      "#first_name" #> SHtml.text(first_name, first_name = _) &
      "#cc_type" #> SHtml.select(cc_types.map(t => ((if (t.toString == "-- CC Type --") "" else
        t.toString) -> t.toString)), Empty, cc_type = _) &
      "#cc_no" #> SHtml.text(cc_no, cc_no = _) &
      "#cc_expiry_month" #> SHtml.select(expiry_months.map(t => ((if (t.toString == "-- Expiration Month --") "" else
      t.toString) -> t.toString)), Empty, cc_expiry_month = _) &
      "#cc_expiry_year" #> SHtml.select(expiry_years.map(t => ((if (t.toString == "-- Expiration Year --") "" else
        t.toString) -> t.toString)), Empty, cc_expiry_year = _) &
      "#address1" #> SHtml.text(address1, address1 = _) &
      "#address2" #> SHtml.text(address2, address2 = _) &
      "#city" #> SHtml.text(city, city = _) &
      "#zip_code" #> SHtml.text(zip, zip = _) &
      "#notes" #> SHtml.textarea(notes, notes = _) &
      "#states" #> SHtml.select(states.map(s => ((if (s.toString == "-- State --") "" else
        s.toString) -> s.toString)), Empty, state = _)
  }
}