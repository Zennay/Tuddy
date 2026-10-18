# Afdrukbaar tweetalig gespreksverslag

Tuddy heeft naast TXT, JSON en ondertitelconcepten nu ook een
**leesbaar, zelfstandig HTML-gespreksverslag** voor afdrukken of
offline lezen. Er zijn twee expliciete bronnen:

- **Huidig tijdelijk gesprek** via **Gesprekstools →
  Afdrukbaar gesprek (HTML)…** (alleen met microfoon uit);
- **Eigen JSON-archief** na een afzonderlijke, door de gebruiker
  gekozen read-only archiefimport en **Bewaar leesbaar afdrukbaar
  privéarchief (HTML)…**.

Een toestemmingsvenster toont de privacygevolgen en biedt keuze
tussen **alleen afgeronde zinnen** of **ook oorspronkelijke zinnen
zonder afgeronde vertaling**. Pas na bevestiging laat de Android
Storage Access Framework-bestandskiezer de gebruiker zelf
een opslaglocatie kiezen. Andere bestandsproviders kunnen
gegevens in de cloud bewaren, maar Tuddy synchroniseert zelf niet.

Het HTML-document bevat:

- Per gespreksregel oorspronkelijke tekst, eventuele werkelijk
  lokaal voltooide vertaling, taalrichting en een historische
  datum-/tijdweergave;
- Grote leesbare originele en vertaalde tekst, met
  rechts-naar-links-opmaak voor de relevante schriftsystemen;
- Duidelijke melding dat tekstvertalingen niet onafhankelijk
  gecontroleerd zijn en onvoltooide vertalingen ontbreken;
- Een compact, printvriendelijk ontwerp zonder scripts,
  externe lettertypen, links of netwerkresources.

Invoer maximaal 500 regels en uitvoer maximaal 256 KiB. Velden
worden gevalideerd en HTML-tekens veilig ge-escaped. De oorspronkelijke
getypte/gesproken zin blijft behouden, maar fouten van onvoltooide
vertalers worden **niet** als vertaalinhoud afgedrukt. Het live
gesprek wordt vlak voor schrijven opnieuw beoordeeld; bij
tussentijdse wijziging wordt de oude export geweigerd. Bij het
archief kan de bewust gekozen exportsnapshot de externe
Android-documentkiezer overleven zonder het archief terug
in de live tijdlijn te laden.

Geen automatische transcriptopslag, opname, gedetecteerde identiteit,
taallicentie- of offline toestelacceptatieclaim. Fysieke Android-
acceptatie, native-speakerreview en signing blijven onafhankelijke
productiegates.
