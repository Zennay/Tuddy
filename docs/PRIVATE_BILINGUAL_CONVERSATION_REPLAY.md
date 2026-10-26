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
4. Kies expliciet **1, 3, 5 of 8** meest recente zinnen. Er worden
   nooit meer regels geselecteerd dan werkelijk afgerond en beschikbaar
   zijn. De keuze bestaat alleen zolang dit paneel open is. Een gewijzigde
   keuze stopt eventueel lopend voorlezen.
5. Tik op **▶ Oefen zinnen** of **▶ Rustig**. Tuddy controleert **beide**
   Android-stemmen voordat enige private tekst wordt voorgelezen.
   Er wordt niets gedownload, opnieuw vertaald of online verzonden.
6. Elke regel volgt in oorspronkelijke gespreksvolgorde: eerst het
   origineel, vervolgens de bestaande lokale vertaling. Na elk paar
   verschijnt de voortgang. Bij volledige afronding of **■ Stop**
   sluit de app de tijdelijke TTS-engine en geeft de audiofocus vrij.
7. De replay en de overige voorleesknoppen delen vanaf nu exact **één
   lokale TTS-engine per hoofdscherm**. Daardoor stopt een microfoonstart
   de replay **vóór** Android begint met luisteren: de microfoon hoort
   niet per ongeluk zijn eigen synthetic speech. Andere expliciete
   voorleesacties vervangen lopende playback in plaats van beide tegelijk
   te spreken. Wanneer Gesprekstools worden verborgen, of het gesprek
   wordt afgedekt of de app op de achtergrond gaat, wordt playback
   eveneens gestopt.

## Veiligheidsgrenzen

De oefening behoudt alleen een tijdelijke snapshot van bestaande
gespreksregels in RAM. Het maximum is acht tweetalige paren (zestien
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
