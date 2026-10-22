# Geïnstalleerde lokale Android-voorleesstemmen

Open **Taalondersteuning → Controleer lokale stemmen** om de stemmen te bekijken die de standaard Android-tekst-naar-spraakdienst op dit toestel als geïnstalleerd en niet-netwerkafhankelijk meldt.

## Stem bekijken en kiezen

- Tuddy toont alle gevonden geïnstalleerde lokale stemvarianten met locale-tag en engine-ID. Gebruik het zoekveld om te filteren op stemnaam, taal/regio of engine-ID; het aantal treffers en het totaal blijven zichtbaar. Netwerkafhankelijke en nog niet geïnstalleerde stemmen worden uitgesloten.
- Tik op **Luister** om de vaste, bij de stemtaal passende voorbeeldzin af te spelen als Tuddy die taaltekst heeft. Voor overige talen gebruikt de stemproef uitsluitend **“1, 2, 3.”** en wordt geen onbeoordeelde vertaling verzonnen. Het scherm toont tijdens het luisteren de exacte voorbeeldtekst. **Stop stemproef** beëindigt het afspelen; ook zoeken, het stemmenoverzicht opnieuw controleren, het venster sluiten en een time-out stoppen de proef.
- Tik op **Gebruik deze stem** om die als lokale voorkeur voor de betreffende taal in te stellen. De voorkeur geldt voor alle Tuddy-voorleesfuncties die die taal gebruiken.
- **Automatische stemkeuze herstellen** wist de expliciet gekozen stemvoorkeuren en laat Tuddy weer automatisch een passende lokale stem kiezen.
- De voorkeursstem wordt alleen in de private app-instellingen op dit toestel opgeslagen. Er wordt geen tekst, audio, transcript of gebruiksgeschiedenis bewaard.
- **Android-toegankelijkheidsinstellingen** opent, als Android dit ondersteunt, het systeemscherm. Open daar **Tekst-naar-spraakuitvoer** (de benaming verschilt per Android-versie), beheer stemmen zelf en voer daarna de controle opnieuw uit.

## Wat de lijst betekent

De inventaris leest alleen metadata van de Android TTS-dienst en spreekt tijdens de inventariscontrole niets uit. Alleen na een expliciete tik op **Luister** wordt de locale-afhankelijke voorbeeldtekst (of bij andere talen alleen “1, 2, 3.”) afgespeeld, en alleen als exact die stem nog als lokaal geïnstalleerd en niet-netwerkafhankelijk beschikbaar is. Tuddy controleert vlak voor het spreken ook of Android daadwerkelijk die stem selecteerde; lukt dit niet of meldt de engine geen einde, dan stopt de proef uiterlijk na 20 seconden. De preview gebruikt geen eigen tekst, downloadt geen stem, wijzigt geen voorkeur en slaat geen audio of tekst op. Een stemproef laat de klank horen, maar bewijst geen uitspraakskwaliteit of taalondersteuning. Het bewijst niet dat offline spraakherkenning (ASR), machinevertaling (MT), dialectkwaliteit of een volledige vliegtuigmoduservaring werkt.

Taalondersteuning kan per apparaat, TTS-engine en stem verschillen. Tuddy gebruikt geen online spraakfallback; als de gekozen stem niet lokaal beschikbaar is, wordt de voorleesactie geweigerd en wordt niet stilzwijgend naar een netwerkstem overgeschakeld.
