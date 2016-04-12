package models.daos

import java.util.UUID
import javax.inject.Inject

import models._
import models.daos.tables._
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.driver.JdbcProfile
import slick.driver.PostgresDriver.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class RegistrationDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  private val registrations = TableQuery[RegistrationTable]
  private val persons = TableQuery[PersonTable]
  private val groups = TableQuery[GroupTable]
  private val organisations = TableQuery[OrganisationTable]
  private val categories = TableQuery[CategoryTable]

  def all: Future[Seq[Registration]] = {
    val query = for {
      (r, c) <- registrations joinLeft categories on (_.category_id === _.id)
      p <- persons if p.id === r.person_id
      g <- groups if g.id === p.group_id
      o <- organisations if o.id === g.organisation_id
    } yield (r, p, g, o, c)

    db.run(query.result).map(rows => rows.map {
      case (r, p, g, o, c) =>
        val group = Group(g.id, g.name, o)
        val person = Person(p.id, p.name, p.email, p.age, group)
        Registration(r.id, person, r.friday, r.saturday, r.sorting, c, r.team_leader, r.bbq, r.bbq_partner)
    })
  }

  def category(category_id: UUID): Future[Seq[Registration]] = {
    val query = for {
      (r, c) <- registrations joinLeft categories on (_.category_id === _.id)
      p <- persons if p.id === r.person_id && r.category_id === category_id
      g <- groups if g.id === p.group_id
      o <- organisations if o.id === g.organisation_id
    } yield (r, p, g, o, c)

    db.run(query.result).map(rows => rows.map {
      case (r, p, g, o, c) =>
        val group = Group(g.id, g.name, o)
        val person = Person(p.id, p.name, p.email, p.age, group)
        Registration(r.id, person, r.friday, r.saturday, r.sorting, c, r.team_leader, r.bbq, r.bbq_partner)
    })
  }

  def group(group_id: UUID): Future[Seq[Registration]] = {
    val query = for {
      (r, c) <- registrations joinLeft categories on (_.category_id === _.id)
      p <- persons if p.id === r.person_id && p.group_id === group_id
      g <- groups if g.id === group_id
      o <- organisations if o.id === g.organisation_id
    } yield (r, p, g, o, c)

    db.run(query.result).map(rows => rows.map {
      case (r, p, g, o, c) =>
        val group = Group(g.id, g.name, o)
        val person = Person(p.id, p.name, p.email, p.age, group)
        Registration(r.id, person, r.friday, r.saturday, r.sorting, c, r.team_leader, r.bbq, r.bbq_partner)
    })
  }

  def get(id: UUID): Future[Option[Registration]] = {
    val query = for {
      (r, c) <- registrations joinLeft categories on (_.category_id === _.id) if r.id === id
      p <- persons if p.id === r.person_id
      g <- groups if g.id === p.group_id
      o <- organisations if o.id === g.organisation_id
    } yield (r, p, g, o, c)

    db.run(query.result.headOption).map(rows => rows.map {
      case (r, p, g, o, c) =>
        val group = Group(g.id, g.name, o)
        val person = Person(p.id, p.name, p.email, p.age, group)
        Registration(r.id, person, r.friday, r.saturday, r.sorting, c, r.team_leader, r.bbq, r.bbq_partner)
    })
  }

  def save(registration: Registration): Future[Registration] = {
    db.run(registrations.insertOrUpdate(toDBRegistration(registration))).map(_ => registration)
  }

  def toDBRegistration(registration: Registration) =
    DBRegistration(
      registration.id,
      registration.person.id,
      registration.friday,
      registration.saturday,
      registration.sorting,
      registration.category.map(cat => cat.id),
      registration.teamLeader,
      registration.bbq,
      registration.bbqPartner
    )
}