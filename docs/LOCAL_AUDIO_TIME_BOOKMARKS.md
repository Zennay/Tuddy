# Eigen audiomomenten markeren en vrij A–B herhalen

## Product

Tuddy's bestaande native Android scherm **Ondertitels bij eigen audio** kan nu
maximaal acht zelfgekozen audioposities markeren. De gebruiker kan terug naar
zo'n positie springen, een markering verwijderen en uit twee markeringen een
A–B-luisterlus van **0,75 seconde tot en met 5 minuten** selecteren. Deze
luisterlus is **onafhankelijk van SRT/VTT cuegrenzen**: de gebruiker kan
bijvoorbeeld precies een uitspraak tussen twee ondertitelregels herhalen.

Gebruik binnen **▶ Eigen audio met tijdcodes afspelen…**:

1. Open `▾ Markeer audiomomenten en herhaal vrij A–B`.
2. Luister of gebruik de scrubber, kies `＋ Markeer huidige audiopositie`.
   Binnen 0,5 seconde van een bestaande markering wordt niet dubbel gemarkeerd.
   Maximaal acht markeringen, in tijdvolgorde.
3. Met `▶ Naar markering` spring je naar het gekozen audiomoment.
4. Kies bij het gewenste begin `Kies A` en bij het einde `Kies B`.
   Omgekeerde, identieke en te lange intervallen kunnen niet beginnen.
5. `↻ Herhaal tussen mijn audio A- en B-markeringen` start een begrensde
   lus. De afspeelknop blijft verantwoordelijk voor een gepauzeerde speler;
   de lus activeert niet op de achtergrond een microfoon of opname.
6. Stop de lus via dezelfde knop; een sprong buiten het segment stopt de
   lopende lus ook. Individuele markeringen blijven beschikbaar voor
   navigatie tot het scherm wordt afgesloten.

De bestaande cue-herhaling en ondertitel-A–B-oefening schakelen de vrije
audioherhaling uit zodra die herhaalmodus wordt gestart en andersom.

## Privacy en lifecycle

- Geen transcript-/vertalingmodel of woorduitlijning: uitsluitend
  `MediaPlayer.currentPosition` en handmatige gebruikerskeuzes.
- Geen automatische `start()` bij het vastleggen van markeringen.
  Android-levenscyclus pauzeert bestaand geluid bij achtergrondactiviteit.
- Alle markeringen en A/B-keuzes zijn uitsluitend in vluchtige Compose-state.
  Bij het openen van een ander audio-item, sluiten, fout of lifecycle-dispose
  wordt de speler vrijgegeven en de toestand gereset.
- Geen nieuwe bestanden, broadcasts, cloud-API, upload, database, notificatie,
  analytics, account, of runtime/VPS-koppeling. SAF-medialeverancier kan eigen
  netwerktoegang gebruiken; voor werkelijk offline afspelen moet de audio
  lokaal beschikbaar zijn.
- Maximaal zes uur audiolengte volgens bestaande MediaPlayer-admission;
  lusgrenzen zijn streng begrensd, zodat afspeelposities niet overstromen.

## Release-gates

Dit is bronfunctionaliteit, niet een bewezen fysieke Android 12+ acceptatie
of native Kurmanji/Sorani/Pasjtoe ASR/MT. Bestaande verplichte Gradle/Android
CI, review, APK signing en fysieke offline toestel-/licentiegates blijven
van kracht. Geen zelfstandige QA-/regressiesuites toegevoegd of gestart.
