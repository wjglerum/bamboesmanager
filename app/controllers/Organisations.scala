package controllers

import java.util.UUID
import javax.inject.Inject

import com.mohiva.play.silhouette.api.{Environment, Silhouette}
import com.mohiva.play.silhouette.impl.authenticators.SessionAuthenticator
import forms.OrganisationForm
import models._
import models.daos.OrganisationDAO
import play.api.i18n.{Messages, MessagesApi}
import play.api.libs.json.Json

import scala.concurrent.ExecutionContext.Implicits._
import scala.concurrent.Future

class Organisations @Inject()(organisationDAO: OrganisationDAO,
                              val messagesApi: MessagesApi,
                              val env: Environment[User, SessionAuthenticator])
  extends Silhouette[User, SessionAuthenticator] {

  def organisations = SecuredAction.async { implicit request =>
    organisationDAO.all.map(organisations =>
      Ok(views.html.organisations(organisations.sortBy(_.name), request.identity)))
  }

  def organisation(id: String) = SecuredAction.async { implicit request =>
    try {
      val uuid = UUID.fromString(id)
      for {
        groups <- organisationDAO.groups(uuid)
        organisation <- organisationDAO.get(uuid)
      } yield {
        organisation match {
          case Some(o) => Ok(views.html.organisation(o, groups.sortBy(_.name), request.identity))
          case None => NotFound(views.html.notFound(id, Some(request.identity)))
        }
      }
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(views.html.badRequest(Messages("uuid.invalid"), Some(request.identity))))
    }
  }

  def add = SecuredAction.async { implicit request =>
    Future.successful(Ok(views.html.organisationAdd(OrganisationForm.form, request.identity)))
  }

  def save = SecuredAction.async { implicit request =>
    OrganisationForm.form.bindFromRequest.fold(
      form => Future.successful(BadRequest(views.html.organisationAdd(form, request.identity))),
      data => {
        val organisation = Organisation(UUID.randomUUID, data.name)
        for {
          _ <- organisationDAO.save(organisation)
          groups <- organisationDAO.groups(organisation.id)
        } yield {
          Ok(views.html.organisation(organisation, groups, request.identity))
        }
      }
    )
  }

  def all = SecuredAction.async {
    organisationDAO.all.map(organisations => Ok(Json.toJson(organisations.sortBy(_.name))))
  }

  def get(id: String) = SecuredAction.async {
    try {
      val uuid = UUID.fromString(id)
      organisationDAO.get(uuid).map {
        case Some(organisation) => Ok(Json.toJson(organisation))
        case None => NotFound(Json.toJson(Messages("organisation.not_found")))
      }
    } catch {
      case _: IllegalArgumentException => Future.successful(BadRequest(Json.toJson(Messages("uuid.invalid"))))
    }
  }
}