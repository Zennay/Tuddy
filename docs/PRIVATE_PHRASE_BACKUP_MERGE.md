# Eigen offline zinnen: back-up samenvoegen of vervangen

In **Mijn eigen Kurmanji/Sorani/Pasjtoe zinnen → Herstel back-up…**
kiest de gebruiker een eerder opgeslagen JSON-bestand. Tuddy leest het
bestand begrensd via de Android-bestandskiezer en weigert een ongeldige
versie, foutieve UTF-8, dubbele identiteiten, onbekende talen of te
grote payload voordat er iets aan de privélijst verandert.

Na een geldige selectie toont het scherm hoeveel zinnen nieuw zijn en
hoeveel al in de bestaande privébibliotheek voorkomen. De gebruiker
kiest **expliciet** een van twee aparte acties:

- **Voeg alleen nieuwe zinnen toe**: voegt uitsluitend combinaties
  taal+zin toe die nog niet lokaal aanwezig zijn. Bestaande zinnen én
  hun door de gebruiker geschreven betekenissen blijven onaangeroerd,
  ook als de back-up voor die zin een andere betekenis bevat. De totale
  lijst mag nooit meer dan 80 items bevatten.
- **Vervang volledige lijst**: de bestaande, al aanwezige optie. Na de
  expliciete bevestiging wordt de oude lijst vervangen door het
  geselecteerde bestand. Dit kan bestaande zinnen verwijderen.

Samenvoegen wordt met één app-private opslagtransactie uitgevoerd.
Als het bestand of de huidige opslag ongeldig is, of de samenvoeging
boven de maximale capaciteit komt, blijft de oorspronkelijke lijst
ongewijzigd. Annuleren schrijft niets.

De import neemt geen gesprek, audio, transcript, spreektaalmodel,
oefenscore of providerdata over. Eigen zinnen zijn géén door Tuddy
gevalideerde vertalingen. De gekozen externe documentprovider kan
back-upbestanden synchroniseren; Tuddy uploadt ze niet zelf. Een
geslaagde broncode-integratie vormt geen acceptatiebewijs voor fysieke
Android-toestellen of een productie-APK.
