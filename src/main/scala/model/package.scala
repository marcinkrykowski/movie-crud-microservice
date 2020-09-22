package object model {
  abstract sealed class Rate(val value: String)
  case object High extends Rate("Good")
  case object Medium extends Rate("Medium")
  case object Low extends Rate("Bad")

  object Rate {
    private def values = Set(High, Medium, Low)

    def unsafeFromString(value: String): Rate = {
      values.find(_.value == value).get
    }
  }

  case class Movie(
      id: Option[Long],
      title: String,
      rate: Rate
  )

  case object MovieNotFoundError
}
