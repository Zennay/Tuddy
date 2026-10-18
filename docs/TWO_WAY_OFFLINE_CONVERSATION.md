# Tweezijdig gesprek: handmatig de spreekrichting wisselen

Tuddy toont de gekozen **spreektaal → vertaaltaal** boven het live gesprek.
De knop **⇄ Wissel richting** staat direct onder **Luister**. Hiermee kun je
tijdens een gesprek bijvoorbeeld Nederlands → Engels omkeren naar Engels →
Nederlands, zonder oude ondertitels, tijdstempels, markeringen of handmatige
sprekerlabels te wissen. Elke bestaande ondertitel toont voortaan zijn eigen
oorspronkelijke taalpaar, ook nadat je hebt gewisseld.

## Wat gebeurt er bij het wisselen?

1. De microfoon en lopende on-device herkenning stoppen onmiddellijk. Tuddy
   herstart de microfoon nooit automatisch na een taalwisseling.
2. De nieuwe spreektaal moet in de bestaande picker met elf spraakkandidaten
   staan (NL/EN/DE/FR/ES/AR/FA/UK/TR/RU/UR). De extra kandidaten Farsi,
   Oekraïens, Turks, Russisch en Urdu komen uit de oude v0.4-taalhints.
   Androids lokale recognizer moet voor elke gekozen taal op dit apparaat
   een werkelijk beschikbaar spraakpakket hebben; de picker garandeert dit niet. De 59 ML Kit-**tekst**doelen zijn **niet**
   allemaal spraakherkenningstalen. Bij een ander doel of identieke talen is
   omkeren uitgeschakeld. Dit zegt niets over welke lokale pakketten een
   specifiek toestel daadwerkelijk bezit.
3. Lopende tussenresultaten verdwijnen en oude ongeldige toestemmingen voor
   delen/exporteren worden ingetrokken; het reeds afgeronde gesprek blijft
   als tijdelijk geheugen zichtbaar. De nieuwe taalkeuze wordt in de
   afgeschermde Android-instellingen bewaard, niet de tekst of audio.
4. **Taal- en modelinstellingen** opent automatisch. Tik eerst op
   **Controleer spraak** voor het *nieuwe* Android-spraakpakket en
   **Controleer tekst** voor de nieuwe vertaairichting. Bereid ontbrekende
   modellen alleen bewust via wifi voor.
5. Tik pas na de offline-controle opnieuw op **Luister**. Tuddy gebruikt
   alleen Androids on-device spraakherkenner en geen cloudfallback.

De optie **Automatische spraaktaal (Android 14+)** blijft een afzonderlijke,
door de gebruiker gekozen voorziening. Een handmatige richtingswisseling is
geen bewijs dat taalherkenning, spraakpakketten of vertalingen werken.

## Privacy en beperkingen

Dit is geen automatische detectie van gespreksdeelnemers of stemidentiteit.
De A/B-sprekerlabels zijn door de gebruiker ingevoerde tijdelijke notities.
Bewuste exports kunnen persoonsgegevens verlaten via de gekozen Android-
provider. Geen productie-APK of fysieke vliegtuigmodusvalidatie is door
deze UI-uitbreiding geleverd. Vrije Kurmanji/Sorani/Pasjtoe-spraakherkenning
en -vertaling zijn nog niet beschikbaar en mogen niet worden gesuggereerd.
Gebruik automatische vertalingen niet als enige bron voor medische,
juridische of asielbeslissingen.
