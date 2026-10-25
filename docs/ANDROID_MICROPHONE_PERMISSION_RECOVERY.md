# Android-microfoontoegang bewust herstellen

## Toegang geweigerd

Tuddy gebruikt de Android-microfoon alleen voor een expliciet gestarte
**on-device** spraakherkenningssessie. Voor tekst-only vertaling,
handmatig ingevoerde zinnen en het lezen van bestaande RAM-only
ondertitels is `RECORD_AUDIO` niet nodig.

In het gewone gesprek én de fullscreen leesmodus loopt **Luister**
langs dezelfde microfooncontrole. Wanneer Android toegang weigert,
blijft de microfoon uit en toont Tuddy een herstelpaneel:

- **Vraag microfoontoegang opnieuw** wordt alleen aangeboden wanneer
  Android aangeeft dat de permissie nogmaals kan worden gevraagd.
- **Open Android-appinstellingen** opent uitsluitend Androids pagina
  voor de Tuddy-app. Tuddy zet de toestemming niet zelf aan en vraagt
  geen verhoogde rechten.
- Als Android de pagina niet kan openen, geeft Tuddy de handmatige
  navigatie aan: Instellingen → Apps → Tuddy → Machtigingen → Microfoon.

Na het openen van appinstellingen beëindigt Tuddy eerst eventuele lokale
spraak/TTS en geplande automatische herstarts. Zodra de Activity weer
**RESUMED** is, leest Tuddy de huidige Android-microfoontoestemming
opnieuw. Het paneel meldt of de toestemming daadwerkelijk aanwezig is.

**Belangrijk:** een terugkeer uit Android-instellingen, het afsluiten van
een toestemmingsdialoog of het terugkeren naar de fullscreen weergave
start **nooit automatisch een nieuwe microfoonsessie**. Tik daarvoor
opnieuw bewust op **Luister**; daarna blijven alle bestaande
permissie-, offline spraakmodel-, strikte vliegtuigmodus-, batterij-
en levenscyclusgates ongewijzigd.

De keuze dat een permissieverzoek werd geweigerd en de herstelmelding
zijn tijdelijke UI-status in RAM; er wordt geen audio, transcript,
permissiegeschiedenis of persoonsgegevens aan de server geschreven.

## Grenzen van deze mijlpaal

De wijziging maakt het mogelijk om een geweigerde permissie correct
via Android te herstellen. Ze bewijst niet dat spraakmodellen op een
fysiek Android 12+-toestel werken of dat echte netwerkisolatie, NL→EN /
EN→NL kwaliteit, Kurmanji/Sorani/Pasjtoe-modelrechten, APK-installatie,
ondertekening of productie-release zijn geaccepteerd. Die onafhankelijke
gates blijven open.
