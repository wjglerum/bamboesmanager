import play.api.libs.json.{Json, Writes}

package object models {

  implicit val organisationWrites: Writes[Organisation] = Json.writes[Organisation]
  implicit val groupWrites: Writes[Group] = Json.writes[Group]
  implicit val personWrites: Writes[Person] = Json.writes[Person]
  implicit val categoryWrites: Writes[Category] = Json.writes[Category]
  implicit val registrationWrites: Writes[Registration] = Json.writes[Registration]

  def categoriesTupled(categories: Seq[Category]): Seq[(String, String)] =
    categories.sortBy(_.name).map(category => (category.id.toString, category.name))

  def secondChoicesTupled(categories: Seq[Category]): Seq[(String, String)] =
    categories.filter(_.secondChoice).sortBy(_.name).map(category => (category.id.toString, category.name))

  def groupsTupled(groups: Seq[Group]): Seq[(String, String)] =
    groups.sortBy(group => (group.organisation.name, group.name)).map { group =>
      (group.organisation.id.toString + "#" + group.id.toString, group.organisation.name + ": " + group.name)
    }

  def organisationsTupled(organisations: Seq[Organisation]): Seq[(String, String)] =
    organisations.sortBy(_.name).map(organisation => (organisation.id.toString, organisation.name))
}
