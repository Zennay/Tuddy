# Privé getypte overhoorantwoorden op Android

## Wat er is veranderd

De tweetalige zelfoverhoorfunctie oefent met zinnen uit het **bestaande,
tijdelijke gesprek**. De cursist kan zelf de vertaling typen en pas
daarna bewust de bestaande referentiezin onthullen. Het antwoord wordt
niet automatisch beoordeeld, naar ML Kit gestuurd of opgeslagen.

Het invoerveld maakt nu gebruik van Androids **native EditText** met
de volgende beperkingen:

- Maximaal **400 tekens**; regeleinden en besturingskarakters worden
  vóór opname in het tekstbuffer vervangen of geweigerd.
- Het veld vraagt Android om **geen autocorrecties of tekstsuggesties**
  en stuurt **IME_FLAG_NO_PERSONALIZED_LEARNING** naar het gekozen
  toetsenbord.
- Android-autofill en automatische content-capture worden expliciet
  geweigerd voor dit invoerveld.
- De tekst wordt alleen tijdelijk in de actieve schermstatus bijgehouden,
  niet in het bestandssysteem, niet in de oefenlijst en niet in
  Androids klembord gezet.
- Bij **Pauze/Achtergrond**, sluiten of volgende vraag wordt een nog
  niet bewaard getypt antwoord gewist. Een handmatig onthuld antwoord
  wordt bij verlaten van de voorgrond opnieuw verborgen.
- Je kunt nog steeds normaal typen en met Androids eigen toetsenbord
  bewust tekst plakken. Het veld voert geen spraakdictatie namens de
  gebruiker uit.

## Grenzen

De IME-flags zijn **verzoeken** van Tuddy aan het op het toestel
geïnstalleerde toetsenbord. Het besturingssysteem en de IME-leverancier
kunnen eigen tekstverwerking, netwerkgebruik of beleid hebben. Tuddy
kan en mag geen garantie geven dat een derde toetsenbord nooit
persoonlijke woorden of gebruiksgegevens verwerkt. Voor gevoelige
gegevens blijft een vertrouwd offline toetsenbord verstandig.

De bestaande fysieke Android 12+ vliegtuigmodus-acceptatie, lokale
spraak-/vertaalmodellen, rechten- en native-speakerreview, ondertekende
APK en productietoelating zijn volledig onafhankelijke eisen. Dit is
geen claim dat die stappen zijn uitgevoerd.
