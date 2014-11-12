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
import net.liftweb.http.js.JsCmds.SetHtml
import net.liftweb.util.Helpers.strToCssBindPromoter
import net.liftweb.http.S
import code.lib.ProductClient
import code.lib.ApiClient
import net.liftweb.http.RequestVar
import net.liftweb.util.CssSel

class ViewProducts {
  private def noProductsErrMsg = "There are no products."
  private def noProductsMsg = "Sorry, we were unable to find any products at this time."

  private def noProductsInCart = "There are no items in your cart."

  private def formatPrice (price: Long) = {
    if (price >= 100)
      ("$ " + (price.toDouble * 0.01).toString.substring(0,price.toString.length + 1))
    else
      ("$ 0." + price.toString)
  }

  private def addProductToCart (p: Product): JsCmd = {
    ProductClient.addProductToCart(p)
    JsCmds.RedirectTo("/cart")
  }

  private def showCartItem (p: Product): CssSel =
    "#cart_image [src]" #> p.imageUrl &
      "#cart_item_name *" #>  p.name &
      "#cart_item_price *" #> formatPrice (p.price) &
      "#cart_item_desc *" #> p.description &
      ".cart_quantity_input [value]" #> p.qty.getOrElse(1) &
      ".cart_quantity_delete [onclick]" #> SHtml.ajaxInvoke(() => {
        ProductClient.deleteProductFromCart(p)
        SetHtml("cart_item", shoppingCartTemplate.is.applyAgain)
      })

  private def showProductItem (p: Product) =
    "#productImage [src]" #> p.imageUrl &
      "#productPrice *" #>  formatPrice (p.price) &
      "#productDesc *" #> p.description &
      "#addToCart [onclick]" #> SHtml.ajaxInvoke (() => addProductToCart(p))

  private def renderCart = ApiClient.myCart.get match {
    case Full(productList) =>
      "#cart_row *" #> productList.sortBy(i => (i.name)).map { p => showCartItem(p) }
    case _ => "#cart_row *" #> noProductsInCart
  }

  private def renderShoppingCart = SHtml.memoize { renderCart }

  object shoppingCartTemplate extends RequestVar(renderShoppingCart)

  def viewCart (in: NodeSeq): NodeSeq = renderCart(in)

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