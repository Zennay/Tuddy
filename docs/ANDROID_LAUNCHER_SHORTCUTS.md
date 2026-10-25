# Android-startscherm: Tuddy zonder automatische microfoon

Tuddy publiceert drie expliciete app-snelkoppelingen aan Androids
launcher, zodat je bestaande functies rechtstreeks vanaf het
startscherm kunt openen. Op sommige launchers verschijnen ze via
lang indrukken op het Tuddy-icoon. Je kunt ook in Tuddy bij **Meer
functies** voor elk type afzonderlijk **Startscherm: … toevoegen**
aanraken om Androids eigen bevestiging voor een vastgezette
snelkoppeling te openen. Android bepaalt of dit mogelijk is.
Een geaccepteerd pin-verzoek is geen bewijs dat het icoon geplaatst is.

- **Typ lokaal** opent een **leeg** tijdelijk tekstvertalingsconcept,
  zonder microfoontoegang, clipboardimport, modeldownload of upload.
- **Offline check** opent de bestaande startcheck met aparte Android-
  spraakpakketten, ML Kit-tekstmodellen en radiocontrole. De launcher
  zet vliegtuigmodus of wifi nooit zelf aan of uit.
- **Eén zin** selecteert alleen voor de huidige app-sessie de reeds
  bestaande één-zinsherkenningsmodus. Pas een bewuste tik op **Luister**
  kan Android om de microfoon vragen. Het eenmalig herkende resultaat
  moet daarna nog door jou worden beoordeeld en goedgekeurd.

Alle routes werken bij **koude start én opnieuw openen van een bestaande
Activity**. Het routeverzoek is enkel een vaste, gevalideerde interne
actiecode. De app wist die code uit de ontvangen Intent zodat rotatie
of Activity-recreatie de handeling niet automatisch herhaalt. Een
binnenkomende snelkoppeling mag een lopende microfoonopname, een
onbeoordeelde één-zinsherkenning, een open tekstconcept, een
tweeweg-tekstgesprek of een externe documentpreview niet ongevraagd
overschrijven. In zo'n geval verschijnt een instructie om eerst de
lopende actie bewust af te ronden.

De shortcutpublicatie bevat **uitsluitend drie statische titels,
iconen en routecodes**. Nooit tekst, audio, recent gebruikte talen,
bestandslocaties, apparaatidentificaties of transcriptgegevens. Een
andere app kan een exported Activity expliciet openen; de route geeft
daarom alleen toegang tot dezelfde openbare handmatig bedienbare
schermen, niet tot microfoonstart, bestandsimport, modelinstallatie,
automatische vertaling of privileged acties.

Een succesvolle GitHub-build bevestigt hoogstens dat deze native
integratie compileert. Launchergedrag verschilt per Android 12+
toestel/launcher en is niet fysiek gevalideerd door deze bronwijziging.
Vliegtuigmodusacceptatie (#1), modelrechten/native-speakercontrole
(#2), menselijke UX-metingen (#464), ondertekende APK en productie-
uitrol blijven afzonderlijke gates.
