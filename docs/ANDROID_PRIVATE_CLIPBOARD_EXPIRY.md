# Optionele klembordtimer voor privétekst

Tuddy werkt privacy-first met een vluchtige gespreksgeschiedenis, maar
**zelf kopiëren naar het Android-systeemklembord is een aparte
gegevensoverdracht**. Andere apps kunnen de tekst onmiddellijk lezen of
bewaren. Een timer verandert dat risico niet met terugwerkende kracht.

## Eén instelling voor de hele Android-app

In het hoofdscherm bij **Taal- en modelinstellingen** en in
**Vertaal geselecteerde tekst → Kopieer vertaling** kun je de
klembordtimer wisselen tussen:

- **Uit** (standaard): geen automatische verwijdering door Tuddy;
  Android beheert zijn eigen klembordlevensduur
- **1 minuut**: na een nieuwe, bewuste kopieeractie controleert Tuddy
  na ongeveer 60 seconden of zijn eigen kopie nog actief is
- **5 minuten**: dezelfde controle na ongeveer 300 seconden

Het instellen schrijft alleen de gekozen tijdsduur in de lokale
Android-appvoorkeuren. **Geen gesprekszinnen, vertalingen, klembordinhoud
of kopieerlog worden persistent opgeslagen.** Een nieuwe tijdsduur
geldt pas voor volgende kopieeracties.

Alle bestaande Tuddy-tekstkopieën lopen via dezelfde code: een
individuele gespreksregel, een expliciet geselecteerd privéarchieffragment,
een on-device speechpack-overzicht, een offline toesteldiagnostiekrapport
en een vertaling vanuit het Android-tekstselectiemenu.

## Wat er gebeurt bij het verlopen

Tuddy geeft elke door de gebruiker gemaakte kopie een unieke tijdelijke
klembordmarker. Voor de duur van de timer bewaart de app uitsluitend
die marker en een tijdelijke afdruk (SHA-256) van de tekst **in RAM**.
Android ontvangt de normale platte tekst, inclusief de door Android
ondersteunde *gevoelige klembordinhoud*-aanduiding.

Na het verlopen probeert Tuddy het systeemklembord te lezen. Alleen
als **de marker, het MIME-type én de tekstafdruk** nog overeenkomen
met de meest recente Tuddy-kopie, wordt het systeemklembord gewist.
Heeft de gebruiker inmiddels met een andere app iets gekopieerd of een
nieuwe kopie gemaakt, dan mag Tuddy die inhoud niet wissen.

De timer draait alleen zolang het Tuddy-proces bestaat. Android kan
het proces stoppen of achtergrondtoegang tot het klembord blokkeren.
Er komt **geen achtergrondservice, alarm of nieuwe machtiging** bij,
en verwijderen kan niet worden gegarandeerd. Op Android 13+ blijft ook
het reguliere systeemgedrag voor het klembord actief.

## Niet gelijk aan verwijderen bij de ontvanger

Dit verwijdert geen gekopieerde inhoud uit andere apps, gedeelde
screenshots, gesynchroniseerde klembordapps of Androids eigen
klembordgeschiedenis. Je blijft zelf verantwoordelijk voor bewust
kopiëren en plakken. De beste privacy-optie is privétekst niet
buiten Tuddy kopiëren.

De code-/buildintegratie bewijst niet dat Android op een fysiek toestel
de klembordtimer exact uitvoert. Fysieke Android 12+ vliegtuigmodus-
acceptatie, volledige modelrechten/ASR+MT en een getekende productie-
release blijven aparte gates.
