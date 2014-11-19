package bootstrap.liftweb

import code.lib.{UserClient, ApiClient}
import net.liftweb._
import net.liftweb.http.provider.HTTPCookie
import util._
import Helpers._

import common._
import http._
import js.jquery.JQueryArtifacts
import sitemap._
import Loc._
import mapper._

import com.kitchenfantasy.model.UserCredential

import code.model._
import net.liftmodules.JQueryModule


/**
 * A class that's instantiated early and run.  It allows the application
 * to modify lift's environment
 */
class Boot {
  def autoLoginUser(in: List[HTTPCookie]) = {
    val cookies = in.filter { c => (c.name.equals(UserClient.userCookieName))}.map { c => c.value}
    if (cookies.size == 1) {
      val (email, pw) = UserClient.getUserCookie(cookies(0).getOrElse(""))
      UserClient.loginUser(UserCredential(email, pw))
    }
  }

  def boot {
    if (!DB.jndiJdbcConnAvailable_?) {
      val vendor = new StandardDBVendor(Props.get("db.driver") openOr "org.h2.Driver",
			     Props.get("db.url") openOr 
			     "jdbc:h2:lift_proto.db;AUTO_SERVER=TRUE",
			     Props.get("db.user"), Props.get("db.password"))
      LiftRules.unloadHooks.append(vendor.closeAllConnections_! _)
      DB.defineConnectionManager(util.DefaultConnectionIdentifier, vendor)
    }

    LiftRules.earlyInStateful.append {
      case Full(r) if (!ApiClient.isLoggedIn()) => autoLoginUser(r.cookies)
      case _ =>
    }

    // Use Lift's Mapper ORM to populate the database
    // you don't need to use Mapper to use Lift... use
    // any ORM you want
    Schemifier.schemify(true, Schemifier.infoF _, User)

    // where to search snippet
    LiftRules.addToPackages("code")

    // Build SiteMap
    def sitemap = SiteMap(
      Menu("Home") / "index",
      Menu("Login") / "login",
      Menu("Cart") / "cart",
      Menu("Account") /"account" >> If(() => ApiClient.isLoggedIn, ""),
      Menu("Checkout") /"checkout" >> If(() => ApiClient.isLoggedIn, ""))

    def sitemapMutators = User.sitemapMutator

    // set the sitemap.  Note if you don't want access control for
    // each page, just comment this line out.
    LiftRules.setSiteMapFunc(() => sitemapMutators(sitemap))

    //Init the jQuery module, see http://liftweb.net/jquery for more information.
    LiftRules.jsArtifacts = JQueryArtifacts
    JQueryModule.InitParam.JQuery=JQueryModule.JQuery191
    JQueryModule.init()

    //Show the spinny image when an Ajax call starts
    LiftRules.ajaxStart =
      Full(() => LiftRules.jsArtifacts.show("ajax-loader").cmd)
    
    // Make the spinny image go away when it ends
    LiftRules.ajaxEnd =
      Full(() => LiftRules.jsArtifacts.hide("ajax-loader").cmd)

    // Force the request to be UTF-8
    LiftRules.early.append(_.setCharacterEncoding("UTF-8"))

    // What is the function to test if a user is logged in?
    LiftRules.loggedInTest = Full(() => ApiClient.isLoggedIn)

    // Use HTML5 for rendering
    LiftRules.htmlProperties.default.set((r: Req) =>
      new Html5Properties(r.userAgent))    

    // Make a transaction span the whole HTTP request
    S.addAround(DB.buildLoanWrapper)

    LiftRules.uriNotFound.prepend(NamedPF("404handler"){
      case (req,failure) =>
        NotFoundAsTemplate(ParsePath(List("404"),"html",false,false))
    })

    LiftRules.noticesAutoFadeOut.default.set((noticeType: NoticeType.Value) => Full((2 seconds, 4 seconds)))
  }
}
