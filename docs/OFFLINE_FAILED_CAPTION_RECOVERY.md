# Mislukte offline vertalingen herstellen

## Waarom

Een lokale Android-spraakherkenning kan een tijdelijke caption opleveren
terwijl het ML Kit-tekstmodel voor die exacte taalrichting nog ontbreekt of
vertaling mislukt. Tuddy bewaart dan de oorspronkelijke regel in het
tijdelijke gesprek, met status **Model ontbreekt** of **Fout**. De gebruiker
hoeft die oorspronkelijke zin niet opnieuw in te spreken.

## Herstel vanuit het gewone gesprek

1. De knop **Herstel N mislukte offline vertalingen…** verschijnt onder de
   gesprekshulpmiddelen als het actuele tijdelijke gesprek herstelbare fouten
   bevat. Hij is uitgeschakeld tijdens luisteren of modelbeheer.
2. Het paneel groepeert uitsluitend **Model ontbreekt** en **Fout** naar de
   *oorspronkelijke* bron- en doeltaalcodes van iedere regel; het actuele
   microfoontaalpaar hoeft dus niet hetzelfde te zijn.
3. Tik bewust bij één taalrichting op **Controleer lokaal en herstel maximaal
   6**. Er volgt eerst één **read-only** controle van reeds op het toestel
   geïnstalleerde ML Kit-tekstmodellen. Ontbreekt het model of is de status
   onbekend, dan wordt geen regel aangeraakt en opent Tuddy geen download.
   Voor bewuste voorbereiding via wifi bestaat **Offline startcheck**.
4. Na bevestigde beschikbaarheid worden hoogstens **zes ongewijzigde
   mislukte regels** van die taalrichting in hun bestaande tijdelijke
   geschiedenis opnieuw aangeboden aan dezelfde lokale vertaalpipeline.
   Bewerkte tekst, andere taalrichting, verwijderde regels, geslaagde
   vertalingen en handmatig gecorrigeerde regels worden niet vervangen.
   Bij een nieuwe poging verandert de interne vertaalrevisie, zodat oudere
   asynchrone resultaten de nieuwe poging niet kunnen overschrijven.
5. De melding geeft aan hoeveel regels opnieuw zijn aangevraagd, niet
   hoeveel daarvan al geslaagd zijn. Een overblijvende mislukte regel kan
   later na een nieuwe modelcontrole opnieuw worden geprobeerd.

De bestaande één-regel-knop **↻ Probeer opnieuw** en de aparte
tweeweg-tekstherstelactie blijven bestaan. Ook die routes negeren
handmatig gecorrigeerde of verouderde regels.

## Privacy en acceptatie

Alles werkt alleen met de al gekozen tekst uit het tijdelijke gesprek,
zonder nieuw bestand, upload, automatisch modeldownloadverzoek of
cloudvertaler. Het herstelpaneel bewaart geen eigen kopie en kiest nooit
automatisch een nieuwe taal voor bestaande regels. Een voltooide
hervertaling is nog steeds een *lokale modeluitkomst*, geen door een
moedertaalspreker gecontroleerde vertaling.

De broncode en bestaande CI-build kunnen deze flow integreren maar vormen
geen fysieke Android-vliegtuigmodusproef. De afzonderlijke acceptatie voor
Android 12+, volledig rechtengecontroleerde Kurmanji/Sorani/Pasjtoe-ASR+MT,
getekende APK en productie-toelating blijven open.
