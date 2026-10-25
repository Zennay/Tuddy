# Eén zin: herkend voorstel beoordelen voordat het gesprek groeit

## Nieuwe app-core-stroom

In **Eén zin** (naast de bestaande **Doorlopend**-modus) kiest de gebruiker
zelf wanneer de Android on-device microfoon één zin opneemt. Bij een
herkenningsresultaat sluit Tuddy de microfoon meteen. De herkende tekst
gaat nu **eerst** naar een tijdelijk controlevenster, niet automatisch
naar de gespreksgeschiedenis en nog niet naar de lokale vertaler.

In het controlevenster kan de gebruiker:
- de eerste door Android voorgestelde tekst volledig nalezen of aanpassen;
- maximaal drie aanvullende, onzekere Android-herkenningsvoorstellen
  selecteren en daarna nog corrigeren;
- **Gooi zin weg** kiezen: géén gesprek, vertaling of opslag wordt aangemaakt;
- expliciet **Bevestig en vertaal lokaal** kiezen: pas dan wordt de gekozen
  tekst als nieuwe tijdelijke regel ingevoegd en langs dezelfde bestaande
  ML Kit-only modelcontrole en vertaalpipeline gestuurd.

De oorspronkelijke spraakrichting en vertaaldoeltaal worden bevroren bij
de herkenningsronde en in het venster getoond. Een menselijke correctie
wordt als zodanig gemarkeerd, niet als een betrouwbaar of native-speaker-
gevalideerd AI-resultaat. De overige één-regel-alternatieven zijn alleen
Android-hypotheses, géén door Tuddy gecorrigeerde tekst. Oudere asynchrone
uitkomsten van andere regels mogen het nieuwe resultaat niet vervangen.

**Doorlopend** blijft bewust het bestaande gedrag houden: elke geslaagde
recognizer-ronde wordt meteen als tijdelijke gespreksregel toegevoegd,
zonder verplichte tussenstap. De Android-afgedwongen microfoonlimieten
en strikte vliegtuigmodusbewaking blijven ook in **Eén zin** actief.

## Privacy, bewaargrenzen en hervatten

Er is slechts één RAM-only conceptronde tegelijk. Zolang die nog niet
beoordeeld is, kan de microfoon geen nieuwe ronde starten; iemand moet
eerst bevestigen of weggooien. Naar de achtergrond gaan, Activity
verlaten, gespreksgeschiedenis wissen of een verse app-start gooit
het concept weg. Een afgewezen concept komt niet in een export,
ondertitel, studiekaart, klembord of appvoorkeur terecht.

Het systeem onthoudt alleen de eerder gekozen opnamemodus; nooit
hypotheses, microfoonaudio of afgewezen tekst. Android krijgt een
begrensd verzoek om maximaal acht hypotheses, waarvan Tuddy maximaal
drie unieke, kort begrensde alternatieven toont. Er zijn geen nieuwe
rechten, servers, modeldownloads of verborgen ML Kit-vertalingen nodig.

De technische verificatie van deze source wijziging is via de bestaande
GitHub Android-build en source-head checks. Gebruik op een fysiek
Android 12+-toestel is nog **niet** gevalideerd. De afzonderlijke
NL→EN / EN→NL vliegtuigmodusacceptatie, complete rechtengecontroleerde
Kurmanji/Sorani/Pasjtoe ASR+MT-ondersteuning, onafhankelijke UX-meting
en ondertekende release blijven open.
