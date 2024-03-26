package controllers

import forms.{RegisterForm, RegistrationForm}
import models._
import models.daos._
import play.api.i18n.{I18nSupport, Messages}
import play.api.libs.json._
import play.api.mvc.{AbstractController, Action, AnyContent, ControllerComponents}
import play.silhouette.api.Silhouette
import utils.DefaultEnv

import java.util.UUID
import javax.inject.Inject
import scala.concurrent.{ExecutionContext, Future}

class Registrations @Inject()(mail: Mail,
                              registrationDAO: RegistrationDAO,
                              organisationDAO: OrganisationDAO,
                              groupDAO: GroupDAO,
                              categoryDAO: CategoryDAO,
                              personDAO: PersonDAO,
                              components: ControllerComponents,
                              silhouette: Silhouette[DefaultEnv],
                              registrationsTemplate: views.html.registrations,
                              registrationTemplate: views.html.registration,
                              registerTemplate: views.html.register,
                              notFoundTemplate: views.html.notFound,
                              badRequestTemplate: views.html.badRequest)
                             (implicit ec: ExecutionContext)
  extends AbstractController(components) with I18nSupport {

  def registrations(): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    registrationDAO.all.map(registrations =>
      Ok(registrationsTemplate(registrations.sortBy(_.person.name), request.identity)))
  }

  def registration(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    try {
      val uuid = UUID.fromString(id)
      for {
        categories <- categoryDAO.all
        registration <- registrationDAO.get(uuid)
      } yield {
        registration match {
          case Some(r) => Ok(registrationTemplate(
            registration = r,
            registrationForm = RegistrationForm.form,
            categories = categoriesTupled(categories),
            secondChoices = secondChoicesTupled(categories),
            user = request.identity))
          case None => NotFound(notFoundTemplate(id, Some(request.identity)))
        }
      }
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(notFoundTemplate(Messages("uuid.invalid"), Some(request.identity))))
    }
  }

  def register(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    for {
      organisations <- organisationDAO.all
      groups <- groupDAO.all
      categories <- categoryDAO.all
    } yield {
      Ok(registerTemplate(
        registerForm = RegisterForm.form,
        organisations = organisationsTupled(organisations),
        groups = groupsTupled(groups),
        categories = categoriesTupled(categories),
        secondChoices = secondChoicesTupled(categories),
        user = request.identity))
    }
  }

  def save(): Action[AnyContent] = silhouette.UserAwareAction.async { implicit request =>
    RegisterForm.form.bindFromRequest().fold(
      form => for {
        organisations <- organisationDAO.all
        groups <- groupDAO.all
        categories <- categoryDAO.all
      } yield {
        BadRequest(registerTemplate(
          registerForm = form,
          organisations = organisationsTupled(organisations),
          groups = groupsTupled(groups),
          categories = categoriesTupled(categories),
          secondChoices = secondChoicesTupled(categories),
          user = request.identity))
      },
      data => {
        try {
          val group_id = UUID.fromString(data.group.split('#')(1))
          groupDAO.get(group_id).flatMap {
            case Some(group) =>
              val p = Person(UUID.randomUUID, data.fullName, data.email.toLowerCase, data.age, group)
              personDAO.save(p).flatMap { person =>
                (data.category, data.secondChoice) match {
                  case (Some(category), Some(secondChoice)) =>
                    try {
                      val category_id = UUID.fromString(category)
                      categoryDAO.get(category_id).flatMap {
                        case Some(cat) =>
                          val second_choice_id = UUID.fromString(secondChoice);
                          categoryDAO.get(second_choice_id).flatMap {
                            case Some(second) =>
                              val registration = Registration(UUID.randomUUID, person, data.friday, data.saturday,
                                data.sorting, Some(cat), Some(second), teamLeader = false)
                              registrationDAO.save(registration).flatMap(registration => {
                                Future.successful(mail.sendConfirmation(registration, Messages("confirmation.subject")))
                                val flash = ("message", Messages("registered"))
                                Future.successful(Redirect(routes.Application.index()).flashing(flash))
                              })
                            case None =>
                              val error = Messages("object.not.found") + ": " + category_id
                              Future.successful(BadRequest(badRequestTemplate(error, request.identity)))
                          }
                        case None =>
                          val error = Messages("object.not.found") + ": " + category_id
                          Future.successful(BadRequest(badRequestTemplate(error, request.identity)))
                      }
                    } catch {
                      case _: IllegalArgumentException =>
                        Future.successful(BadRequest(badRequestTemplate(Messages("uuid.invalid"), request.identity)))
                    }
                  case (None, None) =>
                    val registration = Registration(UUID.randomUUID, person, data.friday, data.saturday,
                      data.sorting, None, None, teamLeader = false)
                    registrationDAO.save(registration).flatMap(registration => {
                      Future.successful(mail.sendConfirmation(registration, Messages("confirmation.subject")))
                      val flash = ("message", Messages("registered"))
                      Future.successful(Redirect(routes.Application.index()).flashing(flash))
                    })
                  case (_, _) =>
                    val error = Messages("bad.request")
                    Future.successful(BadRequest(badRequestTemplate(error, request.identity)))
                }
              }
            case None =>
              val error = Messages("object.not.found") + ": " + group_id
              Future.successful(BadRequest(badRequestTemplate(error, request.identity)))
          }
        } catch {
          case _: IllegalArgumentException =>
            Future.successful(BadRequest(badRequestTemplate(Messages("uuid.invalid"), request.identity)))
        }
      }
    )
  }

  def update(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    try {
      val uuid = UUID.fromString(id)
      RegistrationForm.form.bindFromRequest().fold(
        form => {
          for {
            registration <- registrationDAO.get(uuid)
            categories <- categoryDAO.all
          } yield {
            registration match {
              case Some(r) =>
                BadRequest(registrationTemplate(
                  registration = r,
                  registrationForm = form,
                  categories = categoriesTupled(categories),
                  secondChoices = secondChoicesTupled(categories),
                  user = request.identity))
              case None =>
                val error = Messages("object.not.found") + ": " + id
                BadRequest(badRequestTemplate(error, Some(request.identity)))
            }
          }
        },
        data => {
          registrationDAO.get(uuid).flatMap {
            case Some(registration) =>
              (data.category, data.secondChoice) match {
                case (Some(category), Some(secondChoice)) =>
                  try {
                    val category_id = UUID.fromString(category)
                    categoryDAO.get(category_id).flatMap {
                      case Some(c) =>
                        val second_choice_id = UUID.fromString(secondChoice);
                        categoryDAO.get(second_choice_id).flatMap {
                          case Some(second) =>
                            val updatedRegistration = registration.copy(
                              friday = data.friday,
                              saturday = data.saturday,
                              sorting = data.sorting,
                              category = Some(c),
                              secondChoice = Some(second),
                              teamLeader = data.teamLeader)
                            registrationDAO.save(updatedRegistration).flatMap(_ =>
                              Future.successful(Redirect(routes.Registrations.registrations()))
                            )
                          case None =>
                            val error = Messages("object.not.found") + ": " + category
                            Future.successful(BadRequest(badRequestTemplate(error, Some(request.identity))))
                        }
                      case None =>
                        val error = Messages("object.not.found") + ": " + category
                        Future.successful(BadRequest(badRequestTemplate(error, Some(request.identity))))
                    }
                  } catch {
                    case _: IllegalArgumentException =>
                      Future.successful(BadRequest(badRequestTemplate(Messages("uuid.invalid"), Some(request.identity))))
                  }
                case (None, None) =>
                  val updatedRegistration = registration.copy(
                    friday = data.friday,
                    saturday = data.saturday,
                    sorting = data.sorting,
                    category = None,
                    secondChoice = None,
                    teamLeader = data.teamLeader)
                  registrationDAO.save(updatedRegistration).flatMap(_ => {
                    Future.successful(Redirect(routes.Registrations.registrations()))
                  })
                case (_, _) =>
                  val error = Messages("bad.request")
                  Future.successful(BadRequest(badRequestTemplate(error, Some(request.identity))))
              }
            case None =>
              val error = Messages("object.not.found") + ": " + id
              Future.successful(BadRequest(badRequestTemplate(error, Some(request.identity))))
          }
        }
      )
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(badRequestTemplate(Messages("uuid.invalid"), Some(request.identity))))
    }
  }

  def delete(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    try {
      val uuid = UUID.fromString(id)
      registrationDAO.get(uuid).flatMap {
        case Some(registration) =>
          for {
            _ <- registrationDAO.delete(uuid)
            _ <- personDAO.delete(registration.person.id)
          } yield Redirect(routes.Registrations.registrations())
        case None => Future.successful(NotFound(notFoundTemplate(id, Some(request.identity))))
      }
    } catch {
      case _: IllegalArgumentException =>
        Future.successful(BadRequest(badRequestTemplate(Messages("uuid.invalid"), Some(request.identity))))
    }
  }

  def all(): Action[AnyContent] = silhouette.SecuredAction.async {
    registrationDAO.all.map(registrations => Ok(Json.toJson(Map("registrations" -> registrations))))
  }

  def get(id: String): Action[AnyContent] = silhouette.SecuredAction.async { implicit request =>
    try {
      registrationDAO.get(UUID.fromString(id)).map {
        case Some(registration) => Ok(Json.toJson(Map("registration" -> registration)))
        case None => NotFound(Json.toJson(Map("error" -> Messages("registration.not_found"))))
      }
    } catch {
      case _: IllegalArgumentException => Future.successful(BadRequest(Json.toJson(Map("error" -> Messages("uuid.invalid")))))
    }
  }
}
