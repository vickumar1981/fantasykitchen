package code
package snippet

import scala.xml.NodeSeq
import scala.xml.NodeSeq.seqToNodeSeq
import scala.xml.Unparsed
import com.kitchenfantasy.model._
import net.liftweb.common.Box.box2Option
import net.liftweb.common.Empty
import net.liftweb.common.Full
import net.liftweb.http.SHtml
import net.liftweb.http.js.JsCmd
import net.liftweb.http.js.JsCmds
import net.liftweb.util.Helpers.strToCssBindPromoter
import net.liftweb.http.S
import code.lib.ProductClient
import code.lib.ApiClient

class ViewProducts {
  private def noProductsErrMsg = "There are no products."
  private def noProductsMsg = "Sorry, we were unable to find any products at this time."

  private def formatPrice (price: Long) = {
    if (price >= 100)
      ("$ " + (price.toDouble * 0.01).toString.substring(0,price.toString.length + 1))
    else
      ("$ 0." + price.toString)
  }

  private def showProductItem (p: Product) = {
    ("#productImage [src]") #> (p.imageUrl) &
      ("#productPrice *") #>  (formatPrice (p.price)) &
      ("#productDesc *") #> (p.description)
  }

  def viewProducts (in: NodeSeq): NodeSeq = {
    val cssSel = ProductClient.viewProducts match {
      case Some(productList) => {
        if (productList.rows == 0)
          "#productItem" #> noProductsMsg
        else
          "#productItem *" #> productList.data.map { p =>
            showProductItem(p)
          }
      }
      case _ => "#productItem" #> noProductsErrMsg
    }
    cssSel(in)
  }
}