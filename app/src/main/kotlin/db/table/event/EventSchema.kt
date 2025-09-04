package db.table.event

object EventSchema {
    const val NAME = "event"

    override fun toString(): String {
        return """
            CREATE TABLE $NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.Lat} REAL NOT NULL,
                ${Columns.Lon} REAL NOT NULL,
                ${Columns.Name} TEXT NOT NULL,
                ${Columns.Website} TEXT NOT NULL,
                ${Columns.StartsAt} TEXT NOT NULL,
                ${Columns.EndsAt} TEXT
            )
        """
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        Lat("lat"),
        Lon("lon"),
        Name("name"),
        Website("website"),
        StartsAt("starts_at"),
        EndsAt("ends_at");

        override fun toString() = sqlName
    }
}