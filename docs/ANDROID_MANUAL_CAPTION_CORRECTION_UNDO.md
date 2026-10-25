# Eén stap terug na een handmatige ondertitelcorrectie

## Waarom

Tuddy bewaart het actuele gesprek uitsluitend in RAM. Een handmatige
correctie van de herkende oorspronkelijke zin kan ook de daaruit afgeleide
lokale vertaling vervangen. Een handmatige vertaalcorrectie kan bovendien
optioneel als **exact voorstel voor deze sessie** worden onthouden. Deze
acties waren definitief totdat de gebruiker opnieuw zelf ging typen.

## Nieuwe gebruikersflow

In het bestaande gesprek:

1. Kies bij één regel **Herstel herkende zin** of **Corrigeer vertaling**.
2. Pas de tekst aan en bevestig bewust de bestaande correctieknop.
3. Direct onder de gespreksterugmelding verschijnt uitsluitend voor de
   **laatste** handmatige correctie de knop **↶ Herstel vorige
   oorspronkelijke zin** of **↶ Herstel vorige vertaling**.
4. Tik daarop om precies de voorafgaande handmatig gewijzigde regel
   terug te halen. Het resultaat blijft in dit tijdelijke gesprek.

Bij een herstelde **oorspronkelijke zin** wordt de vorige bronzin,
taalrichting, correctievlag en eventuele menselijke uitleg hersteld.
Daarna wordt een *nieuwe, expliciet lokale* ML Kit-tekstvertaling
aangevraagd. Een oud vertaalresultaat wordt niet klakkeloos hergebruikt:
het kan immers uit een andere, inmiddels vervangen tekst stammen.

Bij een herstelde **vertaling** keert de vorige, reeds afgeronde tekst
met zijn status en correctievlag terug, zonder een nieuwe vertaalopdracht.
Had de gebruiker die gewijzigde vertaling bewust als exact
**sessievoorstel** opgeslagen, dan wordt ook uitsluitend de vorige
suggestielijst teruggezet **als niemand de lijst intussen heeft
gewijzigd**. Nieuwere suggesties worden niet overschreven.

## Privacy, herhaalgedrag en grenzen

- Er bestaat maximaal **één herstelmoment** tegelijk. Een volgende
  handmatige correctie vervangt het vorige. Er wordt geen permanent
  correctiearchief, modelgeheugen, auditlog of bestand gemaakt.
- De knop is niet beschikbaar tijdens het luisteren. Een verwante regel
  die is gewist, uit het gesprek verdwenen of een nieuwe vertaalrevisie
  heeft gekregen, mag niet worden hersteld. Het gesprek leegmaken of de
  Activity beëindigen wist ook dit ene herstelmoment.
- Bij herstellen krijgen gewijzigde regels een **nieuwe revisie**; eerder
  gestarte ML Kit-vertalingen, tijdelijke deadlines en wachtplekken
  worden geannuleerd of ongeldig. Late callbacks mogen niet de teruggezette
  tekst vervangen.
- Nog niet gedeelde externe export-/deelvoorstellen worden ongeldig
  gemaakt. Eerder gekopieerde, verstuurde of geëxporteerde tekst buiten
  Tuddy kan deze knop **niet** terughalen.
- Herstellen van een oorspronkelijke zin vereist dat de benodigde
  lokale tekstmodellen beschikbaar zijn; er is nooit een stille
  modeldownload of cloudfallback. Bij ontbrekende modellen blijft de
  oorspronkelijke zin zichtbaar met een herstelbare fout.
- Dit is een gebruikersfunctie, **geen fysieke Android 12+
  vliegtuigmodustest**, validatie door moedertaalsprekers, akkoord op
  modelrechten of toestemming voor een productie-release.
