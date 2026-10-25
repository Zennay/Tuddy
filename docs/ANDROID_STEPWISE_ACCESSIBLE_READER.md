# Eén zin tegelijk lezen (Android fullscreen)

In de bestaande fullscreen **Tuddy · Leesmodus** kun je nu bewust kiezen
voor **Lees één zin tegelijk**. De schakelaar geldt uitsluitend voor de
huidige weergave; teksten, selectie en voorkeursmodus worden niet
opgeslagen of buiten Tuddy verstuurd.

## Gesprek rustig doorlopen

- Bij het openen van de één-zin-modus wordt de **laatste momenteel
  zichtbare** gespreksregel geselecteerd.
- De knoppen **← Vorige zin** en **Volgende zin →** lopen door de
  gefilterde regels. De teller toont de positie en het aantal zichtbare
  regels. Een wijziging van zoekterm, spreker- of markeringsfilter
  selecteert opnieuw de laatste regel die aan dat filter voldoet.
- **Nieuwe live spraak** of vertalingen veranderen de gekozen regel
  niet automatisch. De app zet in deze modus Volg live uit; er is geen
  automatische geluidsuitvoer en geen ongevraagde verplaatsing van
  schermlezerfocus. De bestaande microfoon- en privacygrenzen blijven
  ongewijzigd.
- De geselecteerde zin laat een apart kopje **Origineel · taal** en
  **Vertaling · taal** zien. Het zijn gewone tekstblokken met een
  Android-toegankelijke kopstructuur, ook op smalle telefoons. Er zijn
  geen in de tekst verweven woordkaartknoppen of visuele
  laatste-woordmarkeringen: gebruikers hoeven niet tientallen
  losse woorden door te navigeren. De reeds bestaande kopieer-,
  correctie-, spreker- en markeringsknoppen blijven beschikbaar.
- Een nog niet afgeronde vertaling toont nadrukkelijk de huidige
  status en geen gefingeerd vertaalresultaat. De voorlopige microfoonzinnen
  worden niet in deze rustige weergave geschoven.
- **Toon alle gespreksregels** gaat terug naar het gewone volledige
  transcript, inclusief de bestaande live-weergave en woordkaarten.
  Archiefleesmodus gebruikt dezelfde lokale navigatie, zonder dat het
  archief in het actieve gesprek wordt geïmporteerd.

Dit is een implementatiemijlpaal voor Android leesbaarheid en
bedieningszekerheid, **geen** bewijs van een menselijke TalkBack-
gebruikersproef of de volledige research-/screenshots-/metingen-
acceptatie van issue #464. De fysieke Android 12+ vliegtuigmodus-
acceptatie (#1), volledige rechten- en native-speaker-gecontroleerde
Kurmanji/Sorani/Pasjtoe-ondersteuning (#2) en een ondertekende
productierelease blijven afzonderlijke gates.
