# Oude LiveLingo v0.4-taalverzoeken eerlijk terugvinden

Bron: gebruiker-eigen archief
`/Projects/LiveLingo/LiveLingo-Android-v0.4-Kurdish-Pashto.zip`,
`model/Languages.kt`, met afzonderlijke lijsten voor ondersteunde
ML Kit-teksttalen, drie beperkte basiswoordenboeken en zes gewenste
maar onbewezen talen.

De moderne Tuddy-interface biedt nog steeds uitsluitend de daadwerkelijk
in `TargetCatalog.codes` ondersteunde **59 ML Kit-teksttalen** als
selecteerbare bron/doeltaal. Uit het historische archief is een
**niet-selecteerbare informatiecatalogus** gereconstrueerd voor zoekvragen:
- Kurmanji (kmr), Sorani (ckb) en Pasjtoe (ps): geen vrije zinsvertaling
  of bewezen on-device ASR/MT; wel vijf exact opgenomen basisuitdrukkingen.
- Somalisch (so), Tigrinya (ti), Amhaars (am), Oromo (om), Rohingya (rhg)
  en Dari (prs): geen hier bewezen beschikbare ML Kit-tekst- of
  spraakmodellen. Een Perzisch `fa`-model is geen gevalideerde vervanger
  voor Dari.

## Nieuwe gebruikersstroom

Open **Tweeweg-tekstgesprek → Van/Naar → zoek taal**. Zoek bijvoorbeeld
`pashto`, `sorani`, `somali` of `dari`. Tuddy toont waarom
de taal niet in het ML Kit-menu beschikbaar is, in plaats van een
onwerkzame taalcode te accepteren of stilzwijgend naar een andere
taal over te schakelen.

Bij Kurmanji, Sorani en Pasjtoe biedt **Bekijk 5 basiswoorden** toegang
tot het bestaande exact-match offline woordenbord, direct op de gekozen
taal geopend. Als er een nog niet verstuurd tekstconcept is, vraagt
de app eerst of dat concept mag vervallen. De vijf historische
uitdrukkingen zijn geen vrij vertaalmodel en niet onafhankelijk door
native speakers gevalideerd.

## Beveiliging, productgrenzen en v0.4-status

De historische `OfflineTranslator.kt` startte lokale modeldownloads
impliciet tijdens een vertaalverzoek; dat conflicteert met Tuddy's
latere expliciete wifi-toestemmingsbeleid en is daarom **niet**
zonder meer teruggekopieerd. Ook wordt de oude 59-talen-spraaklijst
niet als bewezen lokale Android-ASR-pakketlijst geïmporteerd.

De code scheidt ondersteunde ML Kit-tekstcodes van labels voor
niet-ondersteunde talen; de nieuwe lijst raakt nooit
`SpeechRecognizer`, `TranslatorOptions` of auto-downloads. Geen
test- of QA-suite toegevoegd of los uitgevoerd.

Volledige historische backport, fysieke Android 12+
vliegtuigmodusacceptatie, vrije gelicentieerde Kurmanji/Sorani/Pasjtoe
ASR+MT, taalreview en ondertekende productierelease blijven open.
