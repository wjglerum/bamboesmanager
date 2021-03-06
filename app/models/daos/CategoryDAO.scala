package models.daos

import java.util.UUID
import javax.inject.Inject

import models.Category
import models.daos.tables.{CategoryTable, PersonTable, RegistrationTable}
import play.api.db.slick.{DatabaseConfigProvider, HasDatabaseConfigProvider}
import slick.jdbc.JdbcProfile
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CategoryDAO @Inject()(protected val dbConfigProvider: DatabaseConfigProvider)
  extends HasDatabaseConfigProvider[JdbcProfile] {

  private val categories = TableQuery[CategoryTable]
  private val registrations = TableQuery[RegistrationTable]
  private val persons = TableQuery[PersonTable]

  def all: Future[Seq[Category]] = db.run(categories.result)

  def get(id: UUID): Future[Option[Category]] = db.run(categories.filter(_.id === id).result.headOption)

  def save(category: Category): Future[Int] = db.run(categories.insertOrUpdate(category))

  def teamLeaders: Future[Map[UUID, String]] = {
    val query = for {
      r <- registrations if r.team_leader
      p <- persons if r.person_id === p.id
    } yield (r.category_id, p.name)

    db.run(query.result).map { results =>
      results.collect {
        case (Some(category), teamLeaders) => category -> teamLeaders
      }.toMap
    }
  }
}