package cut.the.crap.data.rest.eci

import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

/**
 * Localised call-to-action templates for motivating signatures, one set per EU official
 * language. Two bands, two tone variants each:
 *
 *  - **belowThreshold** — push a country toward its national threshold.
 *  - **buildingMargin** — threshold met, build a 2× safety margin against verification losses.
 *
 * Variant 0 mentions the `{deadline}`; variant 1 omits it. Placeholders: `{remaining}` (the
 * localised number of signatures still needed) and `{deadline}` (the localised collection
 * deadline). The country flag and the localised signing link are added by [EciPostGenerator],
 * so translators only handle the sentence.
 *
 * NOTE: translations below the English source are machine-drafted first passes and are
 * flagged `// needs native review` — they should be checked by a native speaker before a
 * public campaign relies on them.
 */
internal object EciPostTemplates {

    data class LangTemplates(
        val belowThreshold: List<String>,
        val buildingMargin: List<String>
    )

    /** Templates for a language, falling back to English when a language isn't covered. */
    fun forLanguage(language: String): LangTemplates =
        TEMPLATES[language.lowercase()] ?: TEMPLATES.getValue("en")

    private val TEMPLATES: Map<String, LangTemplates> = mapOf(
        "en" to LangTemplates(
            belowThreshold = listOf(
                "Only {remaining} more signatures are needed to reach the national threshold for this European Citizens' Initiative. Your voice counts — please sign before {deadline}:",
                "We're almost there — just {remaining} signatures away from the threshold. Add yours and help make it official:"
            ),
            buildingMargin = listOf(
                "We've passed the threshold — but signatures are verified and some are invalidated. {remaining} more would double our margin and protect the result. Please sign:",
                "Threshold reached! Let's keep going — {remaining} more signatures build a safe buffer so every voice holds. Add yours:"
            )
        ),
        // needs native review
        "de" to LangTemplates(
            belowThreshold = listOf(
                "Nur noch {remaining} Unterschriften fehlen, um die nationale Schwelle für diese Europäische Bürgerinitiative zu erreichen. Ihre Stimme zählt — bitte unterschreiben Sie vor dem {deadline}:",
                "Wir sind fast am Ziel — nur noch {remaining} Unterschriften bis zur Schwelle. Fügen Sie Ihre hinzu und helfen Sie mit:"
            ),
            buildingMargin = listOf(
                "Wir haben die Schwelle überschritten — aber Unterschriften werden geprüft und einige für ungültig erklärt. {remaining} weitere würden unseren Puffer verdoppeln und das Ergebnis sichern. Bitte unterschreiben Sie:",
                "Schwelle erreicht! Weiter so — {remaining} weitere Unterschriften schaffen einen sicheren Puffer, damit jede Stimme zählt. Machen Sie mit:"
            )
        ),
        // needs native review
        "fr" to LangTemplates(
            belowThreshold = listOf(
                "Il ne manque que {remaining} signatures pour atteindre le seuil national de cette initiative citoyenne européenne. Votre voix compte — signez avant le {deadline} :",
                "Nous y sommes presque — plus que {remaining} signatures avant le seuil. Ajoutez la vôtre et aidez-nous :"
            ),
            buildingMargin = listOf(
                "Nous avons dépassé le seuil — mais les signatures sont vérifiées et certaines invalidées. {remaining} de plus doubleraient notre marge et protégeraient le résultat. Signez :",
                "Seuil atteint ! Continuons — {remaining} signatures de plus créent une marge de sécurité pour que chaque voix compte. Ajoutez la vôtre :"
            )
        ),
        // needs native review
        "nl" to LangTemplates(
            belowThreshold = listOf(
                "Er zijn nog maar {remaining} handtekeningen nodig om de nationale drempel voor dit Europees burgerinitiatief te halen. Uw stem telt — teken vóór {deadline}:",
                "We zijn er bijna — nog maar {remaining} handtekeningen tot de drempel. Voeg de uwe toe en help mee:"
            ),
            buildingMargin = listOf(
                "We zijn over de drempel — maar handtekeningen worden gecontroleerd en sommige afgekeurd. {remaining} extra zouden onze marge verdubbelen en het resultaat beschermen. Teken alstublieft:",
                "Drempel gehaald! Laten we doorgaan — {remaining} extra handtekeningen bouwen een veilige buffer zodat elke stem telt. Voeg de uwe toe:"
            )
        ),
        // needs native review
        "it" to LangTemplates(
            belowThreshold = listOf(
                "Mancano solo {remaining} firme per raggiungere la soglia nazionale di questa iniziativa dei cittadini europei. La tua voce conta — firma entro il {deadline}:",
                "Ci siamo quasi — solo {remaining} firme alla soglia. Aggiungi la tua e aiutaci:"
            ),
            buildingMargin = listOf(
                "Abbiamo superato la soglia — ma le firme vengono verificate e alcune annullate. Altre {remaining} raddoppierebbero il nostro margine e proteggerebbero il risultato. Firma:",
                "Soglia raggiunta! Continuiamo — altre {remaining} firme creano un margine di sicurezza perché ogni voce conti. Aggiungi la tua:"
            )
        ),
        // needs native review
        "es" to LangTemplates(
            belowThreshold = listOf(
                "Solo faltan {remaining} firmas para alcanzar el umbral nacional de esta iniciativa ciudadana europea. Tu voz cuenta — firma antes del {deadline}:",
                "Ya casi está — solo {remaining} firmas para el umbral. Añade la tuya y ayúdanos:"
            ),
            buildingMargin = listOf(
                "Hemos superado el umbral — pero las firmas se verifican y algunas se invalidan. {remaining} más duplicarían nuestro margen y protegerían el resultado. Firma:",
                "¡Umbral alcanzado! Sigamos — {remaining} firmas más crean un margen seguro para que cada voz cuente. Añade la tuya:"
            )
        ),
        // needs native review
        "pt" to LangTemplates(
            belowThreshold = listOf(
                "Faltam apenas {remaining} assinaturas para atingir o limiar nacional desta iniciativa de cidadania europeia. A sua voz conta — assine antes de {deadline}:",
                "Estamos quase lá — faltam só {remaining} assinaturas para o limiar. Junte a sua e ajude:"
            ),
            buildingMargin = listOf(
                "Ultrapassámos o limiar — mas as assinaturas são verificadas e algumas invalidadas. Mais {remaining} duplicariam a nossa margem e protegeriam o resultado. Assine:",
                "Limiar atingido! Vamos continuar — mais {remaining} assinaturas criam uma margem segura para que cada voz conte. Junte a sua:"
            )
        ),
        // needs native review
        "pl" to LangTemplates(
            belowThreshold = listOf(
                "Brakuje już tylko {remaining} podpisów, aby osiągnąć krajowy próg tej europejskiej inicjatywy obywatelskiej. Twój głos się liczy — podpisz przed {deadline}:",
                "Jesteśmy blisko — jeszcze tylko {remaining} podpisów do progu. Dodaj swój i pomóż:"
            ),
            buildingMargin = listOf(
                "Przekroczyliśmy próg — ale podpisy są weryfikowane, a część unieważniana. Kolejne {remaining} podwoiłyby naszą rezerwę i zabezpieczyły wynik. Podpisz:",
                "Próg osiągnięty! Działajmy dalej — kolejne {remaining} podpisów tworzy bezpieczny zapas, by każdy głos się liczył. Dodaj swój:"
            )
        ),
        // needs native review
        "sv" to LangTemplates(
            belowThreshold = listOf(
                "Det saknas bara {remaining} underskrifter för att nå det nationella tröskelvärdet för detta europeiska medborgarinitiativ. Din röst räknas — skriv under före {deadline}:",
                "Vi är nästan framme — bara {remaining} underskrifter kvar till tröskeln. Lägg till din och hjälp till:"
            ),
            buildingMargin = listOf(
                "Vi har passerat tröskeln — men underskrifter granskas och vissa underkänns. {remaining} till skulle fördubbla vår marginal och skydda resultatet. Skriv under:",
                "Tröskeln nådd! Låt oss fortsätta — {remaining} underskrifter till bygger en säker marginal så att varje röst håller. Lägg till din:"
            )
        ),
        // needs native review
        "da" to LangTemplates(
            belowThreshold = listOf(
                "Der mangler kun {remaining} underskrifter for at nå den nationale tærskel for dette europæiske borgerinitiativ. Din stemme tæller — skriv under inden {deadline}:",
                "Vi er der næsten — kun {remaining} underskrifter fra tærsklen. Tilføj din og hjælp med:"
            ),
            buildingMargin = listOf(
                "Vi har passeret tærsklen — men underskrifter kontrolleres, og nogle underkendes. {remaining} mere ville fordoble vores margin og beskytte resultatet. Skriv under:",
                "Tærsklen er nået! Lad os fortsætte — {remaining} flere underskrifter opbygger en sikker buffer, så hver stemme tæller. Tilføj din:"
            )
        ),
        // needs native review
        "fi" to LangTemplates(
            belowThreshold = listOf(
                "Enää {remaining} allekirjoitusta puuttuu, jotta tämän eurooppalaisen kansalaisaloitteen kansallinen kynnys saavutetaan. Äänelläsi on merkitystä — allekirjoita ennen {deadline}:",
                "Olemme lähellä — vain {remaining} allekirjoitusta kynnykseen. Lisää omasi ja auta:"
            ),
            buildingMargin = listOf(
                "Olemme ylittäneet kynnyksen — mutta allekirjoitukset tarkistetaan ja osa hylätään. {remaining} lisää kaksinkertaistaisi marginaalimme ja turvaisi tuloksen. Allekirjoita:",
                "Kynnys saavutettu! Jatketaan — {remaining} allekirjoitusta lisää luo turvamarginaalin, jotta jokainen ääni pitää. Lisää omasi:"
            )
        ),
        // needs native review
        "el" to LangTemplates(
            belowThreshold = listOf(
                "Λείπουν μόνο {remaining} υπογραφές για να επιτευχθεί το εθνικό όριο αυτής της Ευρωπαϊκής Πρωτοβουλίας Πολιτών. Η φωνή σας μετράει — υπογράψτε πριν τις {deadline}:",
                "Είμαστε σχεδόν εκεί — μόνο {remaining} υπογραφές ακόμη για το όριο. Προσθέστε τη δική σας και βοηθήστε:"
            ),
            buildingMargin = listOf(
                "Ξεπεράσαμε το όριο — αλλά οι υπογραφές ελέγχονται και κάποιες ακυρώνονται. Άλλες {remaining} θα διπλασίαζαν το περιθώριό μας και θα προστάτευαν το αποτέλεσμα. Υπογράψτε:",
                "Το όριο επιτεύχθηκε! Ας συνεχίσουμε — {remaining} υπογραφές ακόμη χτίζουν ασφαλές περιθώριο ώστε κάθε φωνή να μετράει. Προσθέστε τη δική σας:"
            )
        ),
        // needs native review
        "cs" to LangTemplates(
            belowThreshold = listOf(
                "Chybí už jen {remaining} podpisů k dosažení národního prahu této evropské občanské iniciativy. Váš hlas se počítá — podepište před {deadline}:",
                "Jsme téměř u cíle — už jen {remaining} podpisů k prahu. Přidejte svůj a pomozte:"
            ),
            buildingMargin = listOf(
                "Překročili jsme práh — podpisy se však ověřují a některé se ruší. Dalších {remaining} by zdvojnásobilo naši rezervu a ochránilo výsledek. Podepište:",
                "Práh dosažen! Pokračujme — dalších {remaining} podpisů vytvoří bezpečnou rezervu, aby každý hlas platil. Přidejte svůj:"
            )
        ),
        // needs native review
        "sk" to LangTemplates(
            belowThreshold = listOf(
                "Chýba už len {remaining} podpisov na dosiahnutie národného prahu tejto európskej občianskej iniciatívy. Váš hlas sa počíta — podpíšte pred {deadline}:",
                "Sme takmer tam — už len {remaining} podpisov k prahu. Pridajte svoj a pomôžte:"
            ),
            buildingMargin = listOf(
                "Prekročili sme prah — podpisy sa však overujú a niektoré sa rušia. Ďalších {remaining} by zdvojnásobilo našu rezervu a ochránilo výsledok. Podpíšte:",
                "Prah dosiahnutý! Pokračujme — ďalších {remaining} podpisov vytvorí bezpečnú rezervu, aby každý hlas platil. Pridajte svoj:"
            )
        ),
        // needs native review
        "hu" to LangTemplates(
            belowThreshold = listOf(
                "Már csak {remaining} aláírás hiányzik ahhoz, hogy elérjük ennek az európai polgári kezdeményezésnek a nemzeti küszöbét. A te hangod is számít — írd alá {deadline} előtt:",
                "Majdnem megvan — már csak {remaining} aláírás a küszöbig. Add hozzá a tiédet, és segíts:"
            ),
            buildingMargin = listOf(
                "Átléptük a küszöböt — de az aláírásokat ellenőrzik, és néhányat érvénytelenítenek. További {remaining} megduplázná a tartalékunkat, és megvédené az eredményt. Írd alá:",
                "Küszöb elérve! Folytassuk — további {remaining} aláírás biztonságos tartalékot épít, hogy minden hang számítson. Add hozzá a tiédet:"
            )
        ),
        // needs native review
        "ro" to LangTemplates(
            belowThreshold = listOf(
                "Mai lipsesc doar {remaining} semnături pentru a atinge pragul național al acestei inițiative cetățenești europene. Vocea ta contează — semnează înainte de {deadline}:",
                "Aproape am reușit — doar {remaining} semnături până la prag. Adaugă-o pe a ta și ajută:"
            ),
            buildingMargin = listOf(
                "Am depășit pragul — dar semnăturile sunt verificate și unele invalidate. Încă {remaining} ne-ar dubla marja și ar proteja rezultatul. Semnează:",
                "Prag atins! Să continuăm — încă {remaining} semnături creează o marjă sigură ca fiecare voce să conteze. Adaugă-o pe a ta:"
            )
        ),
        // needs native review
        "bg" to LangTemplates(
            belowThreshold = listOf(
                "Остават само {remaining} подписа, за да се достигне националният праг на тази европейска гражданска инициатива. Вашият глас има значение — подпишете преди {deadline}:",
                "Почти сме там — само {remaining} подписа до прага. Добавете своя и помогнете:"
            ),
            buildingMargin = listOf(
                "Преминахме прага — но подписите се проверяват и някои се обезсилват. Още {remaining} биха удвоили резерва ни и защитили резултата. Подпишете:",
                "Прагът е достигнат! Да продължим — още {remaining} подписа изграждат сигурен резерв, за да има значение всеки глас. Добавете своя:"
            )
        ),
        // needs native review
        "hr" to LangTemplates(
            belowThreshold = listOf(
                "Nedostaje još samo {remaining} potpisa za dosezanje nacionalnog praga ove europske građanske inicijative. Vaš glas je važan — potpišite prije {deadline}:",
                "Gotovo smo tamo — još samo {remaining} potpisa do praga. Dodajte svoj i pomozite:"
            ),
            buildingMargin = listOf(
                "Prešli smo prag — ali potpisi se provjeravaju i neki poništavaju. Još {remaining} udvostručilo bi našu razliku i zaštitilo rezultat. Potpišite:",
                "Prag dosegnut! Nastavimo — još {remaining} potpisa gradi sigurnu rezervu da svaki glas vrijedi. Dodajte svoj:"
            )
        ),
        // needs native review
        "sl" to LangTemplates(
            belowThreshold = listOf(
                "Manjka le še {remaining} podpisov za dosego nacionalnega praga te evropske državljanske pobude. Vaš glas šteje — podpišite pred {deadline}:",
                "Skoraj smo tam — le še {remaining} podpisov do praga. Dodajte svojega in pomagajte:"
            ),
            buildingMargin = listOf(
                "Presegli smo prag — a podpisi se preverjajo in nekateri razveljavijo. Dodatnih {remaining} bi podvojilo našo rezervo in zaščitilo rezultat. Podpišite:",
                "Prag dosežen! Nadaljujmo — dodatnih {remaining} podpisov gradi varno rezervo, da vsak glas obvelja. Dodajte svojega:"
            )
        ),
        // needs native review
        "et" to LangTemplates(
            belowThreshold = listOf(
                "Selle Euroopa kodanikualgatuse riikliku künnise saavutamiseks on puudu vaid {remaining} allkirja. Sinu hääl loeb — allkirjasta enne {deadline}:",
                "Oleme peaaegu kohal — künniseni on jäänud vaid {remaining} allkirja. Lisa oma ja aita:"
            ),
            buildingMargin = listOf(
                "Ületasime künnise — kuid allkirju kontrollitakse ja osa tunnistatakse kehtetuks. Veel {remaining} kahekordistaks meie varu ja kaitseks tulemust. Allkirjasta:",
                "Künnis saavutatud! Jätkame — veel {remaining} allkirja loob turvavaru, et iga hääl loeks. Lisa oma:"
            )
        ),
        // needs native review
        "lv" to LangTemplates(
            belowThreshold = listOf(
                "Trūkst vēl tikai {remaining} parakstu, lai sasniegtu šīs Eiropas pilsoņu iniciatīvas valsts slieksni. Tava balss ir svarīga — paraksti pirms {deadline}:",
                "Esam gandrīz klāt — līdz slieksnim atlikuši tikai {remaining} paraksti. Pievieno savu un palīdzi:"
            ),
            buildingMargin = listOf(
                "Esam pārsnieguši slieksni — taču paraksti tiek pārbaudīti un daži atzīti par nederīgiem. Vēl {remaining} dubultotu mūsu rezervi un pasargātu rezultātu. Paraksti:",
                "Slieksnis sasniegts! Turpināsim — vēl {remaining} paraksti veido drošu rezervi, lai katra balss būtu svarīga. Pievieno savu:"
            )
        ),
        // needs native review
        "lt" to LangTemplates(
            belowThreshold = listOf(
                "Trūksta tik {remaining} parašų, kad būtų pasiektas šios Europos piliečių iniciatyvos nacionalinis slenkstis. Tavo balsas svarbus — pasirašyk iki {deadline}:",
                "Beveik pasiekėme — iki slenksčio liko tik {remaining} parašų. Pridėk savo ir padėk:"
            ),
            buildingMargin = listOf(
                "Peržengėme slenkstį — bet parašai tikrinami, o kai kurie panaikinami. Dar {remaining} padvigubintų mūsų atsargą ir apsaugotų rezultatą. Pasirašyk:",
                "Slenkstis pasiektas! Tęskime — dar {remaining} parašų sukuria saugią atsargą, kad kiekvienas balsas galiotų. Pridėk savo:"
            )
        ),
        // needs native review
        "ga" to LangTemplates(
            belowThreshold = listOf(
                "Níl uait ach {remaining} síniú eile chun tairseach náisiúnta an tionscnaimh Eorpaigh ó na saoránaigh seo a bhaint amach. Tá do ghlór tábhachtach — sínigh roimh {deadline}:",
                "Táimid beagnach ann — níl ach {remaining} síniú fágtha go dtí an tairseach. Cuir do cheann leis agus tabhair cúnamh:"
            ),
            buildingMargin = listOf(
                "Sháraíomar an tairseach — ach déantar na sínithe a fhíorú agus cuirtear cuid acu ar neamhní. Dhúblódh {remaining} eile ár lamháil agus chosnódh sé an toradh. Sínigh:",
                "Tairseach bainte amach! Leanaimis ar aghaidh — cruthaíonn {remaining} síniú eile maolán sábháilte ionas go seasann gach glór. Cuir do cheann leis:"
            )
        ),
        // needs native review
        "mt" to LangTemplates(
            belowThreshold = listOf(
                "Fadal biss {remaining} firem biex jintlaħaq il-limitu nazzjonali ta' din l-inizjattiva taċ-ċittadini Ewropej. Il-vuċi tiegħek tgħodd — iffirma qabel {deadline}:",
                "Kważi wasalna — {remaining} firem biss 'il bogħod mil-limitu. Żid tiegħek u għin:"
            ),
            buildingMargin = listOf(
                "Qbiżna l-limitu — imma l-firem jiġu vverifikati u xi wħud jiġu invalidati. {remaining} oħra jirdoppjaw il-marġni tagħna u jipproteġu r-riżultat. Iffirma:",
                "Il-limitu ntlaħaq! Ejja nkomplu — {remaining} firem oħra jibnu marġni sikur biex kull vuċi tgħodd. Żid tiegħek:"
            )
        )
    )
}

/** A single motivational post generated for one country in one language. */
data class EciGeneratedPost(
    val countryCode: String,
    val countryName: String,
    val language: String,
    val text: String
)

/**
 * Builds ready-to-post text from a country row, its band, and the chosen tone variants.
 * The flag emoji is prepended and the localised signing link appended; only the number and
 * (optionally) the deadline are filled into the localised template.
 */
object EciPostGenerator {

    /**
     * Generates one post per official language of [row]'s country. [belowVariant] and
     * [marginVariant] (0 or 1) pick the tone for each band. Returns empty if the row isn't
     * eligible (already at 2× or no threshold).
     */
    fun generateForCountry(
        row: EciCountrySignatures,
        statistics: EciStatistics,
        belowVariant: Int,
        marginVariant: Int
    ): List<EciGeneratedPost> {
        if (!row.isEligibleForPost) return emptyList()
        return EciReferenceData.officialLanguages(row.countryCode).map { language ->
            EciGeneratedPost(
                countryCode = row.countryCode,
                countryName = row.countryName,
                language = language,
                text = buildText(row, statistics, language, belowVariant, marginVariant)
            )
        }
    }

    private fun buildText(
        row: EciCountrySignatures,
        statistics: EciStatistics,
        language: String,
        belowVariant: Int,
        marginVariant: Int
    ): String {
        val templates = EciPostTemplates.forLanguage(language)
        val (variants, variantIndex) = when (row.band) {
            EciBand.BELOW_THRESHOLD -> templates.belowThreshold to belowVariant
            EciBand.BUILDING_MARGIN -> templates.buildingMargin to marginVariant
            else -> templates.belowThreshold to 0
        }

        val locale = Locale.forLanguageTag(language)
        val deadlineText = formatDeadline(statistics.deadline, locale)

        // Variant 0 references the deadline; if we don't have one, fall back to variant 1.
        var index = variantIndex.coerceIn(0, variants.size - 1)
        if (variants[index].contains("{deadline}") && deadlineText.isBlank() && variants.size > 1) {
            index = 1
        }

        val remaining = row.remainingToAim ?: 0L
        val remainingText = NumberFormat.getIntegerInstance(locale).format(remaining)

        val sentence = variants[index]
            .replace("{remaining}", remainingText)
            .replace("{deadline}", deadlineText)

        val flag = EciReferenceData.flagEmoji(row.countryCode)
        val link = statistics.supportLinks[language]
            ?: statistics.supportLinks["en"]
            ?: statistics.supportLinks.values.firstOrNull()
            ?: ""

        return buildString {
            if (flag.isNotEmpty()) append(flag).append(' ')
            append(sentence)
            if (link.isNotEmpty()) append(' ').append(link)
        }
    }

    /** Formats a "dd/MM/yyyy" deadline into [locale]'s medium date style, or "" if absent. */
    private fun formatDeadline(deadline: String?, locale: Locale): String {
        val date = EciReferenceData.parseDate(deadline) ?: return ""
        return date.format(
            DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        )
    }
}
