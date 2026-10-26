# Bewuste terugvertaalvergelijking per lokale gesprekszin

## Gebruik (Incubate — Android)

Een afgeronde lokaal vertaalde gesprekszin (bijvoorbeeld NL→EN) kan een
**⇄ Vergelijk via lokale terugvertaling…**-knop tonen. De gebruiker:

1. Tikt op de knop wanneer de microfoon niet luistert; Tuddy bewaart dan
   uitsluitend die exacte, nog ongewijzigde regel tijdelijk voor het dialoog.
2. Ziet de oorspronkelijke zin en de huidige vertaling naast elkaar.
3. Tikt uitdrukkelijk op **Vergelijk lokaal**. Pas dan controleert Tuddy
   **read-only** of de omgekeerde lokale ML Kit-tekstmodellen al op het
   toestel staan. Bij ontbrekende of onbekende modellen volgt een eerlijk
   bericht en wordt **geen** model of externe dienst opgeroepen.
4. Bij bevestigde modellen reserveert Tuddy hoogstens één vrije plaats
   binnen het bestaande procesbrede ML Kit-budget van maximaal drie
   fysieke vertaaltaken, vertaalt de huidige vertaling terug naar de
   oorspronkelijke taal en toont de nieuwe zin alleen in het dialoog.
5. Vergelijkt zelf de formulering. De functie geeft **geen correctheidsscore**,
   slaat de terugvertaling niet op en overschrijft de bestaande regel
   of handmatige correcties niet.

## Privacy en levenscyclus

De oorspronkelijke zin en vertaling blijven in het bestaande tijdelijke
RAM-gesprek. Het dialoog bewaart alleen de ene vergelijking totdat het
sluit, de regel verandert/verwijderd wordt, of de Activity stopt. De
controller start geen downloads, HTTP-verzoeken, cloudspraak, camera,
microfoon of extra Android-permissies.

Het totale controle-/vertalingstraject heeft een grens van 30 seconden,
inclusief de geïnstalleerde-modelinventaris. Na een verlopen deadline
of gesloten scherm kan een late provideruitkomst niet worden getoond.
Een reeds **ingediende ML Kit-taak** kan intern blijven doorlopen; zijn
fysieke modelsleuf blijft dan bezet totdat ML Kit daadwerkelijk
terugmeldt, ook wanneer het scherm inmiddels gesloten is. Dit
voorkomt dat een afgebroken dialoog onbeperkt nieuwe modeltaken
kan stapelen. Bij voltooiing wordt de translator afgesloten en zijn
modelplaats vrijgegeven.

De functie accepteert uitsluitend veilige, korte (max. 1.000 tekens),
afgeronde vertaalregels met twee verschillende daadwerkelijk ondersteunde
ML Kit-talen. Dit voorkomt onbedoelde hervertaling van concepten,
foutmeldingen, onbetrouwbare lange modeloutput of hetzelfde-taal-echo's.

## Grenzen van de vergelijking

**Terugvertalen is geen onafhankelijk kwaliteitsbewijs.** Twee
modellen kunnen elkaars fouten herhalen of extra formuleringen
introduceren. Een terugvertaalde zin kan afwijken terwijl de eerste
vertaling wél correct is. Gebruikers- of native-speaker-review blijft
nodig voor betekenis en veiligheid.

Dit is uitsluitend app-core-integratiewerk. Geen Android 12+
vliegtuigmodustest, geen bewijs van de geïnstalleerde APK, geen
rechtenvrijgave voor volledige Kurmanji/Sorani/Pasjtoe ASR+MT en
geen ondertekende productie-release.
