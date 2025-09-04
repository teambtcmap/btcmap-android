package db.table.comment

object CommentSchema {
    const val NAME = "comment"

    override fun toString(): String {
        return """
            CREATE TABLE $NAME (
                ${Columns.Id} INTEGER PRIMARY KEY NOT NULL,
                ${Columns.PlaceId} INTEGER NOT NULL,
                ${Columns.Comment} TEXT NOT NULL,
                ${Columns.CreatedAt} TEXT NOT NULL,
                ${Columns.UpdatedAt} TEXT NOT NULL
            )
        """
    }

    enum class Columns(val sqlName: String) {
        Id("id"),
        PlaceId("place_id"),
        Comment("comment"),
        CreatedAt("created_at"),
        UpdatedAt("updated_at");

        override fun toString() = sqlName
    }
}