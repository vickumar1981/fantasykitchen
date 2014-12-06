package code
package snippet

import code.lib.{CartViewer, ProductClient}
import net.liftweb.http.SHtml

import scala.xml.NodeSeq

import net.liftweb.util.Helpers.strToCssBindPromoter
import com.kitchenfantasy.model.{Product, OrderValidator}

class ViewProducts extends CartViewer {
  private def noProductsErrMsg = <b>There are no products.</b>
  private def noProductsMsg = <b>Sorry, we were unable to find any products at this time.</b>

  def viewCart (in: NodeSeq): NodeSeq = renderCart(in)

  private def showProductItem (p: Product) =
    "#productImage [src]" #> p.imageUrl &
      "#productPrice *" #>  OrderValidator.formatPrice (p.price) &
      "#productDesc *" #> p.description &
      "#addToCart [onclick]" #> SHtml.ajaxInvoke (() => addProductToCart(p))

  def viewProducts (in: NodeSeq): NodeSeq = {
    val cssSel = ProductClient.viewProducts match {
      case Some(productList) => {
        if (productList.rows == 0)
          "#productItem" #> noProductsMsg
        else
          "#productItem *" #> productList.data.map { p => showProductItem(p) }
      }
      case _ => "#productItem" #> noProductsErrMsg
    }
    cssSel(in)
  }
}