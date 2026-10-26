# Herstel van vastgelopen offline tekstmodelvoorbereiding

Deze flow geldt voor het **tweeweg-tekstgesprek** en het dashboard
**Mijn favoriete tekstrichtingen**. Het is volledig onafhankelijk van
Androids on-device spraakherkenning en maakt geen nieuwe taal beschikbaar.

## Alleen lezen: uiterlijk 12 seconden op UI-niveau

Voor ieder gekozen teksttaalpaar vraagt Tuddy ML Kit welke modellen op
het toestel geïnstalleerd zijn. Komt er geen antwoord, dan laat de app de
controle niet voor onbepaalde tijd op *Controleren…* staan. Na maximaal
12 seconden wordt het resultaat **onbekend**, niet *lokaal beschikbaar*.
De gebruiker kan daarna **Controleer modellen** of **Controleer alle
favorieten opnieuw** aanraken om zelf een nieuwe read-only controle te
starten. Oude resultaten krijgen geen toegang meer tot de huidige
gekozen taalrichting.

## Via wifi: bewuste toestemming en eerlijk afronden

Alleen een expliciete tik op **Modellen via wifi…** of de bevestigde
voorbereiding van favoriete paren mag een ML Kit-modeldownload starten.

- Eén actieve, eerder geautoriseerde ML Kit-download per app-proces.
  De tijdelijke blokkade loopt pas af wanneer **Androids echte Task**
  een terminale callback oplevert, niet wanneer het scherm sluit of
  de gebruiker een timeoutmelding ziet. Dit voorkomt nieuwe
  overlappende downloads die netwerkverkeer of opslagruimte verbruiken.
- Duurt de voorbereiding langer dan **twee minuten**, dan stopt
  uitsluitend het wachten in de gebruikersinterface. De melding zegt
  dat de werkelijke download nog kan lopen. Er wordt geen *geslaagd*
  resultaat verzonnen; opnieuw **modellen controleren** is read-only.
- Na een door ML Kit gemelde succesvolle download kijkt Tuddy opnieuw
  naar de daadwerkelijk geïnstalleerde modellen. Alleen een nieuwe
  `READY`-ontvangst toont **lokaal beschikbaar**.
- Sluiten of wijzigen van het teksttaalpaar sluit de lopende
  UI-observatie en voorkomt verouderde statusovername. Reeds door
  Android geaccepteerd downloaden kan echter doorgaan.
- Bij favoriete paren stopt de sequentie op de eerste onbekende of
  mislukte download in plaats van overige paren zonder toestemming
  of statusbewijs door te lopen.

Een app-proces waarin de Android-download nooit meer terugmeldt,
blokkeert conservatief nieuwe modeldownloads totdat het proces
opnieuw wordt gestart. Dat is een veiligheidsbeperking, geen bewijs
dat er daadwerkelijk nog netwerkactiviteit is.

De tekst van een privégesprek wordt niet aan de modeldownloader
meegegeven of op schijf gezet. Tekstmodellen via wifi voorbereiden
geeft geen toestemming voor nieuwe spraakmodellen, cloudvertaling,
Kurmanji/Sorani/Pasjtoe-checkpoints of een productie-release.

## Acceptatiegrenzen

De bestaande Android-GitHub-build en branch-checks bevestigen hooguit
dat de code bouwbaar is. Een fysieke Android 12+-vliegtuigmodusproef,
werkelijke NL→EN/EN→NL offline herkenning én vertaling, geïnstalleerde
APK-identiteit en onafhankelijke review blijven verplicht. Modelrechten,
native-speaker-evaluatie en ondertekende productie-admission staan los
van deze UI-herstelvoorziening.
