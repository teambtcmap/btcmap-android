package org.btcmap.i18n

/**
 * A language the UI can be shown in: its BCP-47 [tag] and its endonym, the name
 * written in that language, so the picker reads the same to a speaker of it
 * regardless of the language the app is currently showing.
 */
data class AppLanguage(
    val tag: String,
    val name: String,
)

/**
 * The languages that have a string table (see `StringTables`), in the tag order
 * the catalog declares them. [Strings.forLocale] falls back to English for any
 * tag not listed here, so the picker offers exactly these; a host prepends its
 * own "follow the device" option.
 *
 * The region-bearing `pt-BR` entry keeps its own table, so it is listed
 * separately from `pt`.
 */
val appLanguages: List<AppLanguage> = listOf(
    AppLanguage("en", "English"),
    AppLanguage("af", "Afrikaans"),
    AppLanguage("ar", "العربية"),
    AppLanguage("bg", "Български"),
    AppLanguage("bn", "বাংলা"),
    AppLanguage("ca", "Català"),
    AppLanguage("cs", "Čeština"),
    AppLanguage("da", "Dansk"),
    AppLanguage("de", "Deutsch"),
    AppLanguage("el", "Ελληνικά"),
    AppLanguage("es", "Español"),
    AppLanguage("fa", "فارسی"),
    AppLanguage("fi", "Suomi"),
    AppLanguage("fr", "Français"),
    AppLanguage("he", "עברית"),
    AppLanguage("hi", "हिन्दी"),
    AppLanguage("hu", "Magyar"),
    AppLanguage("it", "Italiano"),
    AppLanguage("ja", "日本語"),
    AppLanguage("ko", "한국어"),
    AppLanguage("nl", "Nederlands"),
    AppLanguage("no", "Norsk"),
    AppLanguage("pl", "Polski"),
    AppLanguage("pt", "Português"),
    AppLanguage("pt-BR", "Português (Brasil)"),
    AppLanguage("ro", "Română"),
    AppLanguage("ru", "Русский"),
    AppLanguage("sk", "Slovenčina"),
    AppLanguage("sr", "Српски"),
    AppLanguage("sv", "Svenska"),
    AppLanguage("th", "ไทย"),
    AppLanguage("tr", "Türkçe"),
    AppLanguage("uk", "Українська"),
    AppLanguage("ur", "اردو"),
    AppLanguage("vi", "Tiếng Việt"),
    AppLanguage("zh", "中文"),
)
