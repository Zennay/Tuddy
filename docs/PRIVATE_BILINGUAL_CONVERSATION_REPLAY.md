# Tweetalige luisteroefening binnen het privégesprek

Tuddy heeft een expliciet bediende **tweetalige replay** van de laatste
maximaal acht lokaal vertaalde zinnen in het actuele tijdelijke gesprek.
Dit is een luisteroefening, geen automatische herkenning, kwaliteitscheck,
stembeoordeling of export.

## Gebruiken

1. Beëindig de microfoonsessie. Open **Gesprekstools**.
2. Tuddy neemt de taalrichting van de **meest recente afgeronde, lokaal vertaalde**
   zin. Andere eerdere taalrichtingen worden niet ongemerkt gecombineerd.
3. Het paneel toont hoeveel bestaande, *voltooide* lokaal vertaalde zinnen
   in diezelfde taalrichting veilig voorgelezen kunnen worden, maximaal acht.
   Handmatige vertaalvoorstellen, verlopen taken, ontbrekende modellen,
   onzekere foutstatussen of regels uit een andere taalrichting worden
   niet automatisch in deze oefening meegenomen.
4. Kies expliciet een maximum van **1, 3, 5 of 8** zinnen. Zonder
   verdere keuze neemt Tuddy zoals voorheen de laatste afgeronde zinnen.
   Via **Kies startzin** kun je in hetzelfde actuele gesprek ook
   **vanaf een specifieke van de acht beschikbare zinnen** oefenen.
   Wanneer er vanaf die zin minder dan het gekozen aantal overblijven,
   oefent Tuddy alleen die bestaande zinnen: er wordt niets herhaald,
   opgevuld of uit een andere taalrichting gehaald. **Automatisch ·
   laatste zinnen** herstelt de standaardselectie.
5. Kies daarnaast **Origineel eerst** of **Vertaling eerst**. De tweede
   optie is een oefening in actieve herinnering: de al aanwezige lokale
   vertaling wordt vóór het originele antwoord voorgelezen. Beide
   volgordes vereisen dezelfde geïnstalleerde offline stem voor *beide*
   talen; bij het wisselen van volgorde stopt lopende playback.
6. Tik op **▶ Oefen zinnen** of **▶ Rustig**. Tuddy controleert **beide**
   Android-stemmen voordat enige private tekst wordt voorgelezen.
   Er wordt niets gedownload, opnieuw vertaald of online verzonden.
   Het gekozen bereik verschijnt vooraf zichtbaar in het paneel.
7. De geselecteerde paren blijven in hun oorspronkelijke
   gespreksvolgorde. Binnen elk paar wordt de gekozen taalvolgorde
   toegepast: de bronzin met de bronstem en de vertaling met de
   doelstem, ook als **Vertaling eerst** actief is. Na elk paar
   verschijnt de voortgang. Bij volledige afronding of **■ Stop**
   sluit de app de tijdelijke TTS-engine en geeft de audiofocus vrij.
8. De replay en de overige voorleesknoppen delen vanaf nu exact **één
   lokale TTS-engine per hoofdscherm**. Daardoor stopt een microfoonstart
   de replay **vóór** Android begint met luisteren: de microfoon hoort
   niet per ongeluk zijn eigen synthetic speech. Andere expliciete
   voorleesacties vervangen lopende playback in plaats van beide tegelijk
   te spreken. Wanneer Gesprekstools worden verborgen, of het gesprek
   wordt afgedekt of de app op de achtergrond gaat, wordt playback
   eveneens gestopt.

## Veiligheidsgrenzen

De oefening behoudt alleen een tijdelijke snapshot van bestaande
gespreksregels in RAM. Startzin, aantal en volgorde worden alleen in
het huidige scherm bijgehouden; niets daarvan gaat naar een bestand. Het maximum is acht tweetalige paren (zestien
uitgesproken segmenten), elk maximaal 400 veilig leesbare tekens.
Er zijn geen audiobestanden, geschiedenis, analytics of voorkeurswijzigingen.
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
