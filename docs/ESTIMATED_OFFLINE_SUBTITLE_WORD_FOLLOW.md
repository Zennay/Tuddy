# Zelf handmatig woordtijdankers plaatsen (optioneel)

Als de **Geschatte woordvolger** aan staat, kun je vanaf nu binnen een
zichtbare ondertitelregel maximaal **16 handmatige woordtijdankers** zetten.
Typ een **origineel woordnummer** vanaf 2 in het veld onder
**Handmatige woordankers**. Zet het geluid aan, ga naar de gewenste
positie en tik op **Markeer dit woord op huidige audiotijd**. Als je een
nummer opnieuw markeert, vervangt het nieuwe moment jouw vorige keuze.
Woord 1 blijft bewust gekoppeld aan het begin van de oorspronkelijke cue.

Tuddy gebruikt deze **zelf gekozen** posities voor de visuele woordvolger,
**Vorig/Volgend**, direct springen en het herhalen van één woord of
woordgroep. Tussen jouw ankers worden tussenliggende woorden nog steeds
**gelijkmatig geschat**; vóór het eerste of na het laatste anker ook.
Markeringen moeten binnen de huidige cue liggen, in strikt oplopende
woord- én tijdvolgorde passen en minimaal één milliseconde per woord
overlaten. Bij een ongeldige keuze blijft de bestaande set behouden.
Je kunt alle ankers voor de actieve regel via **Herstel gelijkmatige
woordschatting voor deze regel** wissen. Een gewijzigde ondertitel-offset
wist alle handmatige ankers omdat ze dan niet meer bij dezelfde tijdcodes
horen. Ander audio- of ondertitelmateriaal en sluiten verwijderen de
sessiemarkeringen eveneens.

**Geen automatische alignering:** een tik tijdens het luisteren is een
menselijke tijdinschatting, géén bewezen begin van het uitgesproken woord.
De app analyseert of verstuurt geen audio; er zijn geen nieuwe permissies,
geen gewijzigde originele SRT/VTT en geen automatische opslag.

---

## Nieuw: aaneengesloten woordgroep bewust als één fragment herhalen

Open **Kies direct een woord uit deze ondertitel** terwijl de optionele
woordvolger aanstaat. Vul **Van woord** en **Tot en met** in, bijvoorbeeld
`3` en `7`. Je kunt de nummers intypen of tijdens het luisteren met **Start = huidig woord**
en **Eind = huidig woord** de twee grenzen rechtstreeks vastleggen.
De app toont de daadwerkelijk gekozen originele woordgroep vóór herhalen.
Tik op **↻ Herhaal gekozen woordgroep** om de geschatte
tijdspanne van die vijf aaneengesloten woorden binnen dezelfde actieve
ondertitel te herhalen. De bestaande knop **Stop geschatte
woordherhaling** stopt zowel een enkel woord als de gekozen groep.

Beide oorspronkelijke woordnummers moeten bestaan in de huidige
ondertitel en oplopend zijn. Een ongeldige selectie heeft geen
herhaalknop; de knop werkt alleen voor een geschat audiovenster van
**0,75 tot 120 seconden** dat geheel binnen de geopende audio valt.
Bij wisselen naar een andere ondertitel worden de eigen woordkeuzes
gewist. Een gewone cue-/A–B-oefening, gewijzigde tijdverschuiving,
andere audio of uitschakelen van de modus beëindigt de woordherhaling.

Deze tijdspanne komt **uitsluitend uit een evenredige deling van eigen
SRT/VTT-cuetijden**, niet uit daadwerkelijke audiodetectie. De keuze
wordt alleen in de huidige Android-spelersessie gehouden, zonder opname,
cloud, nieuwe toestemming of permanent oefenresultaat.

---

## Nieuw: één geschat woordfragment herhalen

In de optionele geschatte woordvolger kun je op **↻ Herhaal alleen dit geschatte
woordfragment** tikken. Tuddy berekent het kleine A–B-venster vanuit het
**huidige eigen SRT/VTT-woordnummer en de zelfgekozen cue-begin- en eindtijd**.
Er is geen ASR, woordherkenning, fonetische segmentering of gemeten
geluidstiming. Dit is een handmatig gekozen ruwe lees- en luisterherhaling.

Om herhalen van extreem korte, onnauwkeurige vensters te voorkomen is
de knop alleen beschikbaar bij **minimaal 750 milliseconden** per geschat
woord en een volledig geldig venster binnen de daadwerkelijk gekozen audio.
De knop **Stop geschatte woordherhaling** heft het tijdelijke venster op.
Zoals bij de bestaande A–B-herhaling druk je zelf op Afspelen als het geluid
gepauzeerd is; de app start geen opname. Een handmatige sprong buiten
het fragment, het uitschakelen van de woordvolger, het kiezen van een
gewone A–B-/cue-/markeringsoefening, het wijzigen van de tijdelijke
ondertiteltijdverschuiving of het sluiten van de audiospeler heft de
woordherhaling op. De bestaande limiet voor handmatig aantal
herhalingen blijft gelden; niets wordt naar opslag weggeschreven.

Dit is nadrukkelijk **geen bewijs** dat het geïsoleerde geluidsfragment
precies het getoonde woord bevat. De foutmarge komt uit de gekozen
tijdcodes en niet uit echte woord-/audio-uitlijning.

---

# Gescheiden tijdcodewoordvolger bij je eigen audio

## Meteen naar een zelfgekozen woord in de actieve regel

Open binnen de optionele woordvolger **Kies direct een woord uit deze
ondertitel**. Een doorbladerbare lijst toont maximaal negen opeenvolgende
woorden per pagina, met de originele tokenvolgorde en woordnummers.
Tik op een woord om direct naar het uit de originele cue-tijd geschatte
moment te springen, zonder eerst alle eerdere woorden langs te gaan.
Bij een lange ondertitel kun je ook een **oorspronkelijk woordnummer**
van 1 t/m het zichtbare totale aantal invoeren en op **Ga** tikken.
Tuddy bladert naar de betreffende woordpagina en springt naar dezelfde
geschatte tijdpositie. Het getal is begrensd en wordt gewist wanneer
de actieve ondertitel verandert. De woordlijst ondersteunt ook
rechts-naar-links-tekst in Sorani en Pasjtoe.
De lijst is begrensd op maximaal 300 woorden per ondertitel en wordt
bij een andere regel opnieuw vanaf de eerste pagina getoond.

De wordsegmenten en springposities zijn nu met exacte gehele
milliseconden berekend: dezelfde geschatte grens wordt gebruikt voor
kleuren en springen, zonder floating-pointafronding. Extreem lange of
ongeldige tijdvakken worden geweigerd in plaats van onveilig berekend.

Dit is uitsluitend een handmatige navigatiebediening bovenop de
bestaande gelijke tijdverdeling. Er is geen automatische woordherkenning,
spraakverwerking, geluidsexport, permanente oefenhistorie of gewijzigde
bestandsrechten. Woordkaarten blijven beschikbaar nadat je de
woordvolger zelf uitzet.

Tuddy gebruikt in de eigen-audiospeler een door jou gekozen audiofragment
en zelf aangeleverd SRT/VTT-bestand. Standaard blijven alle
ondertitelwoorden **aanklikbaar** voor de lokale woordkaart.

Tik bij een actieve ondertitel op **Geschatte woordvolger proberen**
als je liever een visueel meelopend woord ziet. Binnen één SRT/VTT-
regel verdeelt Tuddy het opgegeven tijdvak *evenredig* over de
zichtbare woorden. Het actuele woord krijgt een bescheiden gekleurde
achtergrond terwijl je audio afspeelt of handmatig naar een andere
audiopositie schuift. Met **Vorig woord**, **↻ Woord** en
**Volgend** spring je zelf naar de geschatte plek van het vorige,
huidige of volgende woord. Dit werkt ook als je pauzeert om zinnen
langzamer opnieuw te lezen; deze knoppen starten niet uit zichzelf
een spraakherkenning of nieuwe opname. Tijdcorrecties van de bestaande audiospeler
verschuiven deze inschatting mee.

**Dit is een ruwe visuele leeshulp, geen echte woord-timing.**
De app herkent geen woord in de audio, voert geen geforceerde
spraakuitlijning uit en claimt niet dat het gemarkeerde woord echt
op dat moment wordt uitgesproken. Spreekpauzes, verschillende
woordlengtes of foute SRT/VTT-tijden maken de schatting onnauwkeurig.
Buiten een actieve ondertitel verschijnt geen gemarkeerd woord.
Ongeldige tijdvakken en te grote tekstregels krijgen geen woordstappen.

De modus staat standaard **uit** en wordt bij het sluiten van de
audiospeler of kiezen van andere audio gereset. Zet de modus uit om
weer op woorden te tikken, hun bestaande lokaal gecontroleerde
woordenkaart te bekijken en eigen betekenissen afzonderlijk op te
slaan. Er zijn geen nieuwe rechten, netwerkverzoeken, transcript- of
audiobestanden en er wordt niets automatisch bewaard.
