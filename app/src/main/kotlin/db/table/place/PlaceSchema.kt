package db.table.place

object PlaceSchema {
    const val NAME = "place"

    override fun toString(): String {
        return """
            CREATE TABLE $NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.Bundled} INTEGER NOT NULL,
                ${Columns.UpdatedAt} TEXT NOT NULL,
                ${Columns.Lat} REAL NOT NULL,
                ${Columns.Lon} REAL NOT NULL,
                ${Columns.Icon} TEXT NOT NULL,
                ${Columns.Name} TEXT,
                ${Columns.VerifiedAt} TEXT,
                ${Columns.Address} TEXT,
                ${Columns.OpeningHours} TEXT,
                ${Columns.Phone} TEXT,
                ${Columns.Website} TEXT,
                ${Columns.Email} TEXT,
                ${Columns.Twitter} TEXT,
                ${Columns.Facebook} TEXT,
                ${Columns.Instagram} TEXT,
                ${Columns.Line} TEXT,                
                ${Columns.RequiredAppUrl} TEXT,
                ${Columns.BoostedUntil} TEXT,
                ${Columns.Comments} INTEGER,
                ${Columns.Telegram} TEXT
            )
        """
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        Bundled("bundled"),
        UpdatedAt("updated_at"),
        Lat("lat"),
        Lon("lon"),
        Icon("icon"),
        Name("name"),
        VerifiedAt("verified_at"),
        Address("address"),
        OpeningHours("opening_hours"),
        Phone("phone"),
        Website("website"),
        Email("email"),
        Twitter("twitter"),
        Facebook("facebook"),
        Instagram("instagram"),
        Line("line"),
        RequiredAppUrl("required_app_url"),
        BoostedUntil("boosted_until"),
        Comments("comments"),
        Telegram("telegram");

        override fun toString() = sqlName
    }
}