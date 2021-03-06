package models.daos.tables

import java.util.UUID

import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

case class DBGroup(id: UUID, name: String, organisation_id: UUID)

class GroupTable(tag: Tag) extends Table[DBGroup](tag, "groups") {
  private val organisations = TableQuery[OrganisationTable]

  def * = (id, name, organisation_id) <> (DBGroup.tupled, DBGroup.unapply)

  def id = column[UUID]("id", O.PrimaryKey)

  def name = column[String]("name")

  def organisation_id = column[UUID]("organisation_id")

  def organisation = foreignKey("organisation_fk", organisation_id, organisations)(_.id)
}