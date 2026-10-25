# Eén privé-klembordroute voor alle Tuddy-schermen

## Dezelfde grens, ongeacht het scherm

Tuddy laat pas na een expliciete tik tekst het Android-klembord in gaan.
De **instelbare best-effort klembordvervaltijd** (uit / 1 minuut / 5 minuten)
en de Android-markering voor gevoelige inhoud werken via
`PrivateClipboardExpiry.copy` nu ook voor de oudere Compose-kopieeracties:

- **Live fullscreen leesmodus**: één afgeronde bronzin met lokale vertaling,
  zowel in het tijdelijke gesprek als wanneer een gebruiker een
  privésessiearchief bekijkt.
- **Fullscreen gesprekskaart**: bewust één afgeronde tweetalige caption.
- **Taalondersteuning**: een gefilterd overzicht van openbare
  modelonderzoekmetadata (nooit automatisch een gesprek of audio).
- **Privé-ondertitelbewerking**: het door de gebruiker opgestelde
  correctiescript, met dezelfde bestaande handmatige plakstap.
- **Audio-/ondertitelwoorden**: absolute en relatieve ankers voor het
  handmatig uitlijnen van ondertitels. De bestaande in-app-plakoptie blijft.

De al bestaande snelle Android-tekstvertaler gebruikt dezelfde centrale
kopieerroutine. Hierdoor zijn de gevoeligheidsmarkering, eventuele
vervaltijd, de maximale kopieergrootte en foutmeldingen consistent.
Kopieeracties geven nu alleen *gelukt* terug als Android het klembord
daadwerkelijk heeft geaccepteerd. Lukt het niet, dan meldt Tuddy
waarheidsgetrouw dat de inhoud niet gekopieerd is.

## Wat de vervaltijd wél en niet doet

Een kopie blijft bewust toegankelijk voor andere apps. Tuddy kan een
klembord-item alleen later proberen te wissen als hetzelfde, onveranderde
Tuddy-item nog op het systeemklembord staat en Android de app daar op
dat moment toegang toe geeft. Een clipboardmanager, synchronisatiedienst
of andere app kan de inhoud al direct hebben opgeslagen. De Android-
markering voorkomt dit **niet gegarandeerd**. Als je later andere tekst
kopieert, wist Tuddy die nieuwe tekst niet met een oude timeout.

**De vervaltijd is standaard uit** en kan in Tuddy bewust op één of
vijf minuten worden gezet. Zonder die opt-in wordt tekst niet
automatisch gewist. De functie stuurt niets naar internet, bewaart geen
tekst in de preferences en maakt geen achtergrondservice of
netwerkactie aan.

Bij langdurige taken zoals een correctiescript kan eerder gekopieerde
tekst na de ingestelde tijd verdwijnen vóórdat je plakt. Gebruik dan
bewust opnieuw **Kopieer** of schakel de vervaltijd uit. Plakken zelf
blijft een aparte actie met de bestaande grenzen en bevestiging.

Dit is source-integratie. Een groene Android-build is geen bewijs van
fysieke isolatie, OS-versie-specifieke klembordtoegang, daadwerkelijke
installatie op Android 12+ of rechtmatig gereleasete taalmodellen.
