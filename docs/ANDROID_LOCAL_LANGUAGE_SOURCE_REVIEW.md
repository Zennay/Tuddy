# Betrouwbare brontaal bij lokaal vertalen

## Waarom deze wijziging

Een Android `SpeechRecognizer.createOnDeviceSpeechRecognizer` gebruikt de
spraaktaal die jij koos of, als Android een echte taalwissel bevestigt, de
door de herkenner bevestigde nieuwe taal. Een losse tekstclassifier kan aan
de *achteraf herkende zin* niet bewijzen met welk spraakmodel er is geluisterd.
Bij korte tekst of mengtaal kan hij een andere taal raden, waardoor het
verkeerde ML Kit-tekstmodel zou worden geselecteerd.

**Vanaf deze mijlpaal houdt de normale Tuddy-gespreksvertaling de
daadwerkelijk geselecteerde of door Android bevestigde spreektaal aan.**
Een classificatie van de herkende woorden mag die taal niet zelfstandig
overschrijven. De bestaande handmatige broncorrectie op een gespreksregel
blijft beschikbaar; opnieuw vertalen vereist altijd lokale aanwezige
tekstmodellen en doet geen automatische download.

## Een voorstel bij Androids geselecteerde tekst

De actie **Vertaal geselecteerde tekst** heeft een afzonderlijke,
uitdrukkelijke knop **Stel brontaal lokaal voor**. Dit is een *advies*,
geen automatische omschakeling:

1. Bij minder dan 20 Unicode-tekens is de knop niet beschikbaar.
2. Tuddy gebruikt het al gebundelde ML Kit-model voor taalherkenning om
   mogelijke talen en bijbehorende modelvertrouwenswaarden te lezen.
   Er worden geen externe herkenningsdiensten of modeldownloads gestart.
3. Alleen als de tekst ten minste twaalf letters bevat, de beste score
   minimaal 0,80 is en deze score minstens 0,20 hoger ligt dan de
   op één na beste taalkandidaat, verschijnt een ondersteund voorstel.
   Onbekende, ambigue of niet in de lokale ML Kit-vertaalcatalogus
   aanwezige talen leveren **geen** nieuwe taalkeuze op.
4. Een zichtbaar **Gebruik voorgestelde brontaal**-knopje laat de
   gebruiker dit advies zelf accepteren. Totdat die knop wordt aangetikt
   blijft de eigen bronselectie exact gelijk; niets wordt vertaald,
   gedeeld, gekopieerd of teruggezet naar de andere app.
5. De keuze wordt gewist zodra de selectie verandert, de gebruiker
   handmatig een andere taal kiest, de operatie annuleert of de
   Android-Activity zijn voorgrondfocus verliest.

De modelscore is geen onafhankelijke waarheidsmaat, en bij meertalige
zinnen kan zelfs een hoge score misleidend zijn. Controleer de
daadwerkelijke bron- en doeltaal altijd zelf vóór vertalen of doorsturen.

## Grenzen

Deze code en een geslaagde verplichte Android-CI-build zijn geen
toestelacceptatie. De echte Android 12+-vliegtuigmodusproeven,
NL→EN/EN→NL-kwaliteit, onafhankelijk onderzoek naar gebruikerservaring,
rechten en moedertaalsprekercontrole van volledige Koerdisch/Pasjtoe-
modellen, APK-installatie en ondertekende productierelease blijven
afzonderlijk open. Er is geen cloudfallback, geen server, geen analytics
en geen opslag van privétekst of gedetecteerde scores toegevoegd.
