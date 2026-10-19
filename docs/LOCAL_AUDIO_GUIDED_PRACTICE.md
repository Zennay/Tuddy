# Tuddy · begeleide lokale A–B-oefenroute

Een speler van zelfgekozen lokale audio met eigen SRT/VTT-tijdcodes kan nu **tot vier zelf gemarkeerde A–B-fragmenten in een gekozen volgorde oefenen** zonder telkens zelf het volgende fragment te zoeken.

- De gebruiker plaatst tot acht eigen audiotijdmarkeringen en bewaart daaruit maximaal vier geldige A–B-combinaties (750 milliseconden tot vijf minuten). De bestaande oefenkaarten kunnen handmatig worden geordend.
- Onder die kaarten kiest de gebruiker **1×, 3× of 5× beluisteren per fragment**, waarna **Start mijn begeleide A–B-oefenroute** een vaste kopie van de huidige volgorde in deze audiosessie activeert. Het actuele segment en het aantal afgeronde passages worden op het scherm bijgehouden.
- De originele Android MediaPlayer gebruikt uitsluitend de bestaande lokale source/seek-bediening. Als audio gepauzeerd is, drukt de gebruiker bewust op Afspelen. Vervolgens herhaalt de speler elk fragment het ingestelde aantal keren en gaat naar de volgende kaart. Na het laatste fragment pauzeert hij met een expliciete voltooiingsmelding; er is geen oneindige doorloop en geen automatisch afspelen bij het openen.
- De bestaande pauzeknop pauzeert tijdelijk; **Stop begeleide oefenroute** beëindigt de route en pauzeert. Handmatig springen buiten het huidige fragment annuleert de route. Tijdens een lopende route zijn bewerken, herschikken, verwijderen en andere herhaalmodi bewust uitgeschakeld zodat er geen onverwacht gewijzigde tijdgrenzen worden afgespeeld.
- Bij wisselen van audio, sluiten van de dialoog of Android-background blijven de bestaande bron-/lifecyclevoorwaarden gelden. De route en alle kaarten zijn tijdelijk; geen cloud, opgenomen audio, server, documentwijziging, AI-uitlijning, toestemmingsuitbreiding of permanente opslag.

Nog geen geverifieerde fysieke Android-toestelwerking of getekende productie-APK. De bestaande verplichte hosted Android-build en release-/privacyvoorwaarden blijven intact. Er zijn geen nieuwe QA-/regressiesuites geschreven of handmatig uitgevoerd.
