# Alleen bewuste modeldownloads tijdens offline vertalen

Tuddy laat de microfoon, het typen, het corrigeren en het opnieuw vertalen
van een bijschrift **nooit** op eigen initiatief een ML Kit-taalmodel
downloaden. De app controleert nu vóór iedere nieuwe afgeronde
tekstvertaling via de **read-only** ML Kit-modelinventaris of het
daadwerkelijk benodigde bron-/doeltaalpaar lokaal aanwezig is.

- **Lokaal beschikbaar:** Tuddy gebruikt de aanwezige taalmodellen
  rechtstreeks om de zin offline te vertalen.
- **Model ontbreekt:** de regel toont de eigenlijke (door Android/ML Kit
  waargenomen) brontaal en het doeltaal-ID en vraagt de gebruiker in de
  taalinstellingen via wifi expliciet voor te bereiden. Tuddy probeert
  niet stilzwijgend een download uit te voeren.
- **Status onbekend of servicefout:** vertaling wordt niet alsnog
  via een online dienst geprobeerd. Er verschijnt een lokale foutmelding.
- **Gelijke bron- en doeltaal:** geen tekstvertaalmodel nodig.
- **Taalherkenning detecteert een andere bron dan handmatig gekozen:** ook
  dan moet het *werkelijk gedetecteerde* lokaal modelpaar aanwezig zijn.
  Anders toont Tuddy de ontbrekende combinatie; een ander model wordt
  niet op de achtergrond gedownload.

Iedere zin gebruikt een eigen modelinventarisaanvraag, zodat twee
gelijktijdig ontvangen ondertitels elkaar niet annuleren. Het
per-regel generatie-/revisiemechanisme voorkomt dat een inmiddels
gewiste of gecorrigeerde zin door een vertraagd resultaat terugkomt.

Het bestaande pad **Tekstmodel offline voorbereiden** blijft de
enige plek in Tuddy waar de gebruiker bewust via wifi een ML Kit
tekstmodeldownload aanvraagt. De device-native Android-spraakpakket-
download is een *afzonderlijke*, expliciete gebruikersactie. De
interim-ondertitels begonnen al geen downloads en blijven ongewijzigd.

Dit bewijst geen werkende offline ASR of MT op een fysiek toestel, geeft
geen onbewezen vrije Kurmanji/Sorani/Pasjtoe-ondersteuning en ontslaat
Tuddy niet van de bestaande licentie-, toestel- en releasevoorwaarden.
