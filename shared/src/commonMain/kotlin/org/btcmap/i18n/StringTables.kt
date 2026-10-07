package org.btcmap.i18n

import org.btcmap.i18n.tables.AF
import org.btcmap.i18n.tables.AR
import org.btcmap.i18n.tables.BG
import org.btcmap.i18n.tables.BN
import org.btcmap.i18n.tables.CA
import org.btcmap.i18n.tables.CS
import org.btcmap.i18n.tables.DA
import org.btcmap.i18n.tables.DE
import org.btcmap.i18n.tables.EL
import org.btcmap.i18n.tables.EN
import org.btcmap.i18n.tables.ES
import org.btcmap.i18n.tables.FA
import org.btcmap.i18n.tables.FI
import org.btcmap.i18n.tables.FR
import org.btcmap.i18n.tables.HI
import org.btcmap.i18n.tables.HU
import org.btcmap.i18n.tables.IT
import org.btcmap.i18n.tables.IW
import org.btcmap.i18n.tables.JA
import org.btcmap.i18n.tables.KO
import org.btcmap.i18n.tables.NL
import org.btcmap.i18n.tables.NO
import org.btcmap.i18n.tables.PL
import org.btcmap.i18n.tables.PT
import org.btcmap.i18n.tables.PT_BR
import org.btcmap.i18n.tables.RO
import org.btcmap.i18n.tables.RU
import org.btcmap.i18n.tables.SK
import org.btcmap.i18n.tables.SR
import org.btcmap.i18n.tables.SV
import org.btcmap.i18n.tables.TH
import org.btcmap.i18n.tables.TR
import org.btcmap.i18n.tables.UK
import org.btcmap.i18n.tables.UR
import org.btcmap.i18n.tables.VI
import org.btcmap.i18n.tables.ZH

/** The migrated string table for an exact locale tag, or null. */
internal fun tableForExact(tag: String): Map<String, String>? = when (tag) {
        "en" -> EN
        "af" -> AF
        "ar" -> AR
        "bg" -> BG
        "bn" -> BN
        "ca" -> CA
        "cs" -> CS
        "da" -> DA
        "de" -> DE
        "el" -> EL
        "es" -> ES
        "fa" -> FA
        "fi" -> FI
        "fr" -> FR
        "hi" -> HI
        "hu" -> HU
        "it" -> IT
        "iw" -> IW
        "ja" -> JA
        "ko" -> KO
        "nl" -> NL
        "no" -> NO
        "pl" -> PL
        "pt" -> PT
        "pt-BR" -> PT_BR
        "ro" -> RO
        "ru" -> RU
        "sk" -> SK
        "sr" -> SR
        "sv" -> SV
        "th" -> TH
        "tr" -> TR
        "uk" -> UK
        "ur" -> UR
        "vi" -> VI
        "zh" -> ZH
        "he" -> IW
        "nb" -> NO
        "nn" -> NO
        else -> null
    }
