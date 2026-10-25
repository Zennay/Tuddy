# Android-geheugendruk tijdens lokaal vertalen en luisteren

Tuddy houdt tijdens het gesprek ondertitels, vertalingen en modelclients
tijdelijk op het apparaat. Bij geheugenproblemen moet de app stoppen
zonder ongeautoriseerde online fallback of het wissen van lopend werk.

## Wat gebeurt er op een fysiek Android-toestel?

- **Microfoonstart:** Tuddy leest Androids actuele
  `ActivityManager.MemoryInfo.lowMemory`. Als Android op dat moment
  kritiek geheugentekort meldt, wordt de microfoon niet gestart.
  Een eventuele oude recognizer of herstarttimer wordt eerst veilig
  opgeruimd. Gebruikers hoeven geen toestemmingen toe te voegen.
- **Normale Android-geheugentrim:** De hoofdapp laat ongebruikte
  ML Kit-vertaalclients los. Al ingediende native vertalingen worden
  door de bestaande pool als actief behandeld: ze krijgen gelegenheid
  hun callback af te ronden en worden pas daarna gesloten. Herkende
  zinnen en manuele correcties blijven in het tijdelijke gesprek.
- **Kritieke geheugendruk tijdens luisteren:** Androids
  `onLowMemory` of een expliciete kritieke foreground-trim sluit
  de microfoon, invalideert late herkennercallbacks en annuleert
  automatische herstarts. De bestaande ondertitels en reeds ingezette
  vertaalopdrachten blijven onaangeroerd. Tuddy toont een melding;
  de gebruiker moet later bewust opnieuw op **Luister** drukken.
  Alleen het aantal kritieke onderbrekingen komt als teller in het
  bestaande RAM-only diagnostiekrapport. Geen audio of transcript in
  deze teller.
- **Geselecteerde tekst vertalen:** Het afzonderlijke
  Android-tekstselectiescherm stopt bij een kritiek systeemsignaal
  de lopende lokale vertaling en voorleesactie, maar behoudt het
  tijdelijke invoerveld zolang de Activity vooraan blijft. De reeds
  ingediende ML Kit-taak kan intern nog afronden; oude callbacks zijn
  door de bestaande generatierevisie ontoegankelijk voor de UI en
  de native client wordt pas na daadwerkelijke afronding gesloten.
  Er wordt niets teruggestuurd naar de bronapp zonder een nieuwe
  expliciete actie.
- **Naar de achtergrond:** Het bestaande lifecyclebeleid wist een
  privéselectie en stopt de microfoon en TTS. Een gewone
  `TRIM_MEMORY_UI_HIDDEN` betekent *niet* automatisch dat een
  actieve spraaksessie onder kritiek geheugendruk staat.

## Android 14 en nieuwer

De oude `TRIM_MEMORY_RUNNING_LOW/CRITICAL` signalen worden op
nieuwere Android-versies niet meer uitgezonden. Daarom controleert
Tuddy ook de actuele `lowMemory`-status en reageert op
`onLowMemory`. Het niet ontvangen van zo'n melding is geen bewijs
van voldoende geheugen. Android kan de app nog altijd beëindigen
zonder waarschuwing. Tuddy mag nooit claimen dat een tijdelijk
RAM-only gesprek een procesdood overleeft.

## Onafhankelijke acceptatie

De bestaande GitHub-build bevestigt hoogstens dat de Android-code
compileert. Een fysieke Android 12+-vliegtuigmodusproef, echte
ML Kit-inferentie op het toestel, een onafhankelijk gecontroleerde
APK-identiteit en een ondertekende productie-release blijven
afzonderlijke acceptatiegates. Deze implementatie activeert geen
nieuwe taalmodellen, cloudspraak, netwerkservices of achtergrondwerk.
