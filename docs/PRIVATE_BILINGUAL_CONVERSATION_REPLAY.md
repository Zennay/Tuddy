# Tweetalige luisteroefening binnen het privégesprek

Tuddy heeft een expliciet bediende **tweetalige replay** van de laatste
maximaal acht lokaal vertaalde zinnen in het actuele tijdelijke gesprek.
Dit is een luisteroefening, geen automatische herkenning, kwaliteitscheck,
stembeoordeling of export.

## Gebruiken

1. Beëindig de microfoonsessie. Open **Gesprekstools**.
2. Tuddy selecteert standaard de taalrichting van de **meest recente afgeronde,
   lokaal vertaalde zin**. Tik op **Kies taalrichting** om ook oudere richtingen
   te kiezen die in dit gemengde privégesprek voorkomen. Het menu toont maximaal
   zestien richtingen, met per richting de hoeveelheid reeds veilig afspeelbare
   tweetalige zinnen (maximaal acht). De nieuwste beschikbare richting staat
   bovenaan. Als een recent taalpaar geen veilige voorleesbare regels heeft,
   verschijnt het niet in het menu.
3. Na een bewuste keuze beperkt Tuddy de hele luisteroefening tot die
   taalrichting. Een nieuw gespreksonderdeel in een andere taal verandert
   die bewuste keuze niet. Als alle regels van het gekozen paar verdwijnen,
   valt Tuddy terug op de actuele, wel beschikbare richting. Een richting
   wijzigen stopt lopende spraak en begint een nieuwe oefenkeuze; het verandert
   nooit de microfoontaal, de ondertitels of geïnstalleerde modellen.
   Handmatige vertaalvoorstellen, verlopen taken, ontbrekende modellen
   en foutstatussen worden niet automatisch meegenomen.
4. Kies expliciet een maximum van **1, 3, 5 of 8** zinnen. Zonder
   verdere keuze neemt Tuddy zoals voorheen de laatste afgeronde zinnen.
   Via **Kies startzin** kun je in hetzelfde actuele gesprek ook
   **vanaf een specifieke van de acht beschikbare zinnen** oefenen.
   Bij een andere taalrichting begint ook de startzinselectie opnieuw.
   Wanneer er vanaf die zin minder dan het gekozen aantal overblijven,
   oefent Tuddy alleen die bestaande zinnen: er wordt niets herhaald,
   opgevuld of uit een andere taalrichting gehaald. **Automatisch ·
   laatste zinnen** herstelt de standaardselectie.
5. Kies daarnaast **Origineel eerst** of **Vertaling eerst**. De tweede
   optie is een oefening in actieve herinnering: de al aanwezige lokale
   vertaling wordt vóór het originele antwoord voorgelezen. Beide
   volgordes vereisen dezelfde geïnstalleerde offline stem voor *beide*
   talen; bij het wisselen van volgorde stopt lopende playback.
6. Kies desgewenst **Direct**, **2,5 s** of **5 s** nadenkpauze.
   **Direct** behoudt het oude afspelen zonder tussenpauze. Met de twee
   andere keuzes wacht Tuddy na de *eerste* uitgesproken taal van elk
   paar, zodat je zelf de andere taal kunt proberen te zeggen voordat
   Android het antwoord voorleest. Tussen twee volledige paren wacht
   Tuddy korter (respectievelijk 0,75 of 1,5 seconde). De volgorde
   **Origineel eerst** of **Vertaling eerst** blijft intact.
   De pauze-instelling is tijdelijk in RAM en verandert geen
   systeemsnelheid of telefoonvoorkeur.
7. Tik op **▶ Oefen zinnen** of **▶ Rustig**. Tuddy controleert **beide**
   Android-stemmen voordat enige private tekst wordt voorgelezen.
   Er wordt niets gedownload, opnieuw vertaald of online verzonden.
   Het gekozen bereik verschijnt vooraf zichtbaar in het paneel.
8. De geselecteerde paren blijven in hun oorspronkelijke
   gespreksvolgorde. Binnen elk paar wordt de gekozen taalvolgorde
   toegepast: de bronzin met de bronstem en de vertaling met de
   doelstem, ook als **Vertaling eerst** actief is. Na elk paar
   verschijnt de voortgang. Bij volledige afronding of **■ Stop**
   sluit de app de tijdelijke TTS-engine en geeft de audiofocus vrij.
9. De replay en de overige voorleesknoppen delen vanaf nu exact **één
   lokale TTS-engine per hoofdscherm**. Daardoor stopt een microfoonstart
   de replay **vóór** Android begint met luisteren: de microfoon hoort
   niet per ongeluk zijn eigen synthetic speech. Andere expliciete
   voorleesacties vervangen lopende playback in plaats van beide tegelijk
   te spreken. Wanneer Gesprekstools worden verborgen, of het gesprek
   wordt afgedekt of de app op de achtergrond gaat, wordt playback
   eveneens gestopt.

## Veiligheidsgrenzen

De oefening behoudt alleen een tijdelijke snapshot van bestaande
gespreksregels in RAM. Taalrichting, startzin, aantal en volgorde worden alleen in
het huidige scherm bijgehouden; niets daarvan gaat naar een bestand. Het maximum is acht tweetalige paren (zestien
uitgesproken segmenten), elk maximaal 400 veilig leesbare tekens.
Er zijn geen audiobestanden, geschiedenis, analytics of voorkeurswijzigingen.
De geplande pauze maakt geen opnames en houdt geen audit- of oefentijdlijn
bij. Tijdens de pauze houdt Tuddy de al aangevraagde tijdelijke audiofocus
vast; Android-mediabediening, Stop, een nieuwe voorleesactie,
het afsluiten van de app of achtergrondgang beëindigt die sessie en
annuleert de uitgestelde volgende zin. Een Android-fout bij het plannen
van zo'n korte pauze beëindigt de oefening zonder stilzwijgend
extra audio te starten.

De Android-spraakdienst moet voor beide talen een geïnstalleerde,
niet-netwerkafhankelijke stem melden. Ontbreekt één stem, dan wordt
**geen enkele** zin afgespeeld. Het apparaat kan een oproep of andere
audio-app prioriteit geven; Tuddy stopt dan, evenals bij gemeld
headsetverlies. Android-spraakcallbacks worden per exacte utterance-ID
verwerkt en kunnen de volgende regel niet tweemaal activeren.

De engine is eigendom van het hoofdscherm en wordt pas bij de
Activity-lifecycle volledig gesloten; de verborgen replay stopt de
audio maar mag die gedeelde engine niet permanent sluiten. De app stopt
voorlezen bij achtergrond/Stop en bewaart geen voorgelezen tekst buiten
het bestaande privégesprek. Een stem die Android als
offline meldt is geen onafhankelijke garantie voor radiostilte.
De fysieke Android 12+-acceptatie van NL→EN en EN→NL, rechten voor
volledige Kurmanji/Sorani/Pasjtoe-modellen, native-speakercontrole en
een getekende release blijven afzonderlijke gates.
