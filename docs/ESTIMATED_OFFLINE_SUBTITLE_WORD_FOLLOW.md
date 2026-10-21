## Relatieve woordankers voor een andere zelfgekozen ondertitel

Naast absolute tijdcodes is er nu een eigen **cue-relatieve** notatie:
`2=+00:00:01.250;5=+00:00:02.900`. Die tijden betekenen 1,25
en 2,9 seconden **na het begin van de actieve ondertitel**.
Bij al geplaatste handmatige ankers verschijnt een apart selecteerbaar
overzicht met **Kopieer relatieve ankers** en **Vul relatieve tijden in**.
Je kunt dezelfde zelfgekozen offsets later bewust in een andere,
passende cue invoeren; de nieuwe cue moet de hele reeks opnieuw
accepteren. Zonder passende tekst, woordvolgorde en genoeg duur
wordt het voorstel geheel geweigerd.

Gebruik uitsluitend één vorm per invoer: alle tijden absoluut
(`2=00:00:12.250`) óf alle tijden relatief
(`2=+00:00:01.250`). Gemengde reeksen, negatieve offsets,
buiten-cue-tijden, dubbelen en onmogelijke woordinterpolatie
worden volledig afgewezen. Maximaal 16 ankers en 512 tekens;
bestaande bewuste preview, aparte bevestiging en RAM-only
sessiegedrag veranderen niet. Het formaat verplaatst geen
audio of ondertitelbron, detecteert geen woorden en verricht
geen automatische taaluitlijning.

## Eigen woordankers hergebruiken met kopieerbare tijdcodetekst

Bij **Handmatige woordankers** kun je de huidige set nu als gewone
selecteerbare tekst bekijken. Gebruik **Kopieer woordankers** om de
tijdcodetekst met één expliciete tik naar het Android-klembord te sturen.
Met **Plak woordankers uit klembord…** lees je klembordtekst pas na een
aparte gebruikersactie in; de invoer wordt op 512 tekens begrensd.
De bestaande tekstselectie blijft ook beschikbaar. Bijvoorbeeld:
`2=00:00:12.250;5=00:00:13.900`. Dit bevat uitsluitend eigen
woordnummers en de handmatig aangebrachte **absolute** tijdcodes:
geen audio, transcriptietekst, account of verborgen woordenkaartdata.

Wil je een eerder bewaarde eigen reeks opnieuw gebruiken, plak deze in
**Woord=uu:mm:ss.mmm of Woord=+uu:mm:ss.mmm** bij **de juiste actieve ondertitel**. Scheid
paren met een puntkomma of regeleinde. Er mogen maximaal **16
verschillende oorspronkelijke woordnummers** (2 t/m het werkelijke
aantal van die cue) staan; de totale invoer is maximaal 512 tekens.
Elke tijdcode gebruikt `uu:mm:ss.mmm` met hele milliseconden. Alle
posities moeten **binnen de actieve cue** passen en samen met het
begin/einde ervan een strikt oplopende reeks vormen. Te grote paste
wist bewust eerder geldige invoer, zodat die niet alsnog als voorstel
kan worden toegepast. Onjuiste, dubbele of onmogelijke invoer wordt
integraal geweigerd, zonder gedeeltelijke ankers te plaatsen.

Tuddy toont de volledige **voorgestelde set** voordat je op
**Bevestig en vervang ankers van deze ondertitel** tikt. De knop
vervangt alleen de tijdelijke ankers van die ene cue; bestaande
andere cues en hun originele SRT/VTT/geluid blijven ongemoeid.
Wisselen van ondertitel of audio en het sluiten van de speler
wist de tijdelijke invoer en actieve sessie-ankers. Er is geen
automatische permanente opslag of netwerkverzoek. Wie de tekst via
een Android-klembordapp kopieert, moet er rekening mee houden dat
die app zelf gegevens kan synchroniseren.

Deze tijden zijn volledig door de gebruiker gekozen en vormen
**geen gemeten audio-uitlijning**. Herhalen, direct springen en de
meeloop-highlight gebruiken na bevestiging dezelfde al bestaande
tijdinterpolatie tussen handmatige ankers.

---

# Zelf handmatig woordtijdankers plaatsen (optioneel)

## Expliciete woordgrenzen zonder automatische segmentatie

De woordvolger volgt nu zowel bestaande Unicode-witruimte als expliciete
zero-width spaces (`U+200B`) in de aangeleverde ondertiteltekst. Dit helpt
bij tekst waarin zulke scheidingstekens al aanwezig zijn. Tuddy verzint
geen woordgrenzen: een ongesegmenteerde zin blijft één token en levert
geen kunstmatig opgesplitste woordstappen op.

---

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
Kies **Bekijk of bewerk eigen woordtijdmarkeringen** om per nummer de
**exacte uu:mm:ss.mmm-markering** te inspecteren, het nummer opnieuw voor
een nieuwe audiotijd in te vullen (**Bewerk**) of alleen dat ene anker
te verwijderen (**Wis**). Met de knop **Vul huidig geschat woordnummer in**
zet Tuddy enkel een voorstel in het nummerinvoerveld; er wordt niets
zonder jouw tweede expliciete tik gemarkeerd.

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
